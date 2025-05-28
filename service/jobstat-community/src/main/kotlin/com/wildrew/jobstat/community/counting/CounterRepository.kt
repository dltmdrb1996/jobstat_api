package com.wildrew.jobstat.community.counting

interface CounterRepository {
    fun deleteBoardCounters(boardId: Long)

    fun atomicIncrementViewCountAndAddPending(boardId: Long): Long

    fun atomicLikeOperation(
        boardId: Long,
        userId: String,
    ): Int

    fun atomicUnlikeOperation(
        boardId: Long,
        userId: String,
    ): Int

    fun getSingleBoardCountersFromRedis(
        boardId: Long,
        userId: String?,
    ): RedisCounterRepository.RedisBoardCounters

    fun getBulkBoardCounters(
        boardIds: List<Long>,
        userId: String?,
    ): Map<Long, RedisCounterRepository.RedisBoardCounters>

    fun getAndDeleteCount(key: String): Int

    fun getAndDeleteCountsPipelined(keys: List<String>): List<Int?>

    fun removePendingBoardIds(boardIds: List<String>)

    fun fetchPendingBoardIds(): Set<String>

    fun popPendingBoardIdsAtomically(count: Long): List<String> // 또는 List<String>

    fun addBoardIdsToPending(boardIds: Collection<String>)

    fun rollbackIncrements(incrementsToRollback: Map<Long, Pair<Int, Int>>)
}
