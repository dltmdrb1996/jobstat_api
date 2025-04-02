package com.example.jobstat.community.counting

import com.example.jobstat.community.CommunityEventPublisher
import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.RedisOperationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
internal class CounterService(
    private val counterRepository: CounterRepository,
    private val boardRepository: BoardRepository,
    private val counterBatchService: CounterBatchService,
    @Value("\${counter.max-retry-count:3}") private val maxRetryCount: Int = 3
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    private val processingFailureCache = ConcurrentHashMap<Long, Int>()

    fun getViewCount(boardId: Long, dbViewCount: Int?): Int {
        val actualDbCount = dbViewCount ?: (boardRepository.findViewCountById(boardId) ?: 0)
        return (counterRepository.getViewCount(boardId) + actualDbCount)
    }

    fun getBulkViewCounts(boardIdsAndViews: List<Pair<Long, Int>>): List<Int> {
        if (boardIdsAndViews.isEmpty()) return emptyList()
        val redisViewCountMap = counterRepository.getBulkViewCounts(
            boardIdsAndViews.map { it.first }
        )
        return boardIdsAndViews.map { (boardId, dbCount) ->
            dbCount + (redisViewCountMap[boardId] ?: 0)
        }
    }
    /**
     * 조회수 증가
     */
    fun incrementViewCount(boardId: Long, dbViewCount: Int? = null): Int {
        val redisIncrement = counterRepository.incrementViewCount(boardId)
        counterRepository.addPendingBoardId(boardId)
        processingFailureCache.remove(boardId)
        val actualDbCount = dbViewCount ?: (boardRepository.findViewCountById(boardId) ?: 0)
        return (redisIncrement + actualDbCount).toInt()
    }


    fun getLikeCount(boardId: Long, dbLikeCount: Int?): Int {
        val actualDbCount = dbLikeCount ?: (boardRepository.findLikeCountById(boardId) ?: 0)
        return (counterRepository.getLikeCount(boardId) + actualDbCount).toInt()
    }

    fun getBulkLikeCounts(boardIdsAndLikes: List<Pair<Long, Int>>): List<Int> {
        if (boardIdsAndLikes.isEmpty()) return emptyList()
        val redisLikeCountMap = counterRepository.getBulkLikeCounts(
            boardIdsAndLikes.map { it.first }
        )
        return boardIdsAndLikes.map { (boardId, dbCount) ->
            dbCount + (redisLikeCountMap[boardId] ?: 0)
        }
    }

    /**
     * 좋아요 증가
     * - 같은 날 무한 취소/재시도 허용, 동시에 2번 불가
     * - likeUsers:{boardId} TTL 자정
     */
    fun incrementLikeCount(boardId: Long, userId: String, dbLikeCount: Int? = null): Int {
        val actualDbCount = dbLikeCount ?: (boardRepository.findLikeCountById(boardId) ?: 0)
        val redisCount = counterRepository.atomicLikeOperation(boardId, userId)
        return (actualDbCount + redisCount).toInt()
    }

    /**
     * 좋아요 취소
     */
    fun decrementLikeCount(boardId: Long, userId: String, dbLikeCount: Int? = null): Int {
        val actualDbCount = dbLikeCount ?: (boardRepository.findLikeCountById(boardId) ?: 0)
        val redisCount = counterRepository.atomicUnlikeOperation(boardId, userId)
        return (actualDbCount + redisCount).toInt()
    }

    fun hasUserLiked(boardId: Long, userId: String): Boolean {
        return counterRepository.hasUserLiked(boardId, userId)
    }

    /**
     * 특정 사용자가 여러 게시글에 좋아요를 했는지 일괄 조회
     * 입력된 순서대로 결과값을 반환합니다.
     */
    fun getBulkUserLikedStatus(boardIds: List<Long>, userId: String): List<Boolean> {
        if (boardIds.isEmpty()) return emptyList()
        val userLikedMap = counterRepository.getBulkUserLikedStatus(boardIds, userId)
        return boardIds.map { boardId ->
            userLikedMap[boardId] ?: false
        }
    }

    /**
     * 게시판 ID, 조회수, 좋아요수, 사용자 좋아요 상태를 한 번에 조회
     * [boardId, viewCount, likeCount, userLiked] 형태의 튜플 리스트 반환
     */
    fun getBulkBoardCounters(
        boardIdsWithCounts: List<Triple<Long, Int, Int>>,
        userId: String?
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
            val viewCount = dbViewCount + (redisViewCountMap[boardId] ?: 0)
            val likeCount = dbLikeCount + (redisLikeCountMap[boardId] ?: 0)
            val userLiked = userLikedMap[boardId] ?: false

            BoardCounters(
                boardId = boardId,
                viewCount = viewCount,
                likeCount = likeCount,
                userLiked = userLiked
            )
        }
    }

    // ===== 배치 =====
    @Scheduled(fixedDelayString = "\${counter.flush-interval-milliseconds:300000}")
    fun flushCountersToDatabase() {
        log.info("Redis 카운터 DB 동기화 시작")
        try {
            val pendingBoardIds = counterRepository.fetchPendingBoardIds()
            if (pendingBoardIds.isEmpty()) {
                log.info("업데이트할 게시글 없음")
                return
            }
            log.info("총 {}개 게시글 업데이트 처리 시작", pendingBoardIds.size)

            val (successful, failed) = processPendingBoards(pendingBoardIds)
            finalizeProcessingResults(successful, failed)

        } catch (e: Exception) {
            log.error("카운터 DB 동기화 전체 실패", e)
        }
    }

    private fun processPendingBoards(boardIds: Set<String>): ProcessingResults {
        val successful = mutableListOf<String>()
        val failed = mutableListOf<String>()

        boardIds.forEach { boardIdStr ->
            val boardId = boardIdStr.toLongOrNull() ?: run {
                log.error("boardId 변환 실패: $boardIdStr")
                failed.add(boardIdStr)
                return@forEach
            }

            if (shouldSkipProcessing(boardId)) {
                failed.add(boardIdStr)
                return@forEach
            }

            try {
                val processed = counterBatchService.processSingleBoardCounter(boardId)
                if (processed) {
                    successful.add(boardIdStr)
                    processingFailureCache.remove(boardId)
                } else {
                    failed.add(boardIdStr)
                    incrementFailCount(boardId)
                }
            } catch (e: Exception) {
                log.error("게시글 ID {} 카운터 처리 예외 발생", boardId, e)
                failed.add(boardIdStr)
                incrementFailCount(boardId)
            }
        }

        return ProcessingResults(successful, failed)
    }

    private fun finalizeProcessingResults(successful: List<String>, failed: List<String>) {
        if (successful.isNotEmpty()) {
            try {
                counterRepository.removePendingBoardIds(successful)
                log.info("{}개 게시글 카운터 업데이트 성공", successful.size)
            } catch (e: Exception) {
                log.error("성공한 게시글 제거 중 예외", e)
            }
        }
        if (failed.isNotEmpty()) {
            log.warn("{}개 게시글 카운터 업데이트 실패 -> 다음 배치에서 재시도", failed.size)
        }
    }

    private fun shouldSkipProcessing(boardId: Long): Boolean {
        val failCount = processingFailureCache[boardId] ?: 0
        if (failCount >= maxRetryCount) {
            log.warn("게시글 ID {} 최대 재시도({}) 초과 -> 건너뛰기", boardId, maxRetryCount)
            processingFailureCache.remove(boardId)
            return true
        }
        return false
    }

    private fun incrementFailCount(boardId: Long) {
        processingFailureCache[boardId] = (processingFailureCache[boardId] ?: 0) + 1
    }

    private data class ProcessingResults(
        val successful: List<String>,
        val failed: List<String>
    )
}