package com.example.jobstat.community.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript

@Suppress("UNCHECKED_CAST")
@Configuration
class LuaScriptConfig {

    // Like 스크립트 Bean 정의
    @Bean
    fun atomicLikeScript(): RedisScript<List<Long>?> { // 반환 타입을 List<Long>으로 지정
        val script = DefaultRedisScript<List<Long>?>()
        // 방법 1: 파일 로드
        // script.setLocation(ClassPathResource("lua/atomic_like.lua"))
        // 방법 2: 스크립트 텍스트 직접 설정
        script.setScriptText("""
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
        """.trimIndent())
        // 중요: 결과 타입을 명시해야 Spring이 올바르게 변환 시도
        // Redis는 List<Long>을 반환하므로 List::class.java 사용 가능 (내부 요소 타입은 직접 캐스팅 필요할 수 있음)
         script.setResultType(List::class.java as Class<List<Long>?>) // 타입 캐스팅 주의
        return script
    }

    // Unlike 스크립트 Bean 정의
    @Bean
    fun atomicUnlikeScript(): RedisScript<List<Long>?> { // 반환 타입을 List<Long>으로 지정
        val script = DefaultRedisScript<List<Long>?>()
        script.setScriptText("""
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
        """.trimIndent())
        script.resultType = List::class.java as Class<List<Long>?>
        return script
    }

    // 다른 스크립트들도 필요하다면 Bean으로 정의...
    // 예: GetAndDelete 스크립트
    @Bean
    fun getAndDeleteScript(): RedisScript<String> {
         val script = DefaultRedisScript<String>()
         script.setScriptText("""
             local value = redis.call('GET', KEYS[1])
             if value then redis.call('DEL', KEYS[1]) end
             return value
         """.trimIndent())
         script.setResultType(String::class.java)
         return script
     }
}