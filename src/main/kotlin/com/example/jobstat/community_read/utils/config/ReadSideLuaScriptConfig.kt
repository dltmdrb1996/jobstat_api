package com.example.jobstat.community_read.utils.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript

@Configuration
class ReadSideLuaScriptConfig {
    companion object {
        const val SCRIPT_RESULT_SUCCESS: Long = 1L
        const val SCRIPT_RESULT_SKIPPED: Long = 0L // 예: 오래된 이벤트
        const val SCRIPT_RESULT_ERROR: Long = -1L // 스크립트 내부 로직 오류 등 (선택적)
    }

    @Bean
    fun applyDeltaScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText(
            """
            -- KEYS[1]: countKey, ARGV[1]: delta
            return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
            """.trimIndent(),
        )
        script.setResultType(Long::class.java) // 결과는 Long
        return script
    }

    @Bean
    fun applyDeltaNegativeCheckScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText(
            """
            -- KEYS[1]: countKey, ARGV[1]: delta
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local delta = tonumber(ARGV[1])
            local new = current + delta
            if new < 0 then new = 0 end
            redis.call('SET', KEYS[1], new)
            return new
            """.trimIndent(),
        )
        script.setResultType(Long::class.java) // 결과는 Long
        return script
    }

    @Bean
    fun cursorPaginationScript(): RedisScript<List<*>> { // 반환 타입은 List<String>이지만, 제네릭 문제로 List<*> 사용 후 캐스팅
        val script = DefaultRedisScript<List<*>>() // 또는 List::class.java
        script.setScriptText(
            """
            local rank = redis.call('ZREVRANK', KEYS[1], ARGV[1])
            if not rank then return {} end
            return redis.call('ZREVRANGE', KEYS[1], rank + 1, rank + tonumber(ARGV[2]))
            """.trimIndent(),
        )
        script.setResultType(List::class.java) // Redis는 List를 반환
        return script
    }

    @Bean
    fun addCommentIfScoreDiffScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>() // ZADD 결과(추가된 수) 반환
        script.setScriptText(
            """
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
            """.trimIndent(),
        )
        script.setResultType(Long::class.java)
        return script
    }

    @Bean
    fun readCommentsByScoreScript(): RedisScript<List<*>> {
        val script = DefaultRedisScript<List<*>>()
        script.setScriptText(
            """
            -- KEYS[1]: commentListKey, ARGV[1]: lastCommentIdString, ARGV[2]: limit, ARGV[3]: epsilon
            local lastScore = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not lastScore then return {} end
            local maxScore = '(' .. (tonumber(lastScore)) -- Use exclusive range by prefixing with '('
            -- ZREVRANGEBYSCORE max min ; use '(' for exclusive max score
            return redis.call('ZREVRANGEBYSCORE', KEYS[1], maxScore, '-inf', 'LIMIT', 0, tonumber(ARGV[2]))
            """.trimIndent(),
        )
        script.setResultType(List::class.java)
        return script
    }

    @Bean
    fun applyBoardCreationScript(): RedisScript<Long> {
        // 외부 파일 사용 권장: script.setLocation(ClassPathResource("lua/applyBoardCreation.lua"))
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: detailKey, KEYS[2]: allListKey, KEYS[3]: categoryListKey(없으면 ""), 
            -- KEYS[4]: countKey, KEYS[5]: eventTsKey
            -- ARGV[1]: boardJson, ARGV[2]: boardIdStr, ARGV[3]: categoryIdStr(없으면 ""), 
            -- ARGV[4]: score, ARGV[5]: eventTs, ARGV[6]: allListLimit, 
            -- ARGV[7]: categoryListLimit, ARGV[8]: tsTtlSeconds
            
            local eventTsKey = KEYS[5]
            local eventTs = tonumber(ARGV[5])
            local currentTs = tonumber(redis.call('HGET', eventTsKey, 'ts') or '0')

            if currentTs >= eventTs then return 0 end -- Skip

            redis.call('SET', KEYS[1], ARGV[1])
            redis.call('ZADD', KEYS[2], ARGV[4], ARGV[2])
            redis.call('ZREMRANGEBYRANK', KEYS[2], 0, -(tonumber(ARGV[6]) + 1))

            if KEYS[3] ~= "" and ARGV[3] ~= "" then
                redis.call('ZADD', KEYS[3], ARGV[4], ARGV[2])
                redis.call('ZREMRANGEBYRANK', KEYS[3], 0, -(tonumber(ARGV[7]) + 1))
            end
            redis.call('INCR', KEYS[4])

            redis.call('HSET', eventTsKey, 'ts', eventTs)
            redis.call('EXPIRE', eventTsKey, tonumber(ARGV[8]))

            return 1 -- Success
        """.trimIndent())
        script.resultType = Long::class.java
        return script
    }

    @Bean
    fun applyBoardUpdateScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: detailKey, KEYS[2]: eventTsKey
            -- ARGV[1]: updatedBoardJson, ARGV[2]: eventTs, ARGV[3]: tsTtlSeconds
            
            local eventTsKey = KEYS[2]
            local eventTs = tonumber(ARGV[2])
            local currentTs = tonumber(redis.call('HGET', eventTsKey, 'ts') or '0')

            if currentTs >= eventTs then return 0 end -- Skip

            redis.call('SET', KEYS[1], ARGV[1])
            
            redis.call('HSET', eventTsKey, 'ts', eventTs)
            redis.call('EXPIRE', eventTsKey, tonumber(ARGV[3]))
            
            return 1 -- Success
         """.trimIndent())
        script.resultType = Long::class.java
        return script
    }

    @Bean
    fun applyBoardDeletionScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: detailKey, KEYS[2]: allListKey, KEYS[3]: categoryListKey(없으면 ""), 
            -- KEYS[4]: countKey, KEYS[5]: eventTsKey, 
            -- KEYS[6..N]: 연관된 댓글 키들 (선택적 - 댓글 동시 삭제 시)
            -- ARGV[1]: boardIdStr, ARGV[2]: categoryIdStr(없으면 ""), ARGV[3]: eventTs
            
            local eventTsKey = KEYS[5]
            local eventTs = tonumber(ARGV[3])
            local currentTs = tonumber(redis.call('HGET', eventTsKey, 'ts') or '0')
            if currentTs >= eventTs then return 0 end -- Skip

            -- 연관 댓글 키 삭제 (선택적)
            -- for i = 6, #KEYS do redis.call('DEL', KEYS[i]) end

            redis.call('DEL', KEYS[1]) -- 상세 정보 삭제
            redis.call('ZREM', KEYS[2], ARGV[1]) -- 전체 목록에서 제거
            if KEYS[3] ~= "" and ARGV[2] ~= "" then
                redis.call('ZREM', KEYS[3], ARGV[1]) -- 카테고리 목록에서 제거
            end
            
            -- 카운터 감소 (음수 방지 고려 - 여기선 DECR 사용)
            local count = redis.call('DECR', KEYS[4]) 
            if count < 0 then redis.call('SET', KEYS[4], '0') end -- 음수면 0으로

            redis.call('DEL', eventTsKey) -- 타임스탬프 키 제거

            return 1 -- Success
        """.trimIndent())
        script.resultType = Long::class.java
        return script
    }

    @Bean fun applyBoardLikeUpdateScript(): RedisScript<Long> = applyBoardUpdateScript()
    @Bean fun applyBoardViewUpdateScript(): RedisScript<Long> = applyBoardUpdateScript()

    @Bean
    fun applyBoardRankingUpdateScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: rankingKey, KEYS[2]: eventTsKey
            -- ARGV[1]: eventTs, ARGV[2]: tsTtlSeconds, ARGV[3]: rankingLimit, 
            -- ARGV[4..N]: boardId1, score1, boardId2, score2, ...
            
            local eventTsKey = KEYS[2]
            local eventTs = tonumber(ARGV[1])
            local currentTs = tonumber(redis.call('HGET', eventTsKey, 'ts') or '0')
            if currentTs >= eventTs then return 0 end -- Skip

            -- 기존 랭킹 삭제
            redis.call('DEL', KEYS[1]) 

            -- 새 랭킹 추가 (ZADD boardId score ...)
            -- ARGV 인덱스는 4부터 시작, 2개씩 쌍으로 처리
            local rankingArgs = {}
            for i = 4, #ARGV, 2 do
                table.insert(rankingArgs, ARGV[i+1]) -- score 먼저
                table.insert(rankingArgs, ARGV[i])   -- boardId 다음
            end
            
            if #rankingArgs > 0 then
                 redis.call('ZADD', KEYS[1], unpack(rankingArgs))
                 -- 크기 제한
                 redis.call('ZREMRANGEBYRANK', KEYS[1], 0, -(tonumber(ARGV[3]) + 1))
            end

            redis.call('HSET', eventTsKey, 'ts', eventTs)
            redis.call('EXPIRE', eventTsKey, tonumber(ARGV[2]))

            return 1 -- Success
        """.trimIndent())
        script.resultType = Long::class.java
        return script
    }

    // --- Comment Scripts (3개) ---

    @Bean
    fun applyCommentCreationScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: commentDetailKey, KEYS[2]: boardCommentListKey, KEYS[3]: boardCommentCountKey, 
            -- KEYS[4]: totalCommentCountKey, KEYS[5]: commentEventTsKey, 
            -- KEYS[6]: boardDetailKey(게시글 댓글 수 업데이트용), KEYS[7]: boardEventTsKey(게시글 업데이트 확인용)
            -- ARGV[1]: commentJson, ARGV[2]: commentIdStr, ARGV[3]: boardIdStr, ARGV[4]: score, ARGV[5]: eventTs, 
            -- ARGV[6]: commentListLimit, ARGV[7]: tsTtlSeconds, ARGV[8]: updatedBoardJson (게시글 업데이트 필요시)
            
            local commentEventTsKey = KEYS[5]
            local eventTs = tonumber(ARGV[5])
            local currentTs = tonumber(redis.call('HGET', commentEventTsKey, 'ts') or '0')
            if currentTs >= eventTs then return 0 end -- Skip

            -- 1. 댓글 상세 저장
            redis.call('SET', KEYS[1], ARGV[1])
            -- 2. 게시글 댓글 목록 추가
            redis.call('ZADD', KEYS[2], ARGV[4], ARGV[2])
            redis.call('ZREMRANGEBYRANK', KEYS[2], 0, -(tonumber(ARGV[6]) + 1))
            -- 3. 카운터 증가
            redis.call('INCR', KEYS[3]) -- 게시글별 댓글 수
            redis.call('INCR', KEYS[4]) -- 전체 댓글 수

            -- 4. 게시글 상세 정보 업데이트 (댓글 수) - 선택적
            if KEYS[6] and ARGV[8] then
                 -- 게시글의 eventTs도 확인하여 최신 상태일 때만 업데이트 (선택적 강화)
                 local boardEventTsKey = KEYS[7]
                 local boardCurrentTs = tonumber(redis.call('HGET', boardEventTsKey, 'ts') or '0')
                 -- 댓글 이벤트가 게시글의 마지막 처리 이벤트보다 최신일 경우 갱신 시도 가능
                 -- (주의: 이 로직은 게시글 업데이트 시나리오와 충돌 가능성 있으므로 정책 재검토 필요)
                 -- 여기서는 게시글 eventTs 체크는 생략하고 무조건 업데이트 시도
                 redis.call('SET', KEYS[6], ARGV[8])
            end

            -- 5. 댓글 이벤트 타임스탬프 업데이트
            redis.call('HSET', commentEventTsKey, 'ts', eventTs)
            redis.call('EXPIRE', commentEventTsKey, tonumber(ARGV[7]))

            return 1 -- Success
        """.trimIndent())
        script.resultType = Long::class.java
        return script
    }

    @Bean
    fun applyCommentUpdateScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: commentDetailKey, KEYS[2]: commentEventTsKey
            -- ARGV[1]: updatedCommentJson, ARGV[2]: eventTs, ARGV[3]: tsTtlSeconds
            
            local eventTsKey = KEYS[2]
            local eventTs = tonumber(ARGV[2])
            local currentTs = tonumber(redis.call('HGET', eventTsKey, 'ts') or '0')

            if currentTs >= eventTs then return 0 end -- Skip

            redis.call('SET', KEYS[1], ARGV[1])
            
            redis.call('HSET', eventTsKey, 'ts', eventTs)
            redis.call('EXPIRE', eventTsKey, tonumber(ARGV[3]))
            
            return 1 -- Success
         """.trimIndent())
        script.resultType = Long::class.java
        return script
    }

    @Bean
    fun applyCommentDeletionScript(): RedisScript<Long> {
        val script = DefaultRedisScript<Long>()
        script.setScriptText("""
            -- KEYS[1]: commentDetailKey, KEYS[2]: boardCommentListKey, KEYS[3]: boardCommentCountKey, 
            -- KEYS[4]: totalCommentCountKey, KEYS[5]: commentEventTsKey, 
            -- KEYS[6]: boardDetailKey(게시글 댓글 수 업데이트용), KEYS[7]: boardEventTsKey(게시글 업데이트 확인용)
            -- ARGV[1]: commentIdStr, ARGV[2]: boardIdStr, ARGV[3]: eventTs, ARGV[4]: updatedBoardJson (게시글 업데이트 필요시)

            local commentEventTsKey = KEYS[5]
            local eventTs = tonumber(ARGV[3])
            local currentTs = tonumber(redis.call('HGET', commentEventTsKey, 'ts') or '0')
            if currentTs >= eventTs then return 0 end -- Skip

            -- 1. 댓글 상세 삭제
            redis.call('DEL', KEYS[1])
            -- 2. 게시글 댓글 목록에서 제거
            redis.call('ZREM', KEYS[2], ARGV[1])
            -- 3. 카운터 감소 (음수 방지)
            local bcCount = redis.call('DECR', KEYS[3])
            if bcCount < 0 then redis.call('SET', KEYS[3], '0') end
            local tcCount = redis.call('DECR', KEYS[4])
            if tcCount < 0 then redis.call('SET', KEYS[4], '0') end

            -- 4. 게시글 상세 정보 업데이트 (댓글 수) - 선택적
            if KEYS[6] and ARGV[4] then
                 -- 댓글 삭제는 비교적 안전하게 게시글 업데이트 가능 (게시글 eventTs 체크 생략)
                 redis.call('SET', KEYS[6], ARGV[4])
            end

            -- 5. 댓글 이벤트 타임스탬프 키 삭제
            redis.call('DEL', commentEventTsKey) 

            return 1 -- Success
        """.trimIndent())
        script.resultType = Long::class.java
        return script
    }
}
