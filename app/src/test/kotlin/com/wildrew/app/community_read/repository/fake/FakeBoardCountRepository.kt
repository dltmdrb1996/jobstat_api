package com.wildrew.app.community_read.repository.fake

import com.wildrew.app.community_read.repository.BoardCountRepository
import com.wildrew.app.community_read.repository.impl.RedisBoardCountRepository
import org.springframework.data.redis.connection.StringRedisConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FakeBoardCountRepository : BoardCountRepository {
    private val store = ConcurrentHashMap<String, AtomicLong>()
    private val totalCountKey = RedisBoardCountRepository.BOARD_TOTAL_COUNT_KEY

    override fun getTotalCount(): Long = store[totalCountKey]?.get() ?: 0L

    override fun applyCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    ) {
        store.computeIfAbsent(totalCountKey) { AtomicLong(0L) }.addAndGet(delta)
    }

    fun setCount(
        key: String,
        value: Long,
    ) {
        store[key] = AtomicLong(value)
    }

    fun getRawValue(key: String): String? = store[key]?.toString()

    fun clear() {
        store.clear()
    }
}
