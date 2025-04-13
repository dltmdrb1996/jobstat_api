package com.example.jobstat.community_read.utils.config // 또는 com.example.jobstat.community_read.config 등 적절한 위치

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript

@Configuration
class ReadSideLuaScriptConfig {

    // RedisBoardCountRepository 용 스크립트 (INCRBY 사용)
    @Bean
    fun applyDeltaScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: countKey, ARGV[1]: delta
            return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
        """.trimIndent())
        script.setResultType(Long::class.java) // 결과는 Long
        return script
    }

    // RedisCommentCountRepository 용 스크립트 (음수 방지 포함 - 원자적 GET/SET)
    @Bean
    fun applyDeltaNegativeCheckScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: countKey, ARGV[1]: delta
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local delta = tonumber(ARGV[1])
            local new = current + delta
            if new < 0 then new = 0 end
            redis.call('SET', KEYS[1], new)
            return new
        """.trimIndent())
        script.setResultType(Long::class.java) // 결과는 Long
        return script
    }

    // Board/Comment ID 목록 커서 기반 페이징 스크립트
    @Bean
    fun cursorPaginationScript(): RedisScript<List<*>> { // 반환 타입은 List<String>이지만, 제네릭 문제로 List<*> 사용 후 캐스팅
        val script = DefaultRedisScript<List<*>>() // 또는 List::class.java
        script.setScriptText("""
            local rank = redis.call('ZREVRANK', KEYS[1], ARGV[1])
            if not rank then return {} end
            return redis.call('ZREVRANGE', KEYS[1], rank + 1, rank + tonumber(ARGV[2]))
        """.trimIndent())
        script.setResultType(List::class.java) // Redis는 List를 반환
        return script
    }

    // 댓글 추가 (Score 비교) 및 Trim 스크립트 (RedisCommentIdListRepository.add)
    @Bean
    fun addCommentIfScoreDiffScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>() // ZADD 결과(추가된 수) 반환
        script.setScriptText("""
            -- KEYS[1]: commentListKey, ARGV[1]: score, ARGV[2]: member, ARGV[3]: limit
            local currentScore = redis.call('ZSCORE', KEYS[1], ARGV[2])
            local newScore = tonumber(ARGV[1])
            local added = 0
            if not currentScore or tonumber(currentScore) ~= newScore then
                added = redis.call('ZADD', KEYS[1], newScore, ARGV[2])
            end
            local size = redis.call('ZCARD', KEYS[1])
            local limit = tonumber(ARGV[3])
            if size > limit then
                 redis.call('ZREMRANGEBYRANK', KEYS[1], 0, size - limit - 1)
            end
            return added
        """.trimIndent())
        script.setResultType(Long::class.java)
        return script
    }

    // 댓글 Score 기반 무한 스크롤 스크립트 (RedisCommentIdListRepository.readAllByBoardInfiniteScroll)
     @Bean
     fun readCommentsByScoreScript(): RedisScript<List<*>> {
         val script = DefaultRedisScript<List<*>>()
         script.setScriptText("""
            -- KEYS[1]: commentListKey, ARGV[1]: lastCommentIdString, ARGV[2]: limit, ARGV[3]: epsilon
            local lastScore = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not lastScore then return {} end
            local maxScore = '(' .. (tonumber(lastScore)) -- Use exclusive range by prefixing with '('
            -- ZREVRANGEBYSCORE max min ; use '(' for exclusive max score
            return redis.call('ZREVRANGEBYSCORE', KEYS[1], maxScore, '-inf', 'LIMIT', 0, tonumber(ARGV[2]))
         """.trimIndent())
         script.setResultType(List::class.java)
         return script
     }

    // Board Add & Trim 스크립트 (선택적 개선 - 필요 시 추가)
    // @Bean fun addAndTrimBoardScript(): RedisScript<Long> { ... }
}