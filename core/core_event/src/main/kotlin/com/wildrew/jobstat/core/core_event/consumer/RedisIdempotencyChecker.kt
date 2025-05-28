package com.wildrew.jobstat.core.core_event.consumer

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class RedisIdempotencyChecker(
    private val stringRedisTemplate: StringRedisTemplate,
) : IdempotencyChecker {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val IDEMPOTENCY_KEY_PREFIX = "idempotency_key:"
    }

    override fun isAlreadyProcessed(key: Long): Boolean {
        val redisKey = IDEMPOTENCY_KEY_PREFIX + key
        return try {
            val exists = stringRedisTemplate.hasKey(redisKey)
            log.debug("멱등성 확인: key={}, redisKey={}, exists={}", key, redisKey, exists)
            exists
        } catch (e: Exception) {
            log.error("멱등성 확인 중 Redis 오류 발생: key={}", redisKey, e)
            false
        }
    }

    override fun markAsProcessed(
        key: Long,
        ttl: Duration,
    ) {
        val redisKey = IDEMPOTENCY_KEY_PREFIX + key
        try {
            stringRedisTemplate.opsForValue().set(redisKey, "processed", ttl)
        } catch (e: Exception) {
            log.error("처리 완료 마킹 중 Redis 오류 발생: key={}, ttl={}", redisKey, ttl, e)
            throw e
        }
    }
}
