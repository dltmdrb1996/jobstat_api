package com.example.jobstat.community.counting

import com.example.jobstat.core.constants.RedisKeyConstants
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.RedisOperationUtils
import org.slf4j.LoggerFactory
// import org.springframework.core.io.ClassPathResource // 이제 ClassPathResource 필요 없음
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

/**
 * 게시글별 카운트(좋아요/조회수) 및 "likeUsers" 세트 관리를 담당.
 * - likeUsers:{boardId} 에 자정까지 TTL 설정
 * - 좋아요 수, 조회수는 별도 "likeCount:{boardId}", "viewCount:{boardId}" 키로 관리
 * - 배치 작업 시 카운터는 값을 읽고 삭제 (Atomic Get & Delete)
 */
@Repository
class CounterRepository(
    private val redisTemplate: StringRedisTemplate,
    private val atomicLikeScript: RedisScript<List<Long>?>,
    private val atomicUnlikeScript: RedisScript<List<Long>?>,
    private val getAndDeleteScript: RedisScript<String>
) {
    private val log = LoggerFactory.getLogger(this::class.java)


    /**
     * 지정된 게시글 ID와 관련된 모든 Redis 카운터 데이터를 삭제합니다.
     * - 조회수 키 (viewCount:{boardId})
     * - 좋아요 수 키 (likeCount:{boardId})
     * - 좋아요 사용자 세트 키 (likeUsers:{boardId})
     * - 배치 처리 대기 목록에서 해당 ID 제거 (pending_updates set)
     *
     * @param boardId 삭제할 게시글 ID
     */
    fun deleteBoardCounters(boardId: Long) {
        val boardIdStr = boardId.toString()
        val viewCountKey = RedisKeyConstants.Counter.viewCountKey(boardId)
        val likeCountKey = RedisKeyConstants.Counter.likeCountKey(boardId)
        val likeUsersKey = RedisKeyConstants.Counter.likeUsersKey(boardId)
        val pendingUpdatesKey = RedisKeyConstants.Counter.PENDING_UPDATES

        // 모아서 삭제할 키 목록
        val keysToDelete = listOf(viewCountKey, likeCountKey, likeUsersKey)

        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "게시글 카운터 데이터 삭제",
            detailInfo = "boardId=$boardId, keys=${keysToDelete.joinToString()}",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED // 또는 적절한 에러 코드
        ) {
            // 1. 관련 키들 삭제 (존재하지 않는 키는 무시됨)
            val deletedCount = redisTemplate.delete(keysToDelete)
            log.debug("Deleted {} counter keys for boardId {}", deletedCount, boardId)

            // 2. Pending Updates 세트에서 boardId 제거 (존재하지 않는 멤버는 무시됨)
            val removedFromPending = redisTemplate.opsForSet().remove(pendingUpdatesKey, boardIdStr)
            if (removedFromPending != null && removedFromPending > 0) {
                log.debug("Removed boardId {} from pending updates set.", boardId)
            }
        }
    }
    /**
     * ===========================
     * 1) 조회수 관련 (View Count)
     * ===========================
     */

    // 실시간 증가 (배치와 무관)
    fun incrementViewCount(boardId: Long): Long {
        val key = RedisKeyConstants.Counter.viewCountKey(boardId)
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 증가(increment)",
            detailInfo = "boardId=$boardId",
            errorCode = ErrorCode.VIEW_COUNT_UPDATE_FAILED
        ) {
            redisTemplate.opsForValue().increment(key) ?: 1L // 키 없으면 1로 시작
        }
    }

    // 현재 조회수 조회 (DB값과 합산 필요)
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

    // 여러 게시글 현재 조회수 조회 (DB값과 합산 필요)
    fun getBulkViewCounts(boardIds: List<Long>): Map<Long, Int> {
        if (boardIds.isEmpty()) return emptyMap()
        val keys = boardIds.map { RedisKeyConstants.Counter.viewCountKey(it) }
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "복수 게시글 조회수 조회 (MGET)",
            detailInfo = "${boardIds.size}개 게시글",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val results: List<String?> = redisTemplate.opsForValue().multiGet(keys) ?: emptyList()
            boardIds.zip(results) { boardId, resultString ->
                val count = resultString?.toIntOrNull() ?: 0
                boardId to count
            }.toMap()
        }
    }

    /**
     * (Deprecated: 배치 로직 변경됨)
     * 기존 배치용 메소드. getAndDeleteCount 또는 getAndDeleteCountsPipelined 사용 권장.
     */
    @Deprecated("Use getAndDeleteCount or getAndDeleteCountsPipelined", ReplaceWith(""))
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

    // 조회수 복원 (예외 처리 시 필요할 수 있음)
    fun restoreViewCount(boardId: Long, value: Int) {
        if (value == 0) return
        val key = RedisKeyConstants.Counter.viewCountKey(boardId)
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "조회수 복원",
            detailInfo = "boardId=$boardId, value=$value",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            // 주의: DECR 대신 INCR 사용. 음수값 복원 시에도 INCR 사용
            redisTemplate.opsForValue().increment(key, value.toLong())
        }
    }

    /**
     * ===========================
     * 2) 좋아요 수 (Like Count) & 좋아요 사용자 세트 (likeUsers Set)
     * ===========================
     */

    // 실시간 좋아요 (원자적)
    fun atomicLikeOperation(boardId: Long, userId: String): Int {
        log.info("게시글 좋아요 요청: boardId={}, userId={}", boardId, userId)
        val keys = listOf(
            RedisKeyConstants.Counter.likeUsersKey(boardId),
            RedisKeyConstants.Counter.likeCountKey(boardId),
            RedisKeyConstants.Counter.PENDING_UPDATES
        )
        val args = arrayOf(userId, boardId.toString(), calculateSecondsUntilMidnight().toString())

        // 주입받은 RedisScript Bean과 redisTemplate.execute 사용
        val result = redisTemplate.execute(atomicLikeScript, keys, *args)

        // 결과 처리 (반환 타입이 List<Long>? 또는 List<*>?)
        // execute의 반환 타입은 RedisScript<T>의 T를 따름
        if (result.size != 2) {
            throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED, "Redis 스크립트 실행/결과 오류 (Like)")
        }

        // List 내부 요소는 Long으로 가정하고 처리 (안전하게 하려면 as? Long 등 사용)
        val status = result[0]
        val value = result[1]
        if (status == 0L) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_OPERATION, "이미 좋아요 상태입니다.")
        }
        return value.toInt()
    }

    fun atomicUnlikeOperation(boardId: Long, userId: String): Int {
        log.info("[atomicUnlikeOperation] START boardId={}, userId={}", boardId, userId)
        val keys = listOf(
            RedisKeyConstants.Counter.likeUsersKey(boardId),
            RedisKeyConstants.Counter.likeCountKey(boardId),
            RedisKeyConstants.Counter.PENDING_UPDATES
        )
        val args = arrayOf(userId, boardId.toString())

        val result = redisTemplate.execute(atomicUnlikeScript, keys, *args)

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

    // 현재 좋아요 수 조회 (DB값과 합산 필요)
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

    // 여러 게시글 현재 좋아요 수 조회 (DB값과 합산 필요)
    fun getBulkLikeCounts(boardIds: List<Long>): Map<Long, Int> {
        if (boardIds.isEmpty()) return emptyMap()
        val keys = boardIds.map { RedisKeyConstants.Counter.likeCountKey(it) }
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "복수 게시글 좋아요 수 조회 (MGET)",
            detailInfo = "${boardIds.size}개 게시글",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val results: List<String?> = redisTemplate.opsForValue().multiGet(keys) ?: emptyList()
            boardIds.zip(results) { boardId, resultString ->
                val count = resultString?.toIntOrNull() ?: 0
                boardId to count
            }.toMap()
        }
    }

    /**
     * (Deprecated: 배치 로직 변경됨)
     * 기존 배치용 메소드. getAndDeleteCount 또는 getAndDeleteCountsPipelined 사용 권장.
     */
    @Deprecated("Use getAndDeleteCount or getAndDeleteCountsPipelined", ReplaceWith(""))
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
     * ===========================
     * 2.1) 배치 처리를 위한 Get & Delete
     * ===========================
     */

    /**
     * 단일 카운터 키에 대해 원자적으로 값을 가져오고 삭제 (Lua 스크립트 사용).
     * 키가 없거나 값이 숫자가 아니면 0 반환.
     * @param key 삭제할 카운터 키 (viewCountKey 또는 likeCountKey)
     * @return 가져온 값 (Int)
     */
    fun getAndDeleteCount(key: String): Int {
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "카운터 Get&Delete (Lua)",
            detailInfo = "key=$key",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED // 적절한 에러 코드
        ) {
            val result = redisTemplate.execute(getAndDeleteScript, listOf(key))
            result.toIntOrNull() ?: 0
        }
    }

    /**
     * 여러 카운터 키에 대해 Get & Delete 를 파이프라인으로 실행하고 결과를 반환합니다. (Lua 스크립트 사용)
     * @param keys 조회 및 삭제할 키 목록
     * @return 각 키에 대한 결과 값 리스트 (키가 없었으면 null 반환)
     */
    fun getAndDeleteCountsPipelined(keys: List<String>): List<Int?> {
        if (keys.isEmpty()) return emptyList()

        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "카운터 Get&Delete (Lua, Pipelined)",
            detailInfo = "${keys.size} keys",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED // 적절한 에러 코드
        ) {
            val results = redisTemplate.executePipelined { connection ->
                keys.forEach { key ->
                    connection.scriptingCommands().eval<String>( // 반환 타입을 String으로 지정
                        getAndDeleteScript.scriptAsString.toByteArray(), // 스크립트 내용 전달 (byte[])
                        org.springframework.data.redis.connection.ReturnType.VALUE, // 반환 타입 지정 (값)
                        1, // 키 개수
                        key.toByteArray() // 키 전달 (byte[])
                    )
                }
                return@executePipelined null
            }

            // 파이프라인 결과 처리
            results.map { result ->
                // eval 결과는 보통 Object 또는 지정 타입으로 옴 (String)
                when (result) {
                    is String -> result.toIntOrNull()
                    is ByteArray -> String(result).toIntOrNull() // eval 결과가 byte[]로 올 수도 있음
                    null -> null // 키가 없었거나 스크립트가 nil 반환
                    else -> {
                        log.warn("Unexpected result type in pipeline: ${result::class.java} - $result")
                        null
                    }
                }
            }
        }
    }


    /**
     * ===========================
     * 2.2) 좋아요 사용자 세트 관련 (likeUsers Set)
     * ===========================
     */

    // (addUserToLikeSet, removeUserFromLikeSet 은 atomic하게 like/unlike 내에서 처리되므로 별도 호출 필요 없어짐)
    // 필요하다면 유지 가능

    // 특정 사용자가 좋아요 했는지 확인
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

    // 특정 사용자가 여러 게시글에 좋아요 했는지 일괄 확인 (Pipeline 사용)
    fun getBulkUserLikedStatus(boardIds: List<Long>, userId: String): Map<Long, Boolean> {
        if (boardIds.isEmpty()) return emptyMap()

        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "복수 게시글 좋아요 상태 조회 (Pipeline)",
            detailInfo = "${boardIds.size}개 게시글, userId=$userId",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            val pipeline = redisTemplate.executePipelined { connection ->
                boardIds.forEach { boardId ->
                    val key = RedisKeyConstants.Counter.likeUsersKey(boardId)
                    connection.setCommands().sIsMember(key.toByteArray(), userId.toByteArray()) // SISMEMBER 호출
                }
                return@executePipelined null
            }

            // 파이프라인 결과 처리 (Boolean 리스트)
            boardIds.mapIndexed { index, boardId ->
                boardId to (pipeline.getOrNull(index) as? Boolean ?: false)
            }.toMap()
        }
    }

    // likeUsers:{boardId} 키에 자정 만료 시간 설정 (키 생성/접근 시 호출)
    private fun ensureExpireAtMidnight(key: String) {
        try {
            val currentExpire = redisTemplate.getExpire(key) ?: -1
            // TTL이 없거나(-1), 알 수 없을 때(-2)만 설정
            if (currentExpire < 0) {
                val seconds = calculateSecondsUntilMidnight()
                if (seconds > 0) {
                    redisTemplate.expire(key, Duration.ofSeconds(seconds))
                    log.info("키 {} 에 {}초 후 만료 설정 (=자정)", key, seconds)
                }
            }
        } catch (e: Exception) {
            log.error("키 만료 시간 설정 실패: key=$key", e)
            // 실패해도 핵심 로직에 영향은 적으므로 에러만 로깅
        }
    }

    // 현재 시간부터 다음 날 자정까지 남은 초 계산
    private fun calculateSecondsUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().atStartOfDay().plusDays(1)
        val duration = java.time.Duration.between(now, midnight)
        return if (duration.isNegative) 0 else duration.seconds
    }


    /**
     * ===========================
     * 3) 배치 처리 대기 목록 (Pending Updates Set)
     * ===========================
     */

    // 배치 처리 대기 목록에 boardId 추가
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

    // 배치 처리 완료된 boardId 목록 제거
    fun removePendingBoardIds(boardIds: List<String>) {
        if (boardIds.isEmpty()) return
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "대기 목록 제거",
            detailInfo = "${boardIds.size} items",
            errorCode = ErrorCode.REDIS_OPERATION_FAILED
        ) {
            // 한 번에 여러 멤버 제거
            redisTemplate.opsForSet().remove(RedisKeyConstants.Counter.PENDING_UPDATES, *boardIds.toTypedArray())
        }
    }

    // 처리 대기 중인 boardId 목록 조회
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