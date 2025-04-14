package com.example.jobstat.community.counting

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.*
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

/**
 * 게시글별 카운트(좋아요/조회수) 및 "likeUsers" 세트 관리를 담당.
 * - Redis 데이터에 대한 CRUD 및 원자적 연산을 수행합니다.
 * - 카운터 조회 시 최적화된 통합 조회 메소드를 제공합니다.
 */
@Repository
class CounterRepository(
    private val redisTemplate: StringRedisTemplate,
    // --- 스크립트 의존성 ---
    private val atomicLikeScript: RedisScript<List<Long>?>,
    private val atomicUnlikeScript: RedisScript<List<Long>?>,
    private val getAndDeleteScript: RedisScript<String>,
    private val atomicIncrementAndAddPendingScript: RedisScript<Long>,
    private val getCountersAndLikedStatusScript: RedisScript<List<Any>>,
) {
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
        val userLiked: Boolean, // userId 제공 시에만 정확
    )

    // ===========================
    // 1. 카운터 삭제/관리
    // ===========================

    /**
     * 지정된 게시글 ID와 관련된 모든 Redis 카운터 데이터를 삭제합니다.
     */
    fun deleteBoardCounters(boardId: Long) {
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

    // ===========================
    // 2. 조회수 원자적 증가
    // ===========================

    /**
     * 게시글 조회수를 원자적으로 증가시키고 배치 처리 대기 목록에 추가합니다 (Lua 스크립트 사용).
     */
    fun atomicIncrementViewCountAndAddPending(boardId: Long): Long {
        val viewCountKey = viewCountKey(boardId)
        val pendingKey = PENDING_UPDATES
        val boardIdStr = boardId.toString()

        return executeRedisOperation(
            operationName = "조회수 증가 및 대기 목록 추가 (Lua)",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.VIEW_COUNT_UPDATE_FAILED, // 조회수 관련 에러 코드 사용
        ) {
            val result =
                redisTemplate.execute(
                    atomicIncrementAndAddPendingScript,
                    listOf(viewCountKey, pendingKey),
                    boardIdStr,
                )
            // 스크립트가 Long을 반환하도록 정의됨
            result ?: throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                "Redis 스크립트 실행/결과 오류 (IncrementView)",
            )
        }
    }

    // ===========================
    // 3. 좋아요 원자적 처리
    // ===========================

    /**
     * 게시글 좋아요 처리 (Lua 스크립트 사용).
     */
    fun atomicLikeOperation(
        boardId: Long,
        userId: String,
    ): Int {
        log.info("게시글 좋아요 요청: boardId={}, userId={}", boardId, userId)
        val keys =
            listOf(
                likeUsersKey(boardId),
                likeCountKey(boardId),
                PENDING_UPDATES,
            )
        // likeUsers 세트에 TTL 설정 (자정까지)
        val args = arrayOf(userId, boardId.toString(), calculateSecondsUntilMidnight().toString())

        val result =
            executeRedisOperation(
                operationName = "게시글 좋아요 (Lua)",
                detailInfo = "boardId=$boardId, userId=$userId",
                errorCode = ErrorCode.REDIS_OPERATION_FAILED,
            ) {
                redisTemplate.execute(atomicLikeScript, keys, *args)
            }

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

    /**
     * 게시글 좋아요 취소 (Lua 스크립트 사용).
     */
    fun atomicUnlikeOperation(
        boardId: Long,
        userId: String,
    ): Int {
        log.info("게시글 좋아요 취소 요청: boardId={}, userId={}", boardId, userId)
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
            }

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

    // ===========================
    // 4. 카운터 정보 통합 조회 (최적화)
    // ===========================

    /**
     * 단일 게시글 Redis 카운터 통합 조회 (Lua 사용)
     */
    fun getSingleBoardCountersFromRedis(
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

    /**
     * 여러 게시글 Redis 카운터 통합 조회 (Pipeline 사용)
     */
    @Suppress("UNCHECKED_CAST")
    fun getBulkBoardCounters(
        boardIds: List<Long>,
        userId: String?,
    ): Map<Long, RedisBoardCounters> {
        if (boardIds.isEmpty()) return emptyMap()

        val viewCountKeys = boardIds.map { viewCountKey(it) }
        val likeCountKeys = boardIds.map { likeCountKey(it) }
        val hasUserId = userId != null

        return executeRedisOperation(
            operationName = "복수 게시글 카운터 통합 조회 (SessionCallback)",
            detailInfo = "boardIds=${boardIds.size}, userId=${userId ?: "N/A"}",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val results: List<Any?> =
                redisTemplate.execute(
                    object : SessionCallback<List<Any?>> {
                        override fun <K : Any?, V : Any?> execute(operations: RedisOperations<K, V>): List<Any?> {
                            val setOps = operations.opsForSet() as SetOperations<String, String>
                            val valueOps = operations.opsForValue() as ValueOperations<String, String>

                            // 1. 조회수 MGET 호출 (결과는 즉시 반환되지 않음)
                            if (viewCountKeys.isNotEmpty()) {
                                valueOps.multiGet(viewCountKeys)
                            }

                            // 2. 좋아요 수 MGET 호출
                            if (likeCountKeys.isNotEmpty()) {
                                valueOps.multiGet(likeCountKeys)
                            }

                            // 3. 사용자 좋아요 여부 SISMEMBER 호출 (userId 제공 시)
                            if (hasUserId) {
                                boardIds.forEach { boardId ->
                                    val userLikeKey = likeUsersKey(boardId)
                                    // isMember는 Boolean을 반환할 것으로 기대됨
                                    setOps.isMember(userLikeKey, userId!!)
                                }
                            }

                            return emptyList() // 실제 반환값은 이 리스트가 아님
                        }
                    },
                )

            // executePipelined와 유사하게 순서 기반 파싱 필요
            var resultIndex = 0

            val viewCounts: List<String?>? =
                if (viewCountKeys.isNotEmpty()) {
                    results.getOrNull(resultIndex++) as? List<String?>
                } else {
                    null
                }

            val likeCounts: List<String?>? =
                if (likeCountKeys.isNotEmpty()) {
                    results.getOrNull(resultIndex++) as? List<String?>
                } else {
                    null
                }

            val likedStatusList = mutableListOf<Boolean>()
            if (hasUserId) {
                repeat(boardIds.size) {
                    likedStatusList.add(results.getOrNull(resultIndex++) as? Boolean ?: false)
                }
            }

            boardIds
                .mapIndexed { index, boardId ->
                    val viewCount = viewCounts?.getOrNull(index)?.toIntOrNull() ?: 0
                    val likeCount = likeCounts?.getOrNull(index)?.toIntOrNull() ?: 0
                    val userLiked = likedStatusList.getOrNull(index) ?: false

                    boardId to RedisBoardCounters(viewCount, likeCount, userLiked)
                }.toMap()
        }
    }

    // ===========================
    // 5. 배치 처리 관련
    // ===========================

    /**
     * 단일 카운터 키에 대해 원자적으로 값을 가져오고 삭제 (Lua 스크립트 사용).
     */
    fun getAndDeleteCount(key: String): Int =
        executeRedisOperation(
            operationName = "카운터 Get&Delete (Lua)",
            detailInfo = "key=$key",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val result = redisTemplate.execute(getAndDeleteScript, listOf(key))
            result?.toIntOrNull() ?: 0
        }

    /**
     * 여러 카운터 키에 대해 Get & Delete 를 파이프라인으로 실행. (Lua 스크립트 사용)
     */
    fun getAndDeleteCountsPipelined(keys: List<String>): List<Int?> {
        if (keys.isEmpty()) return emptyList()

        return executeRedisOperation(
            operationName = "카운터 Get&Delete (Lua, Pipelined)",
            detailInfo = "${keys.size} keys",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            val results =
                redisTemplate.executePipelined { connection ->
                    keys.forEach { key ->
                        connection.scriptingCommands().eval<ByteArray>( // String 대신 ByteArray로 받아 파싱 시도
                            getAndDeleteScript.scriptAsString.toByteArray(),
                            org.springframework.data.redis.connection.ReturnType.VALUE,
                            1,
                            key.toByteArray(),
                        )
                    }
                    null
                }
            // 파이프라인 결과 처리 (ByteArray 또는 String 가능성 고려)
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

    /**
     * 배치 처리 완료된 게시글 ID 목록 제거.
     */
    fun removePendingBoardIds(boardIds: List<String>) {
        if (boardIds.isEmpty()) return
        executeRedisOperation(
            operationName = "대기 목록 제거",
            detailInfo = "${boardIds.size} items",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            redisTemplate.opsForSet().remove(PENDING_UPDATES, *boardIds.toTypedArray())
        }
    }

    /**
     * 처리 대기 중인 게시글 ID 목록 조회.
     */
    fun fetchPendingBoardIds(): Set<String> =
        executeRedisOperation(
            operationName = "대기 목록 조회",
            detailInfo = "pending updates",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED,
        ) {
            redisTemplate.opsForSet().members(PENDING_UPDATES) ?: emptySet()
        }

    // ===========================
    // 6. 유틸리티 메소드
    // ===========================

    // Redis 작업 실행 래퍼 (재시도 등 포함 가정)
    private fun <T> executeRedisOperation(
        operationName: String,
        detailInfo: String,
        errorCode: ErrorCode,
        operation: () -> T,
    ): T {
        // 실제 구현에서는 RedisOperationUtils.executeWithRetry 등을 사용
        try {
            return operation()
        } catch (e: Exception) {
            log.error("Redis 작업 실패: $operationName ($detailInfo)", e)
            throw AppException.fromErrorCode(errorCode, "Redis 작업 중 오류 발생: $operationName")
        }
    }

    // 자정까지 남은 시간 계산
    private fun calculateSecondsUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().atStartOfDay().plusDays(1)
        val duration = Duration.between(now, midnight)
        return if (duration.isNegative) 0 else duration.seconds
    }

    // 카운트 값 파싱 헬퍼 (String 또는 Number 처리)
    private fun parseCount(rawValue: Any?): Int =
        when (rawValue) {
            is Number -> rawValue.toInt()
            is String -> rawValue.toIntOrNull() ?: 0
            else -> 0
        }
}
