package com.example.jobstat.community.counting

import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.community.counting.CounterRepository.Companion.likeCountKey
import com.example.jobstat.community.counting.CounterRepository.Companion.viewCountKey
import com.example.jobstat.core.error.AppException // AppException 임포트 확인 필요
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

    // DB 업데이트 실패 시 재시도 횟수 추적용 캐시
    private val processingFailureCache = ConcurrentHashMap<Long, Int>()

    // 최종 카운터 정보 DTO
    data class BoardCounters(
        val boardId: Long,
        val viewCount: Int,
        val likeCount: Int,
        val userLiked: Boolean,
    )

    // ========================================
    // 1. 카운터 삭제/관리
    // ========================================

    /**
     * 게시글 삭제 시 관련된 Redis 카운터 데이터를 정리합니다.
     */
    fun cleanupBoardCounters(boardId: Long) {
        log.info("게시글 ID {}의 카운터 정리 요청", boardId)
        try {
            counterRepository.deleteBoardCounters(boardId)
        } catch (e: Exception) {
            log.error("게시글 ID {} 카운터 정리 중 오류 발생", boardId, e)
            // 필요시 예외를 다시 던지거나 특정 방식으로 처리
            throw e // 예: UseCase 레벨에서 처리하도록 다시 던짐
        }
    }

    // ========================================
    // 2. 조회수 증가 (원자적)
    // ========================================

    /**
     * 게시글 조회수를 증가시키고 배치 처리 대기 목록에 추가합니다. (원자적 Redis 작업)
     */
    fun incrementViewCount(boardId: Long) {
        try {
            val newCount = counterRepository.atomicIncrementViewCountAndAddPending(boardId)
            log.debug("게시글 ID {} 조회수 증가 완료, 현재 Redis 카운트: {}", boardId, newCount)
            // 조회수 증가 성공 시, 이전 DB 업데이트 실패 기록 제거
            processingFailureCache.remove(boardId)
        } catch (e: Exception) {
            log.error("조회수 증가 실패: boardId={}", boardId, e)
            // 예외를 다시 던져 UseCase 레벨 등에서 처리하도록 함
            throw e
        }
    }

    // ========================================
    // 3. 좋아요 처리 (원자적)
    // ========================================

    /**
     * 게시글 좋아요 처리 (원자적 Redis 작업).
     * @return 최종 좋아요 수 (DB + Redis)
     */
    fun incrementLikeCount(
        boardId: Long,
        userId: String,
        dbLikeCount: Int? = null, // 미리 조회된 DB 카운트 (선택적)
    ): Int {
        try {
            // 1. RDB 카운터 확인 (없으면 조회)
            val actualDbCount = dbLikeCount ?: fetchDbLikeCount(boardId)
            // 2. Redis 원자적 좋아요 처리 (Redis 카운트 반환)
            val redisCount = counterRepository.atomicLikeOperation(boardId, userId)
            // 좋아요 성공 시, 이전 DB 업데이트 실패 기록 제거
            processingFailureCache.remove(boardId)
            // 3. 최종 카운트 반환
            return (actualDbCount + redisCount)
        } catch (e: AppException) {
            // AppException (예: 이미 좋아요)은 그대로 던짐
            log.warn("좋아요 처리 중 예상된 예외: boardId={}, userId={}, error={}", boardId, userId, e.message)
            throw e
        } catch (e: Exception) {
            // 그 외 Redis 오류 등
            log.error("좋아요 증가 실패: boardId={}, userId={}", boardId, userId, e)
            throw e // 예외 다시 던지기
        }
    }

    /**
     * 게시글 좋아요 취소 처리 (원자적 Redis 작업).
     * @return 최종 좋아요 수 (DB + Redis)
     */
    fun decrementLikeCount(
        boardId: Long,
        userId: String,
        dbLikeCount: Int? = null, // 미리 조회된 DB 카운트 (선택적)
    ): Int {
        try {
            // 1. RDB 카운터 확인 (없으면 조회)
            val actualDbCount = dbLikeCount ?: fetchDbLikeCount(boardId)
            // 2. Redis 원자적 좋아요 취소 처리 (Redis 카운트 반환)
            val redisCount = counterRepository.atomicUnlikeOperation(boardId, userId)
            // 좋아요 취소 성공 시, 이전 DB 업데이트 실패 기록 제거
            processingFailureCache.remove(boardId)
            // 3. 최종 카운트 반환
            return (actualDbCount + redisCount)
        } catch (e: AppException) {
            // AppException (예: 이미 취소)은 그대로 던짐
            log.warn("좋아요 취소 처리 중 예상된 예외: boardId={}, userId={}, error={}", boardId, userId, e.message)
            throw e
        } catch (e: Exception) {
            // 그 외 Redis 오류 등
            log.error("좋아요 취소 실패: boardId={}, userId={}", boardId, userId, e)
            throw e // 예외 다시 던지기
        }
    }

    // ========================================
    // 4. 최종 카운터 정보 조회 (최적화)
    // ========================================

    /**
     * 단일 게시글의 최종 카운터 정보 (DB + Redis) 조회 (최적화됨)
     * RDB 카운트를 선택적으로 받거나 직접 조회합니다.
     */
    fun getSingleBoardCounters(
        boardId: Long,
        userId: String?, // 좋아요 여부 확인용
        dbViewCount: Int? = null, // RDB 조회수 (없으면 조회)
        dbLikeCount: Int? = null, // RDB 좋아요 수 (없으면 조회)
    ): BoardCounters {
        try {
            // 1. Redis에서 카운터 정보 한 번에 가져오기 (최적화된 Repository 호출)
            val redisCounters = counterRepository.getSingleBoardCountersFromRedis(boardId, userId)

            // 2. RDB 카운터 확인 (없으면 조회)
            val finalDbViewCount = dbViewCount ?: fetchDbViewCount(boardId)
            val finalDbLikeCount = dbLikeCount ?: fetchDbLikeCount(boardId)

            // 3. 결과 조합
            return BoardCounters(
                boardId = boardId,
                viewCount = finalDbViewCount + redisCounters.viewCount,
                likeCount = finalDbLikeCount + redisCounters.likeCount,
                userLiked = redisCounters.userLiked, // Redis 결과 사용
            )
        } catch (e: Exception) {
            log.error("단일 게시글({}) 카운터 조회 실패", boardId, e)
            // Redis 조회 실패 시 DB 값만이라도 반환할지, 예외를 던질지 정책 결정 필요
            // 여기서는 예외를 다시 던지는 것으로 가정
            throw e
        }
    }

    /**
     * 여러 게시글의 최종 카운터 정보 일괄 조회 (DB + Redis) (최적화됨)
     * RDB 카운트 정보는 필수로 받습니다. (Triple 리스트)
     */
    fun getBulkBoardCounters(
        boardIdsWithCounts: List<Triple<Long, Int, Int>>, // (boardId, dbView, dbLike)
        userId: String? = null, // 좋아요 여부 확인용
    ): List<BoardCounters> {
        if (boardIdsWithCounts.isEmpty()) return emptyList()

        val boardIds = boardIdsWithCounts.map { it.first }

        try {
            val redisCountersMap = counterRepository.getBulkBoardCounters(boardIds, userId)

            // 2. 결과 조합
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
            // Bulk 조회 실패 시 일부 데이터만 반환하기 어려우므로 예외를 던지는 것이 일반적
            throw e
        }
    }

    // ========================================
    // 5. 배치 처리 (기존 유지)
    // ========================================

    /**
     * 매 5분마다 Redis 카운터를 DB에 동기화하는 배치 작업
     */
    @Scheduled(cron = "0 */5 * * * *") // 예시: 매 5분마다 실행
    fun flushCountersToDatabase() {
        log.info("Redis 카운터 DB 동기화 시작")
        val startTime = System.currentTimeMillis()
        var processedCount = 0
        try {
            // 1. 처리 대상 ID 목록 조회
            val pendingBoardIdStrs = counterRepository.fetchPendingBoardIds()
            if (pendingBoardIdStrs.isEmpty()) {
                log.info("DB 동기화 대상 게시글 없음")
                return
            }
            processedCount = pendingBoardIdStrs.size
            log.info("총 {}개 게시글 업데이트 처리 시작", processedCount)

            // 2. 유효한 Long ID 필터링
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

            // 3. Redis 파이프라인으로 카운터 값 가져오기 & 키 삭제
            val viewCountKeys = boardIds.map { viewCountKey(it) }
            val likeCountKeys = boardIds.map { likeCountKey(it) }

            val viewCounts: List<Int?>
            val likeCounts: List<Int?>
            try {
                // GetAndDelete 동시 수행
                viewCounts = counterRepository.getAndDeleteCountsPipelined(viewCountKeys)
                likeCounts = counterRepository.getAndDeleteCountsPipelined(likeCountKeys)
                log.info("Redis Get&Delete 파이프라인 실행 완료 ({}개 boardId)", boardIds.size)
            } catch (e: Exception) {
                log.error("Redis Get&Delete 파이프라인 실행 중 오류 발생, 배치 중단", e)
                // Redis 오류 시 배치를 중단하고 다음 기회에 재시도 (Pending ID는 남아있음)
                return
            }

            // 4. 결과 매핑 (boardId -> Pair(viewCount, likeCount))
            if (viewCounts.size != boardIds.size || likeCounts.size != boardIds.size) {
                log.error(
                    "Get&Delete 파이프라인 결과 크기 불일치! 요청: {}, 응답: view={}, like={}",
                    boardIds.size,
                    viewCounts.size,
                    likeCounts.size,
                )
                // 데이터 불일치 시, 일단 로그만 남기고 진행 (Pending ID는 아래에서 제거됨)
                // 또는 더 강력한 오류 처리 필요
            }
            val boardIdToCounts =
                boardIds.indices.associate { index ->
                    val boardId = boardIds[index]
                    val viewCount = viewCounts.getOrNull(index) ?: 0 // Get&Delete 결과가 null이면 0
                    val likeCount = likeCounts.getOrNull(index) ?: 0 // Get&Delete 결과가 null이면 0
                    boardId to Pair(viewCount, likeCount)
                }

            // 5. DB 업데이트 처리 (개별 트랜잭션 사용)
            val (successfulStrs, failedStrs) = processBoardUpdates(boardIdToCounts)

            // 6. 최종 결과 처리 (Pending Set 에서 처리 시도한 모든 ID 제거)
            val allAttemptedIds = pendingBoardIdStrs.toList() // 유효/무효 ID 모두 포함
            finalizeProcessingResults(successfulStrs, failedStrs, allAttemptedIds)

            val duration = System.currentTimeMillis() - startTime
            log.info("Redis 카운터 DB 동기화 완료 ({}개 처리 시도, {}ms 소요)", processedCount, duration)
        } catch (e: Exception) {
            // 예상치 못한 전체 배치 실패
            log.error("카운터 DB 동기화 전체 실패", e)
        }
    }

    // DB 업데이트 처리 로직 (개별 트랜잭션)
    private fun processBoardUpdates(boardIdToCounts: Map<Long, Pair<Int, Int>>): ProcessingResults {
        val successful = mutableListOf<String>()
        val failed = mutableListOf<String>()

        boardIdToCounts.forEach { (boardId, counts) ->
            val (viewCount, likeCount) = counts
            val boardIdStr = boardId.toString()

            if (shouldSkipProcessing(boardId)) { // 재시도 횟수 초과 확인
                failed.add(boardIdStr)
                log.warn("게시글 ID {} 최대 재시도({}) 초과하여 DB 업데이트 건너뜀", boardId, maxRetryCount)
                return@forEach // 다음 boardId 처리
            }

            try {
                // CounterBatchService 호출 (REQUIRES_NEW 트랜잭션)
                val processed = counterBatchService.processSingleBoardCounter(boardId, viewCount, likeCount)
                if (processed) {
                    successful.add(boardIdStr)
                    processingFailureCache.remove(boardId) // 성공 시 실패 카운트 제거
                } else {
                    // processSingleBoardCounter 내부에서 에러 로깅됨 (DB 오류 등)
                    failed.add(boardIdStr)
                    incrementFailCount(boardId) // 실패 시 카운트 증가
                }
            } catch (e: Exception) {
                // CounterBatchService 호출 자체 예외 (거의 발생 안 함 가정)
                log.error("게시글 ID {} DB 처리 호출 중 예외 발생 (호출 단계)", boardId, e)
                failed.add(boardIdStr)
                incrementFailCount(boardId)
            }
        }
        return ProcessingResults(successful, failed)
    }

    // 배치 처리 결과 마무리 (Pending Set 정리)
    private fun finalizeProcessingResults(
        successful: List<String>,
        failed: List<String>,
        allProcessedIds: List<String>, // 처리 시도한 모든 ID (유효/무효 포함)
    ) {
        try {
            // 성공/실패/건너뜀 여부와 관계없이 처리 시도한 ID는 pending set에서 제거
            if (allProcessedIds.isNotEmpty()) {
                counterRepository.removePendingBoardIds(allProcessedIds)
                log.info("총 {}개 ID 처리 시도 후 Pending Set에서 제거 완료", allProcessedIds.size)
            }

            // 결과 로깅
            if (successful.isNotEmpty()) {
                log.info("{}개 게시글 카운터 DB 업데이트 성공", successful.size)
            }
            if (failed.isNotEmpty()) {
                // 실패 목록에는 실제 DB 오류 실패와 재시도 초과 건너뜀이 모두 포함될 수 있음
                log.warn("{}개 게시글 카운터 DB 업데이트 실패 또는 건너뜀", failed.size)
                // 필요시 실패한 ID 목록 로깅: log.warn("실패/건너뛴 boardId 목록: {}", failed)
            }
        } catch (e: Exception) {
            log.error("Pending Set 제거 중 예외 발생", e)
            // 이 예외는 다음 배치에 영향을 줄 수 있으므로 심각하게 처리 필요 (예: 알림)
        }
    }

    // DB 업데이트 실패 시 최대 재시도 횟수 확인
    private fun shouldSkipProcessing(boardId: Long): Boolean {
        val failCount = processingFailureCache.getOrDefault(boardId, 0)
        return failCount >= maxRetryCount
    }

    // DB 업데이트 실패 카운트 증가
    private fun incrementFailCount(boardId: Long) {
        processingFailureCache[boardId] = processingFailureCache.getOrDefault(boardId, 0) + 1
    }

    // DB 업데이트 결과 컨테이너
    private data class ProcessingResults(
        val successful: List<String>,
        val failed: List<String>,
    )

    // ========================================
    // 6. 헬퍼 메소드 - RDB 조회
    // ========================================

    // DB에서 조회수 조회
    private fun fetchDbViewCount(boardId: Long): Int =
        try {
            // 실제 BoardRepository 메소드 호출 필요
            boardRepository.findViewCountById(boardId) ?: 0
        } catch (e: Exception) {
            log.error("DB 조회수 조회 실패: boardId={}", boardId, e)
            0 // DB 조회 실패 시 0으로 간주 (또는 예외 발생)
        }

    // DB에서 좋아요 수 조회
    private fun fetchDbLikeCount(boardId: Long): Int =
        try {
            // 실제 BoardRepository 메소드 호출 필요
            boardRepository.findLikeCountById(boardId) ?: 0
        } catch (e: Exception) {
            log.error("DB 좋아요 수 조회 실패: boardId={}", boardId, e)
            0 // DB 조회 실패 시 0으로 간주 (또는 예외 발생)
        }
}
