package com.example.jobstat.community_read.repository

import com.example.jobstat.core.constants.RedisKeyConstants
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisBoardCountRepository(
    private val redisTemplate: StringRedisTemplate
) : BoardCountRepository {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun createOrUpdate(categoryId: Long, count: Long) {
        val key = RedisKeyConstants.Counter.categoryCountKey(categoryId)
        try {
            val current = redisTemplate.opsForValue().get(key)?.toLongOrNull()
            if (current != null && current == count) {
                log.info("Board count for categoryId={} already up-to-date ({}).", categoryId, count)
                return
            }
            redisTemplate.opsForValue().set(key, count.toString())
        } catch (e: Exception) {
            log.error("게시글 수 저장 실패: categoryId={}, count={}", categoryId, count, e)
            throw e
        }
    }

    override fun read(categoryId: Long): Long? {
        val key = RedisKeyConstants.Counter.categoryCountKey(categoryId)
        try {
            return redisTemplate.opsForValue().get(key)?.toLong()
        } catch (e: Exception) {
            log.error("게시글 수 조회 실패: categoryId={}", categoryId, e)
            throw e
        }
    }
}
