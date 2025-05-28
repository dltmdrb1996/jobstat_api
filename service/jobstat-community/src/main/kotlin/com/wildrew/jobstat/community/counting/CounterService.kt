package com.wildrew.jobstat.community.counting

import com.wildrew.jobstat.community.board.repository.BoardRepository
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_event.model.payload.board.item.BoardIncrementItem
import com.wildrew.jobstat.core.core_jpa_base.id_generator.SnowflakeGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * 실시간 카운터 관리 및 배치 이벤트 발행을 담당하는 서비스 (CQRS 적용).
 * - 실시간 요청 처리 (좋아요, 조회수 증가 등 - Redis 직접 업데이트 및 pending 등록)
 * - 최종 카운터 정보 조회 (DB + Redis 통합 - 기존 방식 유지)
 * - 주기적인 배치 실행 (Redis 카운터를 읽어 Kafka 이벤트로 발행)
 */
@Service
class CounterService(
    private val counterRepository: CounterRepository,
    private val boardRepository: BoardRepository, // 조회 API에서 DB 값 가져올 때 사용
    private val communityCommandEventPublisher: CommunityCommandEventPublisher, // Outbox 발행
    private val snowflakeGenerator: SnowflakeGenerator,
    private val transactionManager: PlatformTransactionManager,
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Value("\${counter.scheduler.batch-size:1000}")
    private val schedulerBatchSize: Int = 1000

    data class BoardCounters(
        val boardId: Long,
        val viewCount: Int,
        val likeCount: Int,
        val userLiked: Boolean,
    )

    fun cleanupBoardCounters(boardId: Long) {
        log.debug("게시글 ID {}의 카운터 정리 요청", boardId)
        try {
            counterRepository.deleteBoardCounters(boardId)
        } catch (e: Exception) {
            log.error("게시글 ID {} 카운터 정리 중 오류 발생", boardId, e)
            throw e
        }
    }

    fun incrementViewCount(boardId: Long) {
        try {
            counterRepository.atomicIncrementViewCountAndAddPending(boardId)
            log.debug("게시글 ID {} 조회수 증가 요청 완료 (Redis 업데이트)", boardId)
        } catch (e: Exception) {
            log.error("조회수 증가 실패 (Redis 업데이트): boardId={}", boardId, e)
            throw e
        }
    }

    fun incrementLikeCount(
        boardId: Long,
        userId: String,
        dbLikeCount: Int? = null,
    ): Int {
        try {
            val dbLikeCount = dbLikeCount ?: fetchDbLikeCount(boardId)
            val redisDelta = counterRepository.atomicLikeOperation(boardId, userId)
            log.debug("게시글 ID {} 좋아요 증가 요청 완료 (Redis 증감분: {})", boardId, redisDelta)
            return dbLikeCount + redisDelta
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            log.error("좋아요 증가 실패 (Redis 업데이트): boardId={}, userId={}", boardId, userId, e)
            throw e
        }
    }

    fun decrementLikeCount(
        boardId: Long,
        userId: String,
        dbLikeCount: Int? = null,
    ): Int {
        try {
            val dbLikeCount = dbLikeCount ?: fetchDbLikeCount(boardId)
            val redisDelta = counterRepository.atomicUnlikeOperation(boardId, userId) // 순수 증감분 (-1)
            log.debug("게시글 ID {} 좋아요 취소 요청 완료 (Redis 증감분: {})", boardId, redisDelta)
            return dbLikeCount + redisDelta
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            log.error("좋아요 취소 실패 (Redis 업데이트): boardId={}, userId={}", boardId, userId, e)
            throw e
        }
    }

    fun getSingleBoardCounters(
        boardId: Long,
        userId: String?,
        dbViewCount: Int? = null,
        dbLikeCount: Int? = null,
    ): BoardCounters {
        try {
            val redisCounters = counterRepository.getSingleBoardCountersFromRedis(boardId, userId)
            val dbViewCount = dbViewCount ?: fetchDbViewCount(boardId)
            val dbLikeCount = dbLikeCount ?: fetchDbLikeCount(boardId)

            return BoardCounters(
                boardId = boardId,
                viewCount = dbViewCount + redisCounters.viewCount,
                likeCount = dbLikeCount + redisCounters.likeCount,
                userLiked = redisCounters.userLiked,
            )
        } catch (e: Exception) {
            log.error("단일 게시글({}) 카운터 조회 실패", boardId, e)
            throw e
        }
    }

    fun getBulkBoardCounters(
        boardIdsWithDbCounts: List<Triple<Long, Int, Int>>,
        userId: String? = null,
    ): List<BoardCounters> {
        if (boardIdsWithDbCounts.isEmpty()) return emptyList()
        val boardIds = boardIdsWithDbCounts.map { it.first }
        try {
            val redisCountersMap = counterRepository.getBulkBoardCounters(boardIds, userId)
            return boardIdsWithDbCounts.map { (boardId, dbViewCount, dbLikeCount) ->
                val redisCounters = redisCountersMap[boardId]
                BoardCounters(
                    boardId = boardId,
                    viewCount = dbViewCount + (redisCounters?.viewCount ?: 0),
                    likeCount = dbLikeCount + (redisCounters?.likeCount ?: 0),
                    userLiked = redisCounters?.userLiked ?: false,
                )
            }
        } catch (e: Exception) {
            log.error("Bulk 게시글({}) 카운터 조회 실패", boardIds, e)
            throw e
        }
    }

    private fun fetchDbViewCount(boardId: Long): Int = boardRepository.findViewCountById(boardId) ?: 0

    private fun fetchDbLikeCount(boardId: Long): Int = boardRepository.findLikeCountById(boardId) ?: 0

    @Scheduled(cron = "\${counter.scheduler.cron:0 */1 * * * *}") // @SchedulerLock 제거
    fun publishPendingCounterIncrementsToKafka() {
        val transactionTemplate = TransactionTemplate(transactionManager)
        val originalIncrementsForRollback = mutableMapOf<Long, Pair<Int, Int>>()
        var poppedBoardIdStrs: List<String> = emptyList()
        try {
            log.debug("카운터 배치 처리 스케줄러 시작")
            // popPendingBoardIdsAtomically는 List<String>을 반환하므로 toSet() 불필요 또는 타입 일치
            poppedBoardIdStrs = counterRepository.popPendingBoardIdsAtomically(schedulerBatchSize.toLong())
            if (poppedBoardIdStrs.isEmpty()) {
                log.debug("처리할 pending board ID 없음")
                return
            }

            log.info("Pending board ID {}개 가져옴 (SPOP)", poppedBoardIdStrs.size)

            val boardIdsToProcess = poppedBoardIdStrs.mapNotNull { it.toLongOrNull() }
            if (boardIdsToProcess.isEmpty()) {
                log.warn("유효한 boardId가 없어 처리를 중단합니다 (pop 결과: {})", poppedBoardIdStrs)
                return
            }

            val viewCountKeys = boardIdsToProcess.map { RedisCounterRepository.viewCountKey(it) }
            val likeCountKeys = boardIdsToProcess.map { RedisCounterRepository.likeCountKey(it) }

            // getAndResetIncrementsPipelined는 List<Int?> 반환. Long?으로 변환하는 것이 좋음.
            val viewIncrementsFromRedis: List<Int?>
            val likeIncrementsFromRedis: List<Int?>
            try {
                viewIncrementsFromRedis = counterRepository.getAndDeleteCountsPipelined(viewCountKeys)
                likeIncrementsFromRedis = counterRepository.getAndDeleteCountsPipelined(likeCountKeys)

                boardIdsToProcess.forEachIndexed { index, boardId ->
                    val vIncr = viewIncrementsFromRedis.getOrNull(index) ?: 0
                    val lIncr = likeIncrementsFromRedis.getOrNull(index) ?: 0
                    if (vIncr != 0 || lIncr != 0) {
                        originalIncrementsForRollback[boardId] = Pair(vIncr, lIncr)
                    }
                }
            } catch (redisEx: Exception) {
                log.error("Redis 증분값 Get&Reset 실패. boardIds: {}. PENDING_UPDATES 롤백 시도.", boardIdsToProcess, redisEx)
                safeRollbackPendingUpdatesOnly(poppedBoardIdStrs)
                return
            }

            val eventPayloadItems =
                originalIncrementsForRollback.map { (boardId, increments) ->
                    BoardIncrementItem(boardId, increments.first, increments.second)
                }

            if (eventPayloadItems.isEmpty()) {
                log.info("실제 증분할 카운터 없음 (Get&Reset 결과). boardIds: {}", boardIdsToProcess)
                return
            }

            val batchId = snowflakeGenerator.nextId().toString()
            transactionTemplate.executeWithoutResult { status ->
                try {
                    communityCommandEventPublisher.publishBulkBoardIncrements(
                        batchId = batchId,
                        eventPayloadItems,
                    )
                    log.info("Outbox 이벤트 발행 요청(ApplicationEvent 발행) 성공: batchId={}, 항목 수: {}", batchId, eventPayloadItems.size)
                } catch (e: Exception) {
                    log.error("Outbox 이벤트 발행 요청 또는 Outbox 저장 중 오류 발생. batchId={}. PENDING_UPDATES 및 증분값 롤백 시도.", batchId, e)
                    status.setRollbackOnly()
                    safeRollbackPendingUpdatesAndIncrements(poppedBoardIdStrs, originalIncrementsForRollback)
                }
            }
        } catch (e: Exception) {
            log.error("카운터 배치 처리 스케줄러 작업 중 예상치 못한 오류 발생.", e)
            if (poppedBoardIdStrs.isNotEmpty()) {
                safeRollbackPendingUpdatesAndIncrements(poppedBoardIdStrs, originalIncrementsForRollback)
            }
        }
    }

    private fun safeRollbackPendingUpdatesOnly(boardIdsToRollback: Collection<String>) {
        if (boardIdsToRollback.isEmpty()) return
        try {
            counterRepository.addBoardIdsToPending(boardIdsToRollback)
            log.info("PENDING_UPDATES (ID만) 롤백 성공: {}개 ID 다시 추가됨", boardIdsToRollback.size)
        } catch (rollbackEx: Exception) {
            log.error("PENDING_UPDATES (ID만) 롤백 중 추가 오류 발생. boardIds: {}", boardIdsToRollback, rollbackEx)
        }
    }

    private fun safeRollbackPendingUpdatesAndIncrements(
        boardIdsToRollback: Collection<String>, // Set<String> 또는 List<String>
        incrementsToRollback: Map<Long, Pair<Int, Int>>,
    ) {
        if (boardIdsToRollback.isEmpty() && incrementsToRollback.isEmpty()) return
        log.warn("PENDING_UPDATES 및 증분값 롤백 시도. 대상 ID 수: {}, 증분값 항목 수: {}", boardIdsToRollback.size, incrementsToRollback.size)

        try {
            if (incrementsToRollback.isNotEmpty()) {
                counterRepository.rollbackIncrements(incrementsToRollback)
                log.info("증분값 롤백 성공 (시도). 항목 수: {}", incrementsToRollback.size)
            }
        } catch (incrementRollbackEx: Exception) {
            log.error("증분값 롤백 중 오류 발생. 데이터 불일치 가능성 높음. 수동 개입 필요 알림.", incrementRollbackEx)
        }
        safeRollbackPendingUpdatesOnly(boardIdsToRollback)
    }
}
