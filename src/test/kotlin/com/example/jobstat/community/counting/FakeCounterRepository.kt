package com.example.jobstat.community.counting

import com.example.jobstat.community.counting.RedisCounterRepository.Companion.likeCountKey
import com.example.jobstat.community.counting.RedisCounterRepository.Companion.likeUsersKey
import com.example.jobstat.community.counting.RedisCounterRepository.Companion.viewCountKey
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

internal class FakeCounterRepository : CounterRepository {
    private val counts = ConcurrentHashMap<String, Long>()
    private val likeUsers = ConcurrentHashMap<String, MutableSet<String>>()
    private val pendingUpdates = ConcurrentHashMap.newKeySet<String>()

    fun getViewCount(boardId: Long): Long = counts[viewCountKey(boardId)] ?: 0L

    fun getLikeCount(boardId: Long): Long = counts[likeCountKey(boardId)] ?: 0L

    fun isUserLiked(
        boardId: Long,
        userId: String,
    ): Boolean = likeUsers[likeUsersKey(boardId)]?.contains(userId) ?: false

    override fun deleteBoardCounters(boardId: Long) {
        val boardIdStr = boardId.toString()
        counts.remove(viewCountKey(boardId))
        counts.remove(likeCountKey(boardId))
        likeUsers.remove(likeUsersKey(boardId))
        pendingUpdates.remove(boardIdStr)
    }

    override fun atomicIncrementViewCountAndAddPending(boardId: Long): Long {
        val key = viewCountKey(boardId)
        val newValue = counts.compute(key) { _, current -> (current ?: 0L) + 1L }!!
        pendingUpdates.add(boardId.toString())
        return newValue
    }

    override fun atomicLikeOperation(
        boardId: Long,
        userId: String,
    ): Int {
        val likeUsersKey = likeUsersKey(boardId)
        val likeCountKey = likeCountKey(boardId)
        val boardIdStr = boardId.toString()

        val userSet = likeUsers.computeIfAbsent(likeUsersKey) { ConcurrentHashMap.newKeySet() }

        if (userSet.contains(userId)) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_OPERATION, "이미 좋아요 상태입니다.")
        }

        userSet.add(userId)
        val newLikeCount = counts.compute(likeCountKey) { _, current -> (current ?: 0L) + 1L }!!
        pendingUpdates.add(boardIdStr)

        return newLikeCount.toInt()
    }

    override fun atomicUnlikeOperation(
        boardId: Long,
        userId: String,
    ): Int {
        val likeUsersKey = likeUsersKey(boardId)
        val likeCountKey = likeCountKey(boardId)
        val boardIdStr = boardId.toString()

        val userSet = likeUsers[likeUsersKey]

        if (userSet == null || !userSet.contains(userId)) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_OPERATION, "이미 좋아요 취소 상태입니다.")
        }

        userSet.remove(userId)
        val newLikeCount = counts.compute(likeCountKey) { _, current -> max(0L, (current ?: 1L) - 1L) }!!
        pendingUpdates.add(boardIdStr)

        return newLikeCount.toInt()
    }

    override fun getSingleBoardCountersFromRedis(
        boardId: Long,
        userId: String?,
    ): RedisCounterRepository.RedisBoardCounters {
        val viewCount = getViewCount(boardId).toInt()
        val likeCount = getLikeCount(boardId).toInt()
        val userLiked = if (userId != null) isUserLiked(boardId, userId) else false
        return RedisCounterRepository.RedisBoardCounters(viewCount, likeCount, userLiked)
    }

    override fun getBulkBoardCounters(
        boardIds: List<Long>,
        userId: String?,
    ): Map<Long, RedisCounterRepository.RedisBoardCounters> {
        if (boardIds.isEmpty()) return emptyMap()
        return boardIds.associateWith { boardId ->
            getSingleBoardCountersFromRedis(boardId, userId)
        }
    }

    override fun getAndDeleteCount(key: String): Int {
        val value = counts.remove(key) ?: 0L
        return value.toInt()
    }

    override fun getAndDeleteCountsPipelined(keys: List<String>): List<Int?> {
        if (keys.isEmpty()) return emptyList()
        return keys.map { key ->
            counts.remove(key)?.toInt()
        }
    }

    override fun removePendingBoardIds(boardIds: List<String>) {
        if (boardIds.isEmpty()) return
        boardIds.forEach { pendingUpdates.remove(it) }
    }

    override fun fetchPendingBoardIds(): Set<String> = pendingUpdates.toSet()

    fun clear() {
        counts.clear()
        likeUsers.clear()
        pendingUpdates.clear()
        println("--- FakeCounterRepository Cleared ---")
    }

    fun setCounts(
        boardId: Long,
        viewCount: Long,
        likeCount: Long,
    ) {
        counts[viewCountKey(boardId)] = viewCount
        counts[likeCountKey(boardId)] = likeCount
    }

    fun addUserLike(
        boardId: Long,
        userId: String,
    ) {
        val userSet = likeUsers.computeIfAbsent(likeUsersKey(boardId)) { ConcurrentHashMap.newKeySet() }
        userSet.add(userId)
    }

    fun addPendingUpdate(boardId: Long) {
        pendingUpdates.add(boardId.toString())
    }
}
