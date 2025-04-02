package com.example.jobstat.community.counting

import com.example.jobstat.core.constants.RedisKeyConstants
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.RedisOperationUtils
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime

/**
 * 게시글별 카운트(좋아요/조회수) 및 "likeUsers" 세트 관리를 담당.
 * - likeUsers:{boardId} 에 자정까지 TTL 설정
 * - 좋아요 수, 조회수는 별도 "likeCount:{boardId}", "viewCount:{boardId}" 키로 관리
 */
@Repository
class CounterRepository(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * ===========================
     * 1) 조회수 관련
     * ===========================
     */
    fun incrementViewCount(boardId: Long): Long {
        val key = RedisKeyConstants.Counter.viewCountKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 증가(increment)",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.VIEW_COUNT_UPDATE_FAILED
        ) {
            redisTemplate.opsForValue().increment(key) ?: 1L
        }
    }

    fun getViewCount(boardId: Long): Int {
        val key = RedisKeyConstants.Counter.viewCountKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 조회",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForValue().get(key)?.toIntOrNull() ?: 0
        }
    }

    fun getBulkViewCounts(boardIds: List<Long>): Map<Long, Int> {
        if (boardIds.isEmpty()) return emptyMap()

        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "복수 게시글 조회수 조회",
            detailInfo = "${boardIds.size}개 게시글",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val pipeline = redisTemplate.executePipelined { connection ->
                val conn = connection as StringRedisConnection
                boardIds.forEach { boardId ->
                    val key = RedisKeyConstants.Counter.viewCountKey(boardId)
                    conn.get(key)
                }
            }

            boardIds.mapIndexed { index, boardId ->
                boardId to (pipeline.getOrNull(index)?.toString()?.toIntOrNull() ?: 0)
            }.toMap()
        }
    }

    fun getAndResetViewCount(boardId: Long): Int {
        val key = RedisKeyConstants.Counter.viewCountKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 getAndSet(0)",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForValue().getAndSet(key, "0")?.toIntOrNull() ?: 0
        }
    }

    fun restoreViewCount(boardId: Long, value: Int) {
        if (value == 0) return
        val key = RedisKeyConstants.Counter.viewCountKey(boardId)
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 복원",
            detailInfo = "boardId=$boardId, value=$value",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForValue().increment(key, value.toLong())
        }
    }

    /**
     * ===========================
     * 2) 좋아요 수, likeUsers 세트
     * ===========================
     */

    // CounterRepository.kt에 추가
    fun atomicLikeOperation(boardId: Long, userId: String): Int {
        val script = """
        local userKey = KEYS[1]
        local countKey = KEYS[2]
        local userId = ARGV[1]
        
        -- 이미 좋아요 상태인지 확인
        if redis.call('SISMEMBER', userKey, userId) == 1 then
            return -1  -- 이미 좋아요 상태
        end
        
        -- 좋아요 추가 및 카운트 증가
        redis.call('SADD', userKey, userId)
        local newCount = redis.call('INCR', countKey)
        
        -- 대기 목록에 추가
        redis.call('SADD', KEYS[3], ARGV[2])
        
        return newCount
    """

        val keys = listOf(
            RedisKeyConstants.Counter.likeUsersKey(boardId),
            RedisKeyConstants.Counter.likeCountKey(boardId),
            RedisKeyConstants.Counter.PENDING_UPDATES
        )

        val args = listOf(userId, boardId.toString())

        val result = redisTemplate.execute(
            DefaultRedisScript(script, Long::class.java),
            keys,
            args
        ) ?: 0

        if (result == -1L) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_OPERATION, "이미 좋아요 상태입니다.")
        }

        return result.toInt()
    }

    // CounterRepository.kt에 추가
    fun atomicUnlikeOperation(boardId: Long, userId: String): Int {
        val script = """
        local userKey = KEYS[1]
        local countKey = KEYS[2]
        local userId = ARGV[1]
        
        -- 좋아요 상태인지 확인
        if redis.call('SISMEMBER', userKey, userId) == 0 then
            return -1  -- 이미 좋아요 취소 상태
        end
        
        -- 좋아요 제거 및 카운트 감소
        redis.call('SREM', userKey, userId)
        local newCount = redis.call('DECR', countKey)
        
        -- 대기 목록에 추가
        redis.call('SADD', KEYS[3], ARGV[2])
        
        return newCount
    """

        val keys = listOf(
            RedisKeyConstants.Counter.likeUsersKey(boardId),
            RedisKeyConstants.Counter.likeCountKey(boardId),
            RedisKeyConstants.Counter.PENDING_UPDATES
        )

        val args = listOf(userId, boardId.toString())

        val result = redisTemplate.execute(
            DefaultRedisScript(script, Long::class.java),
            keys,
            args
        ) ?: 0

        if (result == -1L) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_OPERATION, "이미 좋아요 취소 상태입니다.")
        }

        return result.toInt()
    }

    fun getLikeCount(boardId: Long): Int {
        val key = RedisKeyConstants.Counter.likeCountKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "좋아요 수 조회",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForValue().get(key)?.toIntOrNull() ?: 0
        }
    }

    fun getBulkLikeCounts(boardIds: List<Long>): Map<Long, Int> {
        if (boardIds.isEmpty()) return emptyMap()

        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "복수 게시글 좋아요 수 조회",
            detailInfo = "${boardIds.size}개 게시글",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val pipeline = redisTemplate.executePipelined { connection ->
                val conn = connection as StringRedisConnection
                boardIds.forEach { boardId ->
                    val key = RedisKeyConstants.Counter.likeCountKey(boardId)
                    conn.get(key)
                }
            }

            boardIds.mapIndexed { index, boardId ->
                boardId to (pipeline.getOrNull(index)?.toString()?.toIntOrNull() ?: 0)
            }.toMap()
        }
    }

    fun getAndResetLikeCount(boardId: Long): Int {
        val key = RedisKeyConstants.Counter.likeCountKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "좋아요 getAndSet(0)",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForValue().getAndSet(key, "0")?.toIntOrNull() ?: 0
        }
    }

    /**
     * likeUsers:{boardId} 세트 = "현재 좋아요 상태"인 user 목록
     * -> 자정까지만 유지(익일 0시 만료)
     */
    fun addUserToLikeSet(boardId: Long, userId: String) {
        val userLikeKey = RedisKeyConstants.Counter.likeUsersKey(boardId)
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "likeUsers 세트 add",
            detailInfo = "boardId=$boardId, userId=$userId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val added = redisTemplate.opsForSet().add(userLikeKey, userId) ?: 0
            // 만약 아직 TTL이 안 걸려 있으면 설정
            ensureExpireAtMidnight(userLikeKey)
            added
        }
    }

    fun removeUserFromLikeSet(boardId: Long, userId: String): Boolean {
        val userLikeKey = RedisKeyConstants.Counter.likeUsersKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "likeUsers 세트 remove",
            detailInfo = "boardId=$boardId, userId=$userId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            (redisTemplate.opsForSet().remove(userLikeKey, userId) ?: 0) > 0
        }
    }

    fun hasUserLiked(boardId: Long, userId: String): Boolean {
        val userLikeKey = RedisKeyConstants.Counter.likeUsersKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "likeUsers 세트 membership",
            detailInfo = "boardId=$boardId, userId=$userId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForSet().isMember(userLikeKey, userId) ?: false
        }
    }

    fun getBulkUserLikedStatus(boardIds: List<Long>, userId: String): Map<Long, Boolean> {
        if (boardIds.isEmpty()) return emptyMap()

        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "복수 게시글 좋아요 상태 조회",
            detailInfo = "${boardIds.size}개 게시글, userId=$userId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val pipeline = redisTemplate.executePipelined { connection ->
                val conn = connection as StringRedisConnection
                boardIds.forEach { boardId ->
                    val key = RedisKeyConstants.Counter.likeUsersKey(boardId)
                    conn.sIsMember(key, userId)
                }
            }

            boardIds.mapIndexed { index, boardId ->
                boardId to (pipeline.getOrNull(index) as? Boolean ?: false)
            }.toMap()
        }
    }

    /**
     * 세트의 TTL이 설정되지 않았다면 자정까지 expire
     */
    private fun ensureExpireAtMidnight(key: String) {
        val currentExpire = redisTemplate.getExpire(key) ?: -1
        if (currentExpire < 0) {
            // 아직 expire가 없으면, "오늘 자정"까지 남은 시간을 계산
            val seconds = calculateSecondsUntilMidnight()
            if (seconds > 0) {
                redisTemplate.expire(key, Duration.ofSeconds(seconds))
                log.info("키 {} 에 {}초 후 만료 설정(=자정)", key, seconds)
            }
        }
    }

    /**
     * "오늘 ~ 자정"까지 남은 초 계산
     * (TimeZoneConfig 등 고려하여 현지 시각 기준 자정 계산)
     */
    private fun calculateSecondsUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().atStartOfDay().plusDays(1)  // 오늘 0시 + 1일 = 내일 0시
        return java.time.Duration.between(now, midnight).seconds
    }

    /**
     * ==============
     * 대기 목록 관리 (PENDING_UPDATES)
     * ==============
     */
    fun addPendingBoardId(boardId: Long) {
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "대기 목록 추가",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForSet().add(RedisKeyConstants.Counter.PENDING_UPDATES, boardId.toString())
        }
    }

    fun removePendingBoardIds(boardIds: List<String>) {
        if (boardIds.isEmpty()) return
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "대기 목록 제거",
            detailInfo = "${boardIds.size} items",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForSet().remove(RedisKeyConstants.Counter.PENDING_UPDATES, *boardIds.toTypedArray())
        }
    }

    fun fetchPendingBoardIds(): Set<String> {
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "대기 목록 조회",
            detailInfo = "pending updates",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            redisTemplate.opsForSet().members(RedisKeyConstants.Counter.PENDING_UPDATES) ?: emptySet()
        }
    }
}