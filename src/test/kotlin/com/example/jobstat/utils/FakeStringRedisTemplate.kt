package com.example.jobstat.utils

import org.springframework.data.redis.connection.BitFieldSubCommands
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.TimeUnit

class FakeStringRedisTemplate : StringRedisTemplate() {
    private val store = mutableMapOf<String, String>()
    private val expirations = mutableMapOf<String, Long>()
    private val ops = FakeValueOperations()

    override fun opsForValue(): ValueOperations<String, String> = ops

    inner class FakeValueOperations : ValueOperations<String, String> {
        override fun get(key: Any): String? {
            val expiration = expirations[key]
            if (expiration != null && System.currentTimeMillis() > expiration) {
                store.remove(key)
                expirations.remove(key)
                return null
            }
            return store[key]
        }

        override fun set(
            key: String,
            value: String,
            timeout: Long,
            unit: TimeUnit,
        ) {
            store[key] = value
            expirations[key] = System.currentTimeMillis() + unit.toMillis(timeout)
        }

        override fun set(
            key: String,
            value: String,
        ) {
            store[key] = value
        }

        // ValueOperations의 나머지 메서드들
        override fun setIfAbsent(
            key: String,
            value: String,
        ): Boolean {
            if (store.containsKey(key)) return false
            store[key] = value
            return true
        }

        override fun increment(
            key: String,
            delta: Long,
        ): Long {
            val current = store[key]?.toLongOrNull() ?: 0
            val newValue = current + delta
            store[key] = newValue.toString()
            return newValue
        }

        override fun set(
            key: String,
            value: String,
            offset: Long,
        ) {
            TODO("Not yet implemented")
        }

        override fun setIfAbsent(
            key: String,
            value: String,
            timeout: Long,
            unit: TimeUnit,
        ): Boolean? {
            TODO("Not yet implemented")
        }

        override fun setIfPresent(
            key: String,
            value: String,
        ): Boolean? {
            TODO("Not yet implemented")
        }

        override fun setIfPresent(
            key: String,
            value: String,
            timeout: Long,
            unit: TimeUnit,
        ): Boolean? {
            TODO("Not yet implemented")
        }

        override fun multiSet(map: MutableMap<out String, out String>) {
            TODO("Not yet implemented")
        }

        override fun multiSetIfAbsent(map: MutableMap<out String, out String>): Boolean? {
            TODO("Not yet implemented")
        }

        override fun getOperations(): RedisOperations<String, String> {
            TODO("Not yet implemented")
        }

        override fun bitField(
            key: String,
            subCommands: BitFieldSubCommands,
        ): MutableList<Long>? {
            TODO("Not yet implemented")
        }

        override fun getBit(
            key: String,
            offset: Long,
        ): Boolean? {
            TODO("Not yet implemented")
        }

        override fun setBit(
            key: String,
            offset: Long,
            value: Boolean,
        ): Boolean? {
            TODO("Not yet implemented")
        }

        override fun size(key: String): Long? {
            TODO("Not yet implemented")
        }

        override fun append(
            key: String,
            value: String,
        ): Int? {
            TODO("Not yet implemented")
        }

        override fun decrement(
            key: String,
            delta: Long,
        ): Long? {
            TODO("Not yet implemented")
        }

        override fun decrement(key: String): Long? {
            TODO("Not yet implemented")
        }

        override fun multiGet(keys: MutableCollection<String>): MutableList<String>? {
            TODO("Not yet implemented")
        }

        override fun getAndSet(
            key: String,
            value: String,
        ): String? {
            TODO("Not yet implemented")
        }

        override fun getAndPersist(key: String): String? {
            TODO("Not yet implemented")
        }

        override fun getAndExpire(
            key: String,
            timeout: Duration,
        ): String? {
            TODO("Not yet implemented")
        }

        override fun getAndExpire(
            key: String,
            timeout: Long,
            unit: TimeUnit,
        ): String? {
            TODO("Not yet implemented")
        }

        override fun getAndDelete(key: String): String? {
            TODO("Not yet implemented")
        }

        override fun increment(
            key: String,
            delta: Double,
        ): Double? {
            TODO("Not yet implemented")
        }

        override fun increment(key: String): Long? {
            TODO("Not yet implemented")
        }

        override fun get(
            key: String,
            start: Long,
            end: Long,
        ): String? {
            TODO("Not yet implemented")
        }
    }

    override fun delete(key: String): Boolean {
        val removed = store.remove(key) != null
        expirations.remove(key)
        return removed
    }

    override fun keys(pattern: String): Set<String> {
        val regex = pattern.replace("*", ".*").toRegex()
        return store.keys.filter { it.matches(regex) }.toSet()
    }

    override fun getExpire(key: String): Long {
        val expiration = expirations[key] ?: return -2
        val remaining = expiration - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 else -1
    }

    fun clear() {
        store.clear()
        expirations.clear()
    }
}
