package com.example.jobstat.community.counting

import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.*
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime

@Repository
class RedisCounterRepository(
    private val redisTemplate: StringRedisTemplate,
    private val atomicLikeScript: RedisScript<List<Long>?>,
    private val atomicUnlikeScript: RedisScript<List<Long>?>,
    private val getAndDeleteScript: RedisScript<String>,
    private val atomicIncrementAndAddPendingScript: RedisScript<Long>,
    private val getCountersAndLikedStatusScript: RedisScript<List<Any>>,
) : CounterRepository {
    companion object {
        private const val SEPARATOR = ":"
        private const val NS_COMMUNITY = "community"
        private const val NS_COUNTER = "$NS_COMMUNITY:counter"
        private const val VIEW_COUNT_KEY_PREFIX = "$NS_COUNTER:view$SEPARATOR"
        private const val LIKE_COUNT_KEY_PREFIX = "$NS_COUNTER:like$SEPARATOR"
        const val PENDING_UPDATES = "$NS_COUNTER:pending-updates"

        fun viewCountKey(boardId: Long) = "$VIEW_COUNT_KEY_PREFIX$boardId"

        fun likeCountKey(boardId: Long) = "$LIKE_COUNT_KEY_PREFIX$boardId"

        fun likeUsersKey(boardId: Long) = "$NS_COUNTER:like$SEPARATOR$boardId$SEPARATOR:users"
    }

    private val log = LoggerFactory.getLogger(this::class.java)

    data class RedisBoardCounters(
        val viewCount: Int,
        val likeCount: Int,
        val userLiked: Boolean,
    )

    override fun deleteBoardCounters(boardId: Long) {
        val boardIdStr = boardId.toString()
        val viewCountKey = viewCountKey(boardId)
        val likeCountKey = likeCountKey(boardId)
        val likeUsersKey = likeUsersKey(boardId)
        val pendingUpdatesKey = PENDING_UPDATES

        val keysToDelete = listOf(viewCountKey, likeCountKey, likeUsersKey)

        executeRedisOperation(
            operationName = "게시글 카운터 데이터 삭제",
            detailInfo = "boardId=$boardId, keys=${keysToDelete.joinToString()}",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val deletedCount = redisTemplate.delete(keysToDelete)
            log.debug("게시글 ID {} 관련 카운터 키 {}개 삭제됨", boardId, deletedCount)

            val removedFromPending = redisTemplate.opsForSet().remove(pendingUpdatesKey, boardIdStr)
            if (removedFromPending != null && removedFromPending > 0) {
                log.debug("게시글 ID {}가 대기 목록에서 제거됨", boardId)
            }
        }
    }

    override fun atomicIncrementViewCountAndAddPending(boardId: Long): Long {
        val viewCountKey = viewCountKey(boardId)
        val pendingKey = PENDING_UPDATES
        val boardIdStr = boardId.toString()

        return executeRedisOperation(
            operationName = "조회수 증가 및 대기 목록 추가 (Lua)",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.VIEW_COUNT_UPDATE_FAILED,
        ) {
            val result =
                redisTemplate.execute(
                    atomicIncrementAndAddPendingScript,
                    listOf(viewCountKey, pendingKey),
                    boardIdStr,
                )
            result
        }
    }

    override fun atomicLikeOperation(
        boardId: Long,
        userId: String,
    ): Int {
        log.debug("게시글 좋아요 요청: boardId={}, userId={}", boardId, userId)
        val keys =
            listOf(
                likeUsersKey(boardId),
                likeCountKey(boardId),
                PENDING_UPDATES,
            )
        val args = arrayOf(userId, boardId.toString(), calculateSecondsUntilMidnight().toString())

        val result =
            executeRedisOperation(
                operationName = "게시글 좋아요 (Lua)",
                detailInfo = "boardId=$boardId, userId=$userId",
                errorCode = ErrorCode.REDIS_OPERATION_FAILED,
            ) {
                redisTemplate.execute(atomicLikeScript, keys, *args)
            } ?: emptyList()

        if (result.size != 2) {
            throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED, "Redis 스크립트 실행/결과 오류 (Like)")
        }

        val status = result[0]
        val value = result[1]
        if (status == 0L) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_OPERATION, "이미 좋아요 상태입니다.")
        }
        return value.toInt()
    }

    override fun atomicUnlikeOperation(
        boardId: Long,
        userId: String,
    ): Int {
        log.debug("게시글 좋아요 취소 요청: boardId={}, userId={}", boardId, userId)
        val keys =
            listOf(
                likeUsersKey(boardId),
                likeCountKey(boardId),
                PENDING_UPDATES,
            )
        val args = arrayOf(userId, boardId.toString())

        val result =
            executeRedisOperation(
                operationName = "게시글 좋아요 취소 (Lua)",
                detailInfo = "boardId=$boardId, userId=$userId",
                errorCode = ErrorCode.REDIS_OPERATION_FAILED,
            ) {
                redisTemplate.execute(atomicUnlikeScript, keys, *args)
            } ?: emptyList()

        if (result.size != 2) {
            throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED, "Redis 스크립트 실행/결과 오류 (Unlike)")
        }
        val status = result[0]
        val value = result[1]
        if (status == 0L) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_OPERATION, "이미 좋아요 취소 상태입니다.")
        }
        return value.toInt()
    }

    override fun getSingleBoardCountersFromRedis(
        boardId: Long,
        userId: String?,
    ): RedisBoardCounters {
        val viewKey = viewCountKey(boardId)
        val likeKey = likeCountKey(boardId)
        val userLikeKey = likeUsersKey(boardId)
        val userIdArg = userId ?: ""

        return executeRedisOperation(
            operationName = "단일 게시글 카운터 통합 조회 (Lua)",
            detailInfo = "boardId=$boardId, userId=${userId ?: "N/A"}",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val result: List<Any>? =
                redisTemplate.execute(
                    getCountersAndLikedStatusScript,
                    listOf(viewKey, likeKey, userLikeKey),
                    userIdArg,
                )

            if (result == null || result.size != 3) {
                log.error("Redis 스크립트(getCounters) 결과 오류. boardId: {}, result: {}", boardId, result)
                throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED, "Redis 스크립트 실행/결과 오류")
            }

            val viewCount = parseCount(result[0])
            val likeCount = parseCount(result[1])
            val userLiked = (result[2] as? Number)?.toInt() == 1

            RedisBoardCounters(viewCount, likeCount, userLiked)
        }
    }

    override fun getBulkBoardCounters(
        boardIds: List<Long>,
        userId: String?,
    ): Map<Long, RedisBoardCounters> {
        if (boardIds.isEmpty()) return emptyMap()

        return executeRedisOperation(
            operationName = "복수 게시글 카운터 통합 조회",
            detailInfo = "boardIds=${boardIds.size}, userId=${userId ?: "N/A"}",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val viewKeys = boardIds.map { viewCountKey(it) }
            val likeKeys = boardIds.map { likeCountKey(it) }

            // 1. 조회수 MGET으로 가져오기
            val viewCounts: List<String?> = redisTemplate.opsForValue().multiGet(viewKeys) ?: emptyList()
            // 2. 좋아요 수 MGET으로 가져오기
            val likeCounts: List<String?> = redisTemplate.opsForValue().multiGet(likeKeys) ?: emptyList()

            val likedStatusMap = mutableMapOf<Long, Boolean>()
            if (userId != null) {
                // 3. 사용자 좋아요 여부 (파이프라인 사용)
                val likeUserKeys = boardIds.map { likeUsersKey(it) }
                val isMemberResults: List<Any?> =
                    redisTemplate.executePipelined { connection ->
                        likeUserKeys.forEach { key ->
                            // StringRedisTemplate 사용 시 connection은 보통 DefaultStringRedisConnection
                            // key와 value는 byte array로 전달해야 할 수 있음
                            connection.setCommands().sIsMember(key.toByteArray(), userId.toByteArray())
                        }
                        null // Pipelining은 결과를 외부 리스트로 반환
                    } ?: emptyList()

                boardIds.forEachIndexed { index, boardId ->
                    // 파이프라인 결과는 Boolean 타입일 것으로 기대
                    likedStatusMap[boardId] = (isMemberResults.getOrNull(index) as? Boolean) ?: false
                }
            }

            // 4. 결과 조합
            boardIds
                .mapIndexed { index, boardId ->
                    val view = viewCounts.getOrNull(index)?.toIntOrNull() ?: 0
                    val like = likeCounts.getOrNull(index)?.toIntOrNull() ?: 0
                    // userId가 있을 경우 likedStatusMap에서 조회, 없으면 false
                    val liked = if (userId != null) likedStatusMap[boardId] ?: false else false
                    boardId to RedisBoardCounters(view, like, liked)
                }.toMap()
        }
    }

    override fun getAndDeleteCount(key: String): Int =
        executeRedisOperation(
            operationName = "카운터 Get&Delete (Lua)",
            detailInfo = "key=$key",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val result = redisTemplate.execute(getAndDeleteScript, listOf(key))
            result?.toIntOrNull() ?: 0
        }

    override fun getAndDeleteCountsPipelined(keys: List<String>): List<Int?> {
        if (keys.isEmpty()) return emptyList()

        return executeRedisOperation(
            operationName = "카운터 Get&Delete (Lua, Pipelined)",
            detailInfo = "${keys.size} keys",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val results =
                redisTemplate.executePipelined { connection ->
                    keys.forEach { key ->
                        connection.scriptingCommands().eval<ByteArray>(
                            getAndDeleteScript.scriptAsString.toByteArray(),
                            org.springframework.data.redis.connection.ReturnType.VALUE,
                            1,
                            key.toByteArray(),
                        )
                    }
                    null
                }
            results.map { result ->
                when (result) {
                    is String -> result.toIntOrNull()
                    is ByteArray -> String(result).toIntOrNull()
                    null -> null
                    else -> {
                        log.warn("Get&Delete 파이프라인에서 예상치 못한 결과 타입: ${result::class.java} - $result")
                        null
                    }
                }
            }
        }
    }

    override fun removePendingBoardIds(boardIds: List<String>) {
        if (boardIds.isEmpty()) return
        executeRedisOperation(
            operationName = "대기 목록 제거",
            detailInfo = "${boardIds.size} items",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            redisTemplate.opsForSet().remove(PENDING_UPDATES, *boardIds.toTypedArray())
        }
    }

    override fun fetchPendingBoardIds(): Set<String> =
        executeRedisOperation(
            operationName = "대기 목록 조회",
            detailInfo = "pending updates",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            redisTemplate.opsForSet().members(PENDING_UPDATES) ?: emptySet()
        }

    private fun <T> executeRedisOperation(
        operationName: String,
        detailInfo: String,
        errorCode: ErrorCode,
        operation: () -> T,
    ): T {
        try {
            return operation()
        } catch (e: Exception) {
            log.error("Redis 작업 실패: $operationName ($detailInfo)", e)
            throw AppException.fromErrorCode(errorCode, "Redis 작업 중 오류 발생: $operationName")
        }
    }

    private fun calculateSecondsUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().atStartOfDay().plusDays(1)
        val duration = Duration.between(now, midnight)
        return if (duration.isNegative) 0 else duration.seconds
    }

    private fun parseCount(rawValue: Any?): Int =
        when (rawValue) {
            is Number -> rawValue.toInt()
            is String -> rawValue.toIntOrNull() ?: 0
            else -> 0
        }
}
