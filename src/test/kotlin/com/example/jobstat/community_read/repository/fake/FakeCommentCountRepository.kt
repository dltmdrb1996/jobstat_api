package com.example.jobstat.community_read.repository.fake

import com.example.jobstat.community_read.repository.CommentCountRepository
import com.example.jobstat.community_read.repository.impl.RedisCommentCountRepository
import org.springframework.data.redis.connection.StringRedisConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

internal class FakeCommentCountRepository : CommentCountRepository {
    private val store = ConcurrentHashMap<String, AtomicLong>()
    private val totalCountKey = RedisCommentCountRepository.getTotalCommentCountKey()

    override fun getCommentCountByBoardId(boardId: Long): Long = store[RedisCommentCountRepository.getBoardCommentCountKey(boardId)]?.get() ?: 0L

    override fun getTotalCount(): Long = store[totalCountKey]?.get() ?: 0L

    override fun applyBoardCommentCountInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        delta: Long,
    ) {
        val key = RedisCommentCountRepository.getBoardCommentCountKey(boardId)
        store.computeIfAbsent(key) { AtomicLong(0L) }.updateAndGet { max(0L, it + delta) }
    }

    override fun applyTotalCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    ) {
        store.computeIfAbsent(totalCountKey) { AtomicLong(0L) }.updateAndGet { max(0L, it + delta) }
    }

    fun setCount(
        key: String,
        value: Long,
    ) {
        store[key] = AtomicLong(value)
    }

    fun clear() {
        store.clear()
    }
}
