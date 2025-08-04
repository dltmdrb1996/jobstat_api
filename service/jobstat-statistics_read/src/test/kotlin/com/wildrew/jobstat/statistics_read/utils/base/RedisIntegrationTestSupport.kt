package com.wildrew.jobstat.statistics_read.utils.base

import com.wildrew.jobstat.statistics_read.utils.config.BaseIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate

abstract class RedisIntegrationTestSupport : BaseIntegrationTest() {
    @Autowired
    protected lateinit var redisTemplate: StringRedisTemplate

    fun flushAll() {
        redisTemplate.connectionFactory
            ?.connection
            ?.serverCommands()
            ?.flushAll()
    }

    fun zAdd(
        key: String,
        score: Double,
        member: String,
    ) {
        redisTemplate.opsForZSet().add(key, member, score)
    }

    fun zAddPadded(
        key: String,
        score: Double,
        memberId: Long,
    ) {
        redisTemplate.opsForZSet().add(key, "%019d".format(memberId), score)
    }

    fun getPaddedScore(
        key: String,
        memberId: Long,
    ): Double? = redisTemplate.opsForZSet().score(key, "%019d".format(memberId))

    fun getScore(
        key: String,
        memberId: Long,
    ): Double? = redisTemplate.opsForZSet().score(key, memberId.toString())
}
