package com.example.jobstat.core.event.outbox.relay

import com.example.jobstat.core.event.outbox.OutboxConstants
import com.example.jobstat.core.event.outbox.OutboxShardAssignment
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class OutboxCoordinator(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${spring.application.name}") private val applicationName: String
) {
    private val appId: String = UUID.randomUUID().toString()
    private val pingIntervalSeconds = 3
    private val pingFailureThreshold = 3

    fun assignShards(): OutboxShardAssignment =
        OutboxShardAssignment.of(appId, findAppIds(), OutboxConstants.SHARD_COUNT.toLong())

    private fun findAppIds(): List<String> =
        redisTemplate.opsForZSet().reverseRange(generateKey(), 0, -1)
            ?.sorted() ?: emptyList()

    @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
    fun ping() {
        redisTemplate.executePipelined(RedisCallback<Any> { action ->
            val conn = action as StringRedisConnection
            val key = generateKey()
            conn.zAdd(key, Instant.now().toEpochMilli().toDouble(), appId)
            conn.zRemRangeByScore(
                key,
                Double.NEGATIVE_INFINITY,
                Instant.now()
                    .minusSeconds((pingIntervalSeconds * pingFailureThreshold).toLong())
                    .toEpochMilli().toDouble()
            )
            null
        })
    }

    @PreDestroy
    fun leave() {
        redisTemplate.opsForZSet().remove(generateKey(), appId)
    }

    private fun generateKey(): String = "outbox-coordinator::app-list::$applicationName"
}