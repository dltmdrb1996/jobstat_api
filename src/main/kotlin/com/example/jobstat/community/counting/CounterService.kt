package com.example.jobstat.community.counting

import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.community.counting.RedisCounterRepository.Companion.likeCountKey
import com.example.jobstat.community.counting.RedisCounterRepository.Companion.viewCountKey
import com.example.jobstat.core.core_error.model.AppException // AppException 임포트 확인 필요
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * 실시간 카운터 관리 및 배치 처리를 담당하는 서비스.
 * - 실시간 요청 처리 (좋아요, 조회수 증가 등 - 최적화된 Repository 호출)
 * - 최종 카운터 정보 조회 (DB + Redis 통합 - 최적화된 Repository 호출)
 * - 주기적인 배치 실행 (Redis 카운터를 읽어 DB에 반영)
 */
@Service
internal class CounterService(
    private val counterRepository: CounterRepository,
    private val boardRepository: BoardRepository, // RDB 조회용
    private val counterBatchService: CounterBatchService, // 배치 처리용
    @Value("\${counter.max-retry-count:3}") private val maxRetryCount: Int = 3,
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    private val processingFailureCache = ConcurrentHashMap<Long, Int>()

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
            val newCount = counterRepository.atomicIncrementViewCountAndAddPending(boardId)
            log.debug("게시글 ID {} 조회수 증가 완료, 현재 Redis 카운트: {}", boardId, newCount)
            processingFailureCache.remove(boardId)
        } catch (e: Exception) {
            log.error("조회수 증가 실패: boardId={}", boardId, e)
            throw e
        }
    }

    fun incrementLikeCount(
        boardId: Long,
        userId: String,
        dbLikeCount: Int? = null,
    ): Int {
        try {
            val actualDbCount = dbLikeCount ?: fetchDbLikeCount(boardId)
            val redisCount = counterRepository.atomicLikeOperation(boardId, userId)
            processingFailureCache.remove(boardId)
            return (actualDbCount + redisCount)
        } catch (e: AppException) {
            log.warn("좋아요 처리 중 예상된 예외: boardId={}, userId={}, error={}", boardId, userId, e.message)
            throw e
        } catch (e: Exception) {
            log.error("좋아요 증가 실패: boardId={}, userId={}", boardId, userId, e)
            throw e
        }
    }

    fun decrementLikeCount(
        boardId: Long,
        userId: String,
        dbLikeCount: Int? = null,
    ): Int {
        try {
            val actualDbCount = dbLikeCount ?: fetchDbLikeCount(boardId)
            val redisCount = counterRepository.atomicUnlikeOperation(boardId, userId)
            processingFailureCache.remove(boardId)
            return (actualDbCount + redisCount)
        } catch (e: AppException) {
            log.warn("좋아요 취소 처리 중 예상된 예외: boardId={}, userId={}, error={}", boardId, userId, e.message)
            throw e
        } catch (e: Exception) {
            log.error("좋아요 취소 실패: boardId={}, userId={}", boardId, userId, e)
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

            val finalDbViewCount = dbViewCount ?: fetchDbViewCount(boardId)
            val finalDbLikeCount = dbLikeCount ?: fetchDbLikeCount(boardId)

            return BoardCounters(
                boardId = boardId,
                viewCount = finalDbViewCount + redisCounters.viewCount,
                likeCount = finalDbLikeCount + redisCounters.likeCount,
                userLiked = redisCounters.userLiked,
            )
        } catch (e: Exception) {
            log.error("단일 게시글({}) 카운터 조회 실패", boardId, e)
            throw e
        }
    }

    fun getBulkBoardCounters(
        boardIdsWithCounts: List<Triple<Long, Int, Int>>,
        userId: String? = null,
    ): List<BoardCounters> {
        if (boardIdsWithCounts.isEmpty()) return emptyList()

        val boardIds = boardIdsWithCounts.map { it.first }

        try {
            val redisCountersMap = counterRepository.getBulkBoardCounters(boardIds, userId)

            return boardIdsWithCounts.map { (boardId, dbViewCount, dbLikeCount) ->
                val redisCounters = redisCountersMap[boardId]

                val finalViewCount = dbViewCount + (redisCounters?.viewCount ?: 0)
                val finalLikeCount = dbLikeCount + (redisCounters?.likeCount ?: 0)
                val finalUserLiked = redisCounters?.userLiked ?: false

                BoardCounters(
                    boardId = boardId,
                    viewCount = finalViewCount,
                    likeCount = finalLikeCount,
                    userLiked = finalUserLiked,
                )
            }
        } catch (e: Exception) {
            log.error("Bulk 게시글({}) 카운터 조회 실패", boardIds, e)
            throw e
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    fun flushCountersToDatabase() {
        log.debug("Redis 카운터 DB 동기화 시작")
        val startTime = System.currentTimeMillis()
        var processedCount = 0
        try {
            val pendingBoardIdStrs = counterRepository.fetchPendingBoardIds()
            if (pendingBoardIdStrs.isEmpty()) {
                log.debug("DB 동기화 대상 게시글 없음")
                return
            }
            processedCount = pendingBoardIdStrs.size
            log.debug("총 {}개 게시글 업데이트 처리 시작", processedCount)

            val boardIds = pendingBoardIdStrs.mapNotNull { it.toLongOrNull() }
            val invalidIdStrs = pendingBoardIdStrs.filter { it.toLongOrNull() == null }

            if (invalidIdStrs.isNotEmpty()) {
                log.error("잘못된 형식의 boardId 발견 (Pending Set에서 제거 예정): {}", invalidIdStrs)
            }
            if (boardIds.isEmpty() && invalidIdStrs.isNotEmpty()) {
                log.warn("유효한 boardId는 없으나 잘못된 형식의 ID가 있어 Pending Set에서 제거 시도")
                counterRepository.removePendingBoardIds(invalidIdStrs) // 잘못된 ID 제거
                return
            }
            if (boardIds.isEmpty()) {
                log.warn("DB 동기화 대상 유효 boardId 없음")
                return
            }

            val viewCountKeys = boardIds.map { viewCountKey(it) }
            val likeCountKeys = boardIds.map { likeCountKey(it) }

            val viewCounts: List<Int?>
            val likeCounts: List<Int?>
            try {
                // GetAndDelete 동시 수행
                viewCounts = counterRepository.getAndDeleteCountsPipelined(viewCountKeys)
                likeCounts = counterRepository.getAndDeleteCountsPipelined(likeCountKeys)
                log.debug("Redis Get&Delete 파이프라인 실행 완료 ({}개 boardId)", boardIds.size)
            } catch (e: Exception) {
                log.error("Redis Get&Delete 파이프라인 실행 중 오류 발생, 배치 중단", e)
                return
            }

            if (viewCounts.size != boardIds.size || likeCounts.size != boardIds.size) {
                log.error(
                    "Get&Delete 파이프라인 결과 크기 불일치! 요청: {}, 응답: view={}, like={}",
                    boardIds.size,
                    viewCounts.size,
                    likeCounts.size,
                )
            }
            val boardIdToCounts =
                boardIds.indices.associate { index ->
                    val boardId = boardIds[index]
                    val viewCount = viewCounts.getOrNull(index) ?: 0
                    val likeCount = likeCounts.getOrNull(index) ?: 0
                    boardId to Pair(viewCount, likeCount)
                }

            val (successfulStrs, failedStrs) = processBoardUpdates(boardIdToCounts)

            val allAttemptedIds = pendingBoardIdStrs.toList()
            finalizeProcessingResults(successfulStrs, failedStrs, allAttemptedIds)

            val duration = System.currentTimeMillis() - startTime
            log.debug("Redis 카운터 DB 동기화 완료 ({}개 처리 시도, {}ms 소요)", processedCount, duration)
        } catch (e: Exception) {
            // 예상치 못한 전체 배치 실패
            log.error("카운터 DB 동기화 전체 실패", e)
        }
    }

    private fun processBoardUpdates(boardIdToCounts: Map<Long, Pair<Int, Int>>): ProcessingResults {
        val successful = mutableListOf<String>()
        val failed = mutableListOf<String>()

        boardIdToCounts.forEach { (boardId, counts) ->
            val (viewCount, likeCount) = counts
            val boardIdStr = boardId.toString()

            if (shouldSkipProcessing(boardId)) { // 재시도 횟수 초과 확인
                failed.add(boardIdStr)
                log.warn("게시글 ID {} 최대 재시도({}) 초과하여 DB 업데이트 건너뜀", boardId, maxRetryCount)
                return@forEach
            }

            try {
                val processed = counterBatchService.processSingleBoardCounter(boardId, viewCount, likeCount)
                if (processed) {
                    successful.add(boardIdStr)
                    processingFailureCache.remove(boardId)
                } else {
                    failed.add(boardIdStr)
                    incrementFailCount(boardId)
                }
            } catch (e: Exception) {
                log.error("게시글 ID {} DB 처리 호출 중 예외 발생 (호출 단계)", boardId, e)
                failed.add(boardIdStr)
                incrementFailCount(boardId)
            }
        }
        return ProcessingResults(successful, failed)
    }

    private fun finalizeProcessingResults(
        successful: List<String>,
        failed: List<String>,
        allProcessedIds: List<String>,
    ) {
        try {
            if (allProcessedIds.isNotEmpty()) {
                counterRepository.removePendingBoardIds(allProcessedIds)
                log.debug("총 {}개 ID 처리 시도 후 Pending Set에서 제거 완료", allProcessedIds.size)
            }

            if (successful.isNotEmpty()) {
                log.debug("{}개 게시글 카운터 DB 업데이트 성공", successful.size)
            }
            if (failed.isNotEmpty()) {
                log.warn("{}개 게시글 카운터 DB 업데이트 실패 또는 건너뜀", failed.size)
            }
        } catch (e: Exception) {
            log.error("Pending Set 제거 중 예외 발생", e)
        }
    }

    private fun shouldSkipProcessing(boardId: Long): Boolean {
        val failCount = processingFailureCache.getOrDefault(boardId, 0)
        return failCount >= maxRetryCount
    }

    private fun incrementFailCount(boardId: Long) {
        processingFailureCache[boardId] = processingFailureCache.getOrDefault(boardId, 0) + 1
    }

    private data class ProcessingResults(
        val successful: List<String>,
        val failed: List<String>,
    )

    private fun fetchDbViewCount(boardId: Long): Int =
        try {
            boardRepository.findViewCountById(boardId) ?: 0
        } catch (e: Exception) {
            log.error("DB 조회수 조회 실패: boardId={}", boardId, e)
            0
        }

    private fun fetchDbLikeCount(boardId: Long): Int =
        try {
            boardRepository.findLikeCountById(boardId) ?: 0
        } catch (e: Exception) {
            log.error("DB 좋아요 수 조회 실패: boardId={}", boardId, e)
            0
        }
}
