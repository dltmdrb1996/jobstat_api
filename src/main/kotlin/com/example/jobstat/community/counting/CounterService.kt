package com.example.jobstat.community.counting

import com.example.jobstat.community.CommunityEventPublisher
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.core.constants.RedisKeyConstants
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.RedisOperationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Redis를 활용한 게시글 조회수 및 좋아요 카운트 최적화 서비스 (개선 버전)
 * - 카운트 증가는 Redis에 즉시 반영
 * - 주기적으로(5분) DB에 업데이트하고 이벤트 발행
 * - 향상된 예외 처리 및 키 관리 적용
 * - 멱등성 보장 및 데이터 정합성 강화
 * - DB 중복 호출 최적화 적용
 * - 가독성 개선을 위한 코드 리팩토링
 */
@Service
internal class CounterService(
    private val redisTemplate: StringRedisTemplate,
    private val boardRepository: BoardRepository,
    private val communityEventPublisher: CommunityEventPublisher,
    @Value("\${counter.max-retry-count:3}") private val maxRetryCount: Int = 3 // 최대 처리 재시도 횟수
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    // 처리 실패 게시글 추적을 위한 캐시
    private val processingFailureCache = ConcurrentHashMap<Long, Int>() // boardId -> 실패 횟수

    /**
     * 게시글 조회수 증가
     *
     * @param boardId 게시글 ID
     * @param userId 사용자 ID (중복 조회 방지용)
     * @param dbViewCount 이미 로드된 DB의 조회수 (null인 경우 DB에서 조회)
     * @return 현재 추정 조회수
     */
    fun incrementViewCount(boardId: Long, userId: String?, dbViewCount: Int? = null): Int =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 증가",
            detailInfo = "boardId: $boardId, userId: $userId",
            errorCode = ErrorCode.VIEW_COUNT_UPDATE_FAILED
        ) {
            // 1. Redis에 조회수 증가 및 대기 목록에 추가
            val key = RedisKeyConstants.Counter.viewCountKey(boardId)
            val redisIncrement = redisTemplate.opsForValue().increment(key) ?: 1L

            // 2. 처리 대기 목록에 추가 및 실패 캐시 초기화
            with(redisTemplate.opsForSet()) {
                add(RedisKeyConstants.Counter.PENDING_UPDATES, boardId.toString())
            }
            processingFailureCache.remove(boardId)

            // 3. DB 카운트 조회 및 총합계 계산
            val actualDbCount = dbViewCount ?: boardRepository.findViewCountById(boardId) ?: 0
            (redisIncrement + actualDbCount).toInt()
        }

    /**
     * 현재 게시글의 조회수 조회 (Redis + DB)
     */
    fun getViewCount(boardId: Long, dbViewCount: Int? = null): Int =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 조회",
            detailInfo = "boardId: $boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            // DB 값과 Redis 값 합산
            val actualDbCount = dbViewCount ?: boardRepository.findViewCountById(boardId) ?: 0
            val redisCount = redisTemplate.opsForValue()
                .get(RedisKeyConstants.Counter.viewCountKey(boardId))?.toIntOrNull() ?: 0

            actualDbCount + redisCount
        }

    /**
     * 게시글 좋아요 증가
     */
    fun incrementLikeCount(boardId: Long, userId: String, dbLikeCount: Int? = null): Int =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "좋아요 증가",
            detailInfo = "boardId: $boardId, userId: $userId",
            errorCode = ErrorCode.LIKE_COUNT_UPDATE_FAILED
        ) {
            // 1. 사용자가 이미 좋아요를 눌렀는지 확인
            val isFirstOperation = checkIfFirstLikeOperation(boardId, userId)
            if (!isFirstOperation) {
                return@executeWithRetry getLikeCount(boardId, dbLikeCount)
            }

            // 2. 오늘 이미 좋아요를 눌렀는지 확인
            val dailyLikeKey = RedisKeyConstants.Counter.dailyLikeUserKey(userId, boardId)
            val todayLiked = redisTemplate.opsForValue().get(dailyLikeKey) != null
            if (todayLiked) {
                return@executeWithRetry getLikeCount(boardId, dbLikeCount)
            }

            // 3. 일일 좋아요 기록 설정 후 좋아요 카운트 증가
            val remainingSeconds = RedisOperationUtils.calculateSecondsUntilMidnight()
            redisTemplate.opsForValue().set(dailyLikeKey, "1", Duration.ofSeconds(remainingSeconds))

            // 4. 좋아요 관련 Redis 처리 한 번에 수행
            val likeData = with(redisTemplate) {
                val countKey = RedisKeyConstants.Counter.likeCountKey(boardId)
                val userLikeKey = RedisKeyConstants.Counter.likeUsersKey(boardId)

                // 좋아요 수 증가
                val newCount = opsForValue().increment(countKey) ?: 1L

                // 사용자 목록에 추가 및 대기 목록에 등록
                opsForSet().add(userLikeKey, userId)
                opsForSet().add(RedisKeyConstants.Counter.PENDING_UPDATES, boardId.toString())

                newCount
            }

            // 5. 실패 캐시 초기화
            processingFailureCache.remove(boardId)

            // 6. 총 좋아요 수 계산 및 이벤트 발행
            val actualDbCount = dbLikeCount ?: boardRepository.findLikeCountById(boardId) ?: 0
            val totalCount = (likeData + actualDbCount).toInt()

            // 리드모델에 이벤트 소싱
            communityEventPublisher.publishBoardLiked(
                boardId = boardId,
                userId = userId.toLong(),
                likeCount = totalCount
            )

            totalCount
        }

    /**
     * 게시글 좋아요 취소
     */
    fun decrementLikeCount(boardId: Long, userId: String, dbLikeCount: Int? = null): Int =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "좋아요 취소",
            detailInfo = "boardId: $boardId, userId: $userId",
            errorCode = ErrorCode.LIKE_COUNT_UPDATE_FAILED
        ) {
            // 1. 사용자의 좋아요 상태 원자적으로 제거 시도
            val userLikeKey = RedisKeyConstants.Counter.likeUsersKey(boardId)
            val wasLiked = redisTemplate.execute { connection ->
                (connection.setCommands().sRem(
                    userLikeKey.toByteArray(),
                    userId.toByteArray()
                ) ?: 0) > 0
            } ?: false

            // 2. 좋아요를 누른 적이 없으면 현재 카운트만 반환
            if (!wasLiked) {
                return@executeWithRetry getLikeCount(boardId, dbLikeCount)
            }

            // 3. 좋아요 관련 Redis 처리 한 번에 수행
            val decrementData = with(redisTemplate) {
                // 일일 좋아요 기록 삭제 - 당일 다시 좋아요 할 수 있도록
                val dailyLikeKey = RedisKeyConstants.Counter.dailyLikeUserKey(userId, boardId)
                delete(dailyLikeKey)

                // 좋아요 수 감소 및 대기 목록에 추가
                val countKey = RedisKeyConstants.Counter.likeCountKey(boardId)
                val newCount = opsForValue().decrement(countKey) ?: 0L
                opsForSet().add(RedisKeyConstants.Counter.PENDING_UPDATES, boardId.toString())

                newCount
            }

            // 4. 실패 캐시 초기화
            processingFailureCache.remove(boardId)

            // 5. 총 좋아요 수 계산 및 이벤트 발행
            val actualDbCount = dbLikeCount ?: boardRepository.findLikeCountById(boardId) ?: 0
            val totalCount = (decrementData + actualDbCount).toInt()

            // 좋아요 취소 이벤트 발행
            communityEventPublisher.publishBoardUnliked(
                boardId = boardId,
                userId = userId.toLong(),
                likeCount = totalCount
            )

            totalCount
        }

    /**
     * 현재 게시글의 좋아요 수 조회 (Redis + DB)
     */
    fun getLikeCount(boardId: Long, dbLikeCount: Int? = null): Int =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "좋아요 수 조회",
            detailInfo = "boardId: $boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            // DB 값과 Redis 값 합산
            val actualDbCount = dbLikeCount ?: boardRepository.findLikeCountById(boardId) ?: 0
            val redisCount = redisTemplate.opsForValue()
                .get(RedisKeyConstants.Counter.likeCountKey(boardId))?.toIntOrNull() ?: 0

            actualDbCount + redisCount
        }

    /**
     * 사용자가 게시글에 좋아요를 눌렀는지 확인
     */
    fun hasUserLiked(boardId: Long, userId: String): Boolean =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "사용자 좋아요 확인",
            detailInfo = "boardId: $boardId, userId: $userId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForSet().isMember(
                RedisKeyConstants.Counter.likeUsersKey(boardId),
                userId
            ) ?: false
        }

    /**
     * 사용자가 오늘 특정 게시글에 좋아요를 눌렀는지 확인
     */
    fun hasUserLikedToday(boardId: Long, userId: String): Boolean =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "사용자 일일 좋아요 확인",
            detailInfo = "boardId: $boardId, userId: $userId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val dailyLikeKey = RedisKeyConstants.Counter.dailyLikeUserKey(userId, boardId)
            redisTemplate.opsForValue().get(dailyLikeKey) != null
        }

    /**
     * 정기적으로 Redis의 카운터를 DB에 반영하고 이벤트 발행 (5분마다 실행)
     */
    @Scheduled(fixedDelayString = "\${counter.flush-interval-milliseconds:300000}")
    fun flushCountersToDatabase() {
        log.info("Redis 카운터 DB 동기화 시작")

        try {
            // 1. 대기 중인 업데이트 게시글 ID 목록 가져오기
            val pendingBoardIds = fetchPendingBoardIds()
            if (pendingBoardIds.isEmpty()) {
                log.info("업데이트할 게시글 없음")
                return
            }

            log.info("총 {}개 게시글 업데이트 처리 시작", pendingBoardIds.size)

            // 2. 게시글 처리 결과 추적
            val results = processPendingBoards(pendingBoardIds)

            // 3. 처리 결과 정리
            finalizeProcessingResults(results.successful, results.failed)

        } catch (e: Exception) {
            log.error("Redis 카운터 DB 동기화 전체 실패", e)
        }
    }

    /**
     * 대기 중인 업데이트 게시글 ID 목록 가져오기
     */
    private fun fetchPendingBoardIds(): Set<String> =
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "대기 업데이트 목록 조회",
            detailInfo = "PENDING_UPDATES",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForSet().members(RedisKeyConstants.Counter.PENDING_UPDATES) ?: emptySet()
        }

    /**
     * 대기 중인 게시글들 처리
     */
    private fun processPendingBoards(pendingBoardIds: Set<String>): ProcessingResults {
        val successfullyProcessed = mutableListOf<String>()
        val failedToProcess = mutableListOf<String>()

        pendingBoardIds.forEach { boardIdStr ->
            try {
                val boardId = boardIdStr.toLong()

                // 최대 재시도 횟수 초과 게시글 건너뛰기
                if (shouldSkipProcessing(boardId)) {
                    failedToProcess.add(boardIdStr)
                    return@forEach
                }

                // 개별 게시글 처리 (별도 트랜잭션으로 분리)
                val processed = processSingleBoardCounter(boardId)

                if (processed) {
                    successfullyProcessed.add(boardIdStr)
                    processingFailureCache.remove(boardId)
                } else {
                    failedToProcess.add(boardIdStr)
                    incrementFailCount(boardId)
                }
            } catch (e: Exception) {
                log.error("게시글 ID {} 카운터 업데이트 처리 예외 발생", boardIdStr, e)
                failedToProcess.add(boardIdStr)

                try {
                    val boardId = boardIdStr.toLong()
                    incrementFailCount(boardId)
                } catch (e: Exception) {
                    log.error("게시글 ID 변환 실패: {}", boardIdStr, e)
                }
            }
        }

        return ProcessingResults(successfullyProcessed, failedToProcess)
    }

    /**
     * 처리 결과 정리
     */
    private fun finalizeProcessingResults(successful: List<String>, failed: List<String>) {
        if (successful.isNotEmpty()) {
            try {
                RedisOperationUtils.executeWithRetry(
                    logger = log,
                    operationName = "처리 완료 게시글 목록 제거",
                    detailInfo = "successfullyProcessed: ${successful.size}",
                    errorCode = ErrorCode.REDIS_OPERATION_FAILED
                ) {
                    redisTemplate.opsForSet().remove(
                        RedisKeyConstants.Counter.PENDING_UPDATES,
                        *successful.toTypedArray()
                    )
                }
                log.info("{}개 게시글 카운터 업데이트 성공", successful.size)
            } catch (e: Exception) {
                log.error("처리 완료 게시글 대기 목록 정리 실패, 다음 배치에서 재처리될 수 있음", e)
            }
        }

        if (failed.isNotEmpty()) {
            log.warn("{}개 게시글 카운터 업데이트 실패, 다음 배치에서 재시도됨", failed.size)
        }
    }

    /**
     * 최대 재시도 횟수를 초과했는지 확인
     */
    private fun shouldSkipProcessing(boardId: Long): Boolean {
        val failCount = processingFailureCache[boardId] ?: 0
        if (failCount >= maxRetryCount) {
            log.warn("게시글 ID {} 최대 재시도 횟수({}) 초과, 처리 건너뛰기", boardId, maxRetryCount)
            processingFailureCache.remove(boardId)
            return true
        }
        return false
    }

    /**
     * 실패 횟수 증가
     */
    private fun incrementFailCount(boardId: Long) {
        processingFailureCache[boardId] = (processingFailureCache[boardId] ?: 0) + 1
    }

    /**
     * 사용자가 처음으로 좋아요를 누르는지 확인
     */
    private fun checkIfFirstLikeOperation(boardId: Long, userId: String): Boolean {
        return redisTemplate.execute { connection ->
            val userLikeKey = RedisKeyConstants.Counter.likeUsersKey(boardId)
            connection.setCommands().sIsMember(
                userLikeKey.toByteArray(),
                userId.toByteArray()
            ) == false
        } ?: false
    }

    /**
     * 단일 게시글의 카운터 처리 (별도 트랜잭션으로 분리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processSingleBoardCounter(boardId: Long): Boolean {
        try {
            log.debug("게시글 ID {} 카운터 처리 시작", boardId)

            // 1. Redis에서 카운터 값 추출 및 초기화
            val (viewCount, likeCount) = extractAndResetCounters(boardId)

            // 2. 처리할 값이 없으면 성공으로 판단
            if (viewCount == 0 && likeCount == 0) {
                log.debug("게시글 ID {} 카운터 변경 없음", boardId)
                return true
            }

            // 3. 게시글 찾기
            val board = findBoard(boardId) ?: run {
                // 실패 시 카운터 복원
                restoreCounters(boardId, viewCount, likeCount)
                return false
            }

            // 4. DB 업데이트
            board.apply {
                if (viewCount != 0) {
                    incrementViewCount(viewCount)
                    log.debug("게시글 ID {} 조회수 {} 증가", boardId, viewCount)
                }

                if (likeCount != 0) {
                    incrementLikeCount(likeCount)
                    log.debug("게시글 ID {} 좋아요 수 {} 변경", boardId, likeCount)
                }
            }

            // 5. 이벤트 발행 (조회수만 - 좋아요는 이미 즉시 발행됨)
            return publishBoardEvents(board, viewCount, likeCount)
        } catch (e: Exception) {
            log.error("게시글 ID {} 카운터 처리 중 예외 발생", boardId, e)
            return false
        }
    }

    /**
     * Redis에서 카운터 값 추출 및 초기화
     */
    private fun extractAndResetCounters(boardId: Long): Pair<Int, Int> {
        // 조회수 추출 및 초기화
        val viewCount = RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 카운터 추출",
            detailInfo = "boardId: $boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForValue().getAndSet(
                RedisKeyConstants.Counter.viewCountKey(boardId),
                "0"
            )?.toIntOrNull() ?: 0
        }

        // 좋아요 수 추출 및 초기화
        val likeCount = RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "좋아요 카운터 추출",
            detailInfo = "boardId: $boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForValue().getAndSet(
                RedisKeyConstants.Counter.likeCountKey(boardId),
                "0"
            )?.toIntOrNull() ?: 0
        }

        return viewCount to likeCount
    }

    /**
     * 게시글 엔티티 조회
     */
    private fun findBoard(boardId: Long): Board? {
        return try {
            boardRepository.findById(boardId)
        } catch (e: Exception) {
            log.error("게시글 ID {} 찾기 실패", boardId, e)
            null
        }
    }

    /**
     * 카운터 복원 (게시글 찾기 실패 시)
     */
    private fun restoreCounters(boardId: Long, viewCount: Int, likeCount: Int) {
        if (viewCount != 0) {
            redisTemplate.opsForValue().increment(
                RedisKeyConstants.Counter.viewCountKey(boardId),
                viewCount.toLong()
            )
        }

        if (likeCount != 0) {
            redisTemplate.opsForValue().increment(
                RedisKeyConstants.Counter.likeCountKey(boardId),
                likeCount.toLong()
            )
        }
    }

    /**
     * 이벤트 발행
     */
    private fun publishBoardEvents(board: Board, viewCount: Int, likeCount: Int): Boolean {
        try {
            // 조회수 이벤트만 발행 (좋아요는 이미 즉시 발행됨)
            if (viewCount != 0) {
                communityEventPublisher.publishBoardViewCountUpdated(
                    boardId = board.id,
                    viewCount = board.viewCount,
                    incrementAmount = viewCount
                )
            }

            log.info(
                "게시글 ID {} 카운터 업데이트 완료: 조회수 {}{}개, 좋아요 {}{}개",
                board.id,
                if (viewCount >= 0) "+" else "", viewCount,
                if (likeCount >= 0) "+" else "", likeCount
            )

            return true
        } catch (e: Exception) {
            log.error("게시글 ID {} 이벤트 발행 실패", board.id, e)

            // Redis 카운터 부분 복원 - 이벤트 발행 실패에 대한 복구
            if (viewCount != 0) {
                redisTemplate.opsForValue().increment(
                    RedisKeyConstants.Counter.viewCountKey(board.id),
                    viewCount.toLong()
                )
            }

            return false
        }
    }

    /**
     * 처리 결과를 담는 데이터 클래스
     */
    private data class ProcessingResults(
        val successful: List<String>,
        val failed: List<String>
    )
}