package com.wildrew.jobstat.community.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript

@Suppress("UNCHECKED_CAST")
@Configuration
class LuaScriptConfig {
    @Bean
    fun atomicLikeScript(): RedisScript<List<Long>?> {
        val script = DefaultRedisScript<List<Long>?>()
        script.setScriptText(
            """
            local userKey = KEYS[1]
            local countKey = KEYS[2]
            local pendingKey = KEYS[3]
            local userId = ARGV[1]
            local boardIdStr = ARGV[2]
            local expireSeconds = ARGV[3]
            local added = redis.call('SADD', userKey, userId)
            if added == 0 then return {0, -1} end
            local newCount = redis.call('INCR', countKey)
            redis.call('SADD', pendingKey, boardIdStr)
            if tonumber(expireSeconds) > 0 then
                if redis.call('TTL', userKey) == -1 then
                    redis.call('EXPIRE', userKey, expireSeconds)
                end
            end
            return {1, newCount}
            """.trimIndent(),
        )
        script.setResultType(List::class.java as Class<List<Long>?>)
        return script
    }

    @Bean
    fun atomicUnlikeScript(): RedisScript<List<Long>?> {
        val script = DefaultRedisScript<List<Long>?>()
        script.setScriptText(
            """
            local userKey = KEYS[1]
            local countKey = KEYS[2]
            local pendingKey = KEYS[3]
            local userId = ARGV[1]
            local boardIdStr = ARGV[2]
            local removed = redis.call('SREM', userKey, userId)
            if removed == 0 then return {0, -1} end
            local newCount = redis.call('DECR', countKey)
            redis.call('SADD', pendingKey, boardIdStr)
            return {1, newCount}
            """.trimIndent(),
        )
        script.resultType = List::class.java as Class<List<Long>?>
        return script
    }

    @Bean
    fun getAndDeleteScript(): RedisScript<String> {
        val script = DefaultRedisScript<String>()
        script.setScriptText(
            """
            local value = redis.call('GET', KEYS[1])
            if value then redis.call('DEL', KEYS[1]) end
            return value
            """.trimIndent(),
        )
        script.setResultType(String::class.java)
        return script
    }

    @Bean
    fun atomicIncrementAndAddPendingScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText(
            """
            local countKey = KEYS[1]
            local pendingKey = KEYS[2]
            local boardIdStr = ARGV[1]
            
            local newCount = redis.call('INCR', countKey) 
            
            redis.call('SADD', pendingKey, boardIdStr) 
            
            return newCount 
            """.trimIndent(),
        )
        // 스크립트 결과 타입을 Long으로 설정
        script.setResultType(Long::class.java)
        return script
    }

    @Bean
    fun getCountersAndLikedStatusScript(): RedisScript<List<Any>> {
        val script = DefaultRedisScript<List<Any>>()
        script.setScriptText(
            """
            local viewKey = KEYS[1]
            local likeKey = KEYS[2]
            local userLikeKey = KEYS[3]
            local userId = ARGV[1]

            local viewCount = redis.call('GET', viewKey) or 0 
            local likeCount = redis.call('GET', likeKey) or 0

            local likedStatus = 0 
            if userId and #userId > 0 then
                if redis.call('EXISTS', userLikeKey) == 1 then
                     likedStatus = redis.call('SISMEMBER', userLikeKey, userId)
                end
            end
            
            return {viewCount, likeCount, likedStatus} 
            """.trimIndent(),
        )
        script.setResultType(List::class.java as Class<List<Any>>)
        return script
    }
}
