package com.example.jobstat.community.counting

import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.core.constants.RedisKeyConstants // RedisKeyConstants 사용 위해 추가
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * 실시간 카운터 관리 및 배치 처리를 담당하는 서비스.
 * - 실시간 요청 처리 (좋아요, 조회수 증가 등)
 * - 주기적인 배치 실행 (Redis 카운터를 읽어 DB에 반영)
 */
@Service
internal class CounterService(
    private val counterRepository: CounterRepository,
    private val boardRepository: BoardRepository, // DB 값 직접 조회 필요 시 사용
    private val counterBatchService: CounterBatchService, // DB 업데이트 위임
    @Value("\${counter.max-retry-count:3}") private val maxRetryCount: Int = 3
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    // DB 업데이트 실패 시 재시도 횟수 추적용 캐시
    private val processingFailureCache = ConcurrentHashMap<Long, Int>()


    /**
     * 게시글 삭제 시 관련된 Redis 카운터 데이터를 정리합니다.
     * 이 메소드는 내부적으로 CounterRepository의 삭제 로직을 호출합니다.
     * @param boardId 정리할 게시글 ID
     * @throws Exception Redis 작업 중 발생한 예외가 전파될 수 있습니다.
     */
    fun cleanupBoardCounters(boardId: Long) {
        log.info("Requesting cleanup for boardId {} counters.", boardId)
        // Repository의 메소드를 호출하여 실제 작업을 위임
        // Repository 레벨에서 발생하는 예외는 여기서 처리하지 않고 호출 측(UseCase)으로 전파.
        counterRepository.deleteBoardCounters(boardId)
        // 성공 로그는 Repository에서 이미 남기므로 여기서는 생략하거나 시작 로그만 남김.
    }

    // 특정 게시글 조회수 조회 (Redis + DB)
    fun getViewCount(boardId: Long, dbViewCount: Int? = null): Int {
        val redisCount = counterRepository.getViewCount(boardId)
        val actualDbCount = dbViewCount ?: fetchDbViewCount(boardId) // DB 조회 분리
        return (redisCount + actualDbCount)
    }

    // 여러 게시글 조회수 일괄 조회 (Redis + DB)
    fun getBulkViewCounts(boardIdsAndViews: List<Pair<Long, Int>>): List<Int> {
        if (boardIdsAndViews.isEmpty()) return emptyList()
        val boardIds = boardIdsAndViews.map { it.first }
        val redisViewCountMap = counterRepository.getBulkViewCounts(boardIds)
        return boardIdsAndViews.map { (boardId, dbCount) ->
            dbCount + (redisViewCountMap[boardId] ?: 0)
        }
    }

    // 특정 게시글 좋아요 수 조회 (Redis + DB)
    fun getLikeCount(boardId: Long, dbLikeCount: Int? = null): Int {
        val redisCount = counterRepository.getLikeCount(boardId)
        val actualDbCount = dbLikeCount ?: fetchDbLikeCount(boardId) // DB 조회 분리
        return (redisCount + actualDbCount)
    }

    // 여러 게시글 좋아요 수 일괄 조회 (Redis + DB)
    fun getBulkLikeCounts(boardIdsAndLikes: List<Pair<Long, Int>>): List<Int> {
        if (boardIdsAndLikes.isEmpty()) return emptyList()
        val boardIds = boardIdsAndLikes.map { it.first }
        val redisLikeCountMap = counterRepository.getBulkLikeCounts(boardIds)
        return boardIdsAndLikes.map { (boardId, dbCount) ->
            dbCount + (redisLikeCountMap[boardId] ?: 0)
        }
    }

    // 특정 사용자의 좋아요 여부 확인
    fun hasUserLiked(boardId: Long, userId: String): Boolean {
        // likeUsers 세트는 자정 만료. 만료 후 첫 좋아요 시 다시 생성됨.
        return counterRepository.hasUserLiked(boardId, userId)
    }

    // 특정 사용자의 여러 게시글 좋아요 여부 일괄 확인
    fun getBulkUserLikedStatus(boardIds: List<Long>, userId: String): List<Boolean> {
        if (boardIds.isEmpty()) return emptyList()
        val userLikedMap = counterRepository.getBulkUserLikedStatus(boardIds, userId)
        // 결과 순서 보장
        return boardIds.map { boardId ->
            userLikedMap[boardId] ?: false
        }
    }

    // 여러 게시글의 카운터 정보 일괄 조회 (DB + Redis)
    // (BoardCounters data class 정의 필요)
    data class BoardCounters(val boardId: Long, val viewCount: Int, val likeCount: Int, val userLiked: Boolean)
    fun getBulkBoardCounters(
        boardIdsWithCounts: List<Triple<Long, Int, Int>>, // (boardId, dbView, dbLike)
        userId: String? = null // userId 제공 시 좋아요 여부 포함
    ): List<BoardCounters> {
        if (boardIdsWithCounts.isEmpty()) return emptyList()

        val boardIds = boardIdsWithCounts.map { it.first }

        // Redis에서 모든 카운터 정보 한 번에 조회
        val redisViewCountMap = counterRepository.getBulkViewCounts(boardIds)
        val redisLikeCountMap = counterRepository.getBulkLikeCounts(boardIds)
        val userLikedMap = userId?.let {
            counterRepository.getBulkUserLikedStatus(boardIds, it)
        } ?: emptyMap()

        // 결과 조합
        return boardIdsWithCounts.map { (boardId, dbViewCount, dbLikeCount) ->
            val finalViewCount = dbViewCount + (redisViewCountMap[boardId] ?: 0)
            val finalLikeCount = dbLikeCount + (redisLikeCountMap[boardId] ?: 0)
            val userLiked = userLikedMap[boardId] ?: false

            BoardCounters(
                boardId = boardId,
                viewCount = finalViewCount,
                likeCount = finalLikeCount,
                userLiked = userLiked
            )
        }
    }


    // ========================================
    // 실시간 카운터 변경 (Write Operations)
    // ========================================

    // 조회수 증가 (Redis & Pending Set 추가)
    fun incrementViewCount(boardId: Long) {
        try {
            counterRepository.incrementViewCount(boardId)
            counterRepository.addPendingBoardId(boardId) // 배치 처리 대상 추가
            processingFailureCache.remove(boardId) // 이전에 실패했었다면, 새 활동 발생 시 재시도 카운트 리셋
        } catch (e: Exception) {
            log.error("조회수 증가 실패: boardId={}", boardId, e)
            // 실패 시 예외 전파 또는 처리 필요
            throw e // 예시: 예외 다시 던지기
        }
    }

    // (오버로딩된 incrementViewCount(boardId, dbViewCount)는 실시간 카운트 반환이 필요하면 유지, 아니면 제거 가능)

    // 좋아요 증가 (Redis & Pending Set 추가)
    // 반환값: 현재 Redis 카운트 값 (DB와 합산 필요 시 사용)

    fun incrementLikeCount(boardId: Long, userId: String, dbLikeCount: Int? = null): Int {
        val actualDbCount = dbLikeCount ?: (boardRepository.findLikeCountById(boardId) ?: 0)
        val redisCount = counterRepository.atomicLikeOperation(boardId, userId)
        processingFailureCache.remove(boardId)
        return (actualDbCount + redisCount)
    }

    fun decrementLikeCount(boardId: Long, userId: String, dbLikeCount: Int? = null): Int {
        val actualDbCount = dbLikeCount ?: (boardRepository.findLikeCountById(boardId) ?: 0)
        val redisCount = counterRepository.atomicUnlikeOperation(boardId, userId)
        processingFailureCache.remove(boardId)
        return (actualDbCount + redisCount)
    }

    // ========================================
    // 배치 처리 (Scheduled Task)
    // ========================================

    @Scheduled(cron = "0 */5 * * * *") // 매 5분마다 0초에 실행 (예: 00:00:00, 00:05:00, 00:10:00 ...)
    fun flushCountersToDatabase() {
        log.info("Redis 카운터 DB 동기화 시작")
        val startTime = System.currentTimeMillis()
        var processedCount = 0
        try {
            // 1. 처리 대상 ID 목록 조회
            val pendingBoardIdStrs = counterRepository.fetchPendingBoardIds()
            if (pendingBoardIdStrs.isEmpty()) {
                log.info("업데이트할 게시글 없음")
                return
            }
            processedCount = pendingBoardIdStrs.size
            log.info("총 {}개 게시글 업데이트 처리 시작", processedCount)

            // 2. 유효한 Long ID 필터링
            val boardIds = pendingBoardIdStrs.mapNotNull { it.toLongOrNull() }
            val invalidIdStrs = pendingBoardIdStrs.filter { it.toLongOrNull() == null }

            if (invalidIdStrs.isNotEmpty()) {
                log.error("잘못된 형식의 boardId 발견 (Pending Set에서 제거): {}", invalidIdStrs)
            }
            if (boardIds.isEmpty()) {
                log.warn("유효한 boardId 없음")
                // 잘못된 ID만 있었던 경우, Pending Set에서 제거
                if (invalidIdStrs.isNotEmpty()) {
                    counterRepository.removePendingBoardIds(invalidIdStrs)
                }
                return
            }

            // 3. Redis 파이프라인으로 카운터 값 가져오기 & 키 삭제
            val viewCountKeys = boardIds.map { RedisKeyConstants.Counter.viewCountKey(it) }
            val likeCountKeys = boardIds.map { RedisKeyConstants.Counter.likeCountKey(it) }

            val viewCounts: List<Int?>
            val likeCounts: List<Int?>
            try {
                viewCounts = counterRepository.getAndDeleteCountsPipelined(viewCountKeys)
                likeCounts = counterRepository.getAndDeleteCountsPipelined(likeCountKeys)
                log.info("Redis 파이프라인 실행 완료 ({}개 boardId)", boardIds.size)
            } catch (e: Exception) {
                log.error("Redis 파이프라인 실행 중 오류 발생", e)
                // Redis 오류 시 배치를 중단하고 다음 기회에 재시도 (Pending ID는 남아있음)
                return
            }


            // 4. 결과 매핑 (boardId -> Pair(viewCount, likeCount))
            if (viewCounts.size != boardIds.size || likeCounts.size != boardIds.size) {
                log.error("파이프라인 결과 크기 불일치! 요청: {}, 응답: view={}, like={}",
                    boardIds.size, viewCounts.size, likeCounts.size)
                // 데이터 불일치 시 중단 또는 추가 처리 필요
                return
            }
            val boardIdToCounts = boardIds.indices.associate { index ->
                val boardId = boardIds[index]
                // Redis 키가 없었으면 null 반환됨 -> 0으로 처리
                val viewCount = viewCounts.getOrNull(index) ?: 0
                val likeCount = likeCounts.getOrNull(index) ?: 0
                boardId to Pair(viewCount, likeCount)
            }

            // 5. DB 업데이트 처리 (개별 트랜잭션 사용)
            val (successfulStrs, failedStrs) = processBoardUpdates(boardIdToCounts)

            // 6. 최종 결과 처리 (Pending Set 에서 처리 완료된 ID 제거)
            // 성공/실패 여부와 관계없이 처리 시도한 모든 ID 제거 + 잘못된 형식 ID 제거
            val allAttemptedIds = pendingBoardIdStrs.toList() // 원본 Set 사용
            finalizeProcessingResults(successfulStrs, failedStrs, allAttemptedIds)

            val duration = System.currentTimeMillis() - startTime
            log.info("Redis 카운터 DB 동기화 완료 ({}개 처리 시도, {}ms 소요)", processedCount, duration)

        } catch (e: Exception) {
            // 예상치 못한 전체 배치 실패
            log.error("카운터 DB 동기화 전체 실패", e)
        }
    }

    // DB 업데이트 처리 로직 (파이프라인 결과 사용)
    private fun processBoardUpdates(boardIdToCounts: Map<Long, Pair<Int, Int>>): ProcessingResults {
        val successful = mutableListOf<String>()
        val failed = mutableListOf<String>()

        boardIdToCounts.forEach { (boardId, counts) ->
            val (viewCount, likeCount) = counts
            val boardIdStr = boardId.toString()

            if (shouldSkipProcessing(boardId)) { // 재시도 횟수 초과 확인
                failed.add(boardIdStr)
                log.warn("게시글 ID {} 최대 재시도 초과하여 건너뜀", boardId)
                return@forEach
            }

            try {
                // 수정된 CounterBatchService 호출 (Redis에서 읽어온 값 전달)
                val processed = counterBatchService.processSingleBoardCounter(boardId, viewCount, likeCount)
                if (processed) {
                    successful.add(boardIdStr)
                    processingFailureCache.remove(boardId) // 성공 시 실패 카운트 제거
                } else {
                    // processSingleBoardCounter 내부에서 에러 로깅됨
                    failed.add(boardIdStr)
                    incrementFailCount(boardId) // 실패 시 카운트 증가
                }
            } catch (e: Exception) {
                // CounterBatchService 호출 자체에서 발생한 예외 (네트워크 등) - 거의 발생 안 함 가정
                log.error("게시글 ID {} DB 처리 호출 중 예외 발생 (호출 단계)", boardId, e)
                failed.add(boardIdStr)
                incrementFailCount(boardId)
            }
        }
        return ProcessingResults(successful, failed)
    }

    // 배치 처리 결과 마무리 (Pending Set 정리)
    private fun finalizeProcessingResults(successful: List<String>, failed: List<String>, allProcessedIds: List<String>) {
        try {
            // 성공/실패 여부와 관계없이 처리 시도한 ID는 pending set에서 제거
            // Redis 키는 이미 파이프라인에서 삭제되었으므로, 여기서 실패해도 재처리 시 count는 0이 됨
            if (allProcessedIds.isNotEmpty()) {
                counterRepository.removePendingBoardIds(allProcessedIds)
                log.info("총 {}개 게시글 처리 시도 후 Pending Set에서 제거", allProcessedIds.size)
            }

            // 결과 로깅
            if (successful.isNotEmpty()) {
                log.info("{}개 게시글 카운터 DB 업데이트 성공", successful.size)
            }
            if (failed.isNotEmpty()) {
                log.warn("{}개 게시글 카운터 DB 업데이트 실패 또는 건너뜀", failed.size)
            }
        } catch (e: Exception) {
            log.error("Pending Set 제거 중 예외 발생", e)
            // 이 예외는 다음 배치에 영향 줄 수 있으므로 심각도 높게 관리 필요
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
        val failed: List<String>
    )

    // --- Helper methods for DB lookup ---
    private fun fetchDbViewCount(boardId: Long): Int {
        return try {
            boardRepository.findViewCountById(boardId) ?: 0 // BoardRepository에 메소드 필요
        } catch (e: Exception) {
            log.error("DB 조회수 조회 실패: boardId={}", boardId, e)
            0 // DB 조회 실패 시 0으로 간주
        }
    }

    private fun fetchDbLikeCount(boardId: Long): Int {
        return try {
            boardRepository.findLikeCountById(boardId) ?: 0 // BoardRepository에 메소드 필요
        } catch (e: Exception) {
            log.error("DB 좋아요 수 조회 실패: boardId={}", boardId, e)
            0 // DB 조회 실패 시 0으로 간주
        }
    }
}