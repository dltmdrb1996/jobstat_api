package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.repository.impl.RedisBoardIdListRepository as BoardListKeys // Alias 활용
import com.example.jobstat.community_read.repository.impl.RedisCommentIdListRepository as CommentListKeys
import com.example.jobstat.community_read.repository.CommunityEventUpdateRepository
import com.example.jobstat.community_read.utils.config.ReadSideLuaScriptConfig
import com.example.jobstat.community_read.repository.impl.RedisBoardCountRepository as BoardCountKeys
import com.example.jobstat.community_read.repository.impl.RedisBoardDetailRepository as BoardDetailKeys
import com.example.jobstat.community_read.repository.impl.RedisCommentCountRepository as CommentCountKeys
import com.example.jobstat.community_read.repository.impl.RedisCommentDetailRepository as CommentDetailKeys
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*
import com.example.jobstat.core.global.utils.serializer.DataSerializer // Serializer 필요
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisCommunityEventUpdateRepository(
    private val redisTemplate: StringRedisTemplate,
    private val dataSerializer: DataSerializer,
    private val applyBoardCreationScript: RedisScript<Long>,
    private val applyBoardUpdateScript: RedisScript<Long>,
    private val applyBoardDeletionScript: RedisScript<Long>,
    private val applyBoardLikeUpdateScript: RedisScript<Long>,
    private val applyBoardViewUpdateScript: RedisScript<Long>,
    private val applyBoardRankingUpdateScript: RedisScript<Long>,
    private val applyCommentCreationScript: RedisScript<Long>,
    private val applyCommentUpdateScript: RedisScript<Long>,
    private val applyCommentDeletionScript: RedisScript<Long>,
    @Value("\${redis.event-ts.ttl-days:1}") private val eventTsTtlDays: Long = 1L,
) : CommunityEventUpdateRepository {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val eventTsTtlSeconds: String by lazy { Duration.ofDays(eventTsTtlDays).seconds.toString() }

    // --- Event TS 키 생성 로직 ---
    companion object {
        private const val EVENT_TS_KEY_PREFIX = "community-read::event-ts::"
        fun boardEventTsKey(boardId: Long) = EVENT_TS_KEY_PREFIX + "board:$boardId"
        fun commentEventTsKey(commentId: Long) = EVENT_TS_KEY_PREFIX + "comment:$commentId"
        fun rankingEventTsKey(metric: String, period: String) = EVENT_TS_KEY_PREFIX + "ranking:$metric:$period"
    }

    // --- 인터페이스 구현 ---

    override fun applyBoardCreation(payload: BoardCreatedEventPayload): Boolean {
        val boardIdStr = payload.boardId.toString()
        val categoryIdStr = payload.categoryId.toString()
        val boardJson = dataSerializer.serialize(payload.toReadModel()) ?: run {
            log.error("게시글 생성 데이터 직렬화 실패: boardId={}", payload.boardId); return false
        }
        val categoryKey = BoardListKeys.CATEGORY_BOARDS_KEY_FORMAT.format(payload.categoryId)

        val keys = listOf(
            BoardDetailKeys.detailKey(payload.boardId), // KEYS[1]
            BoardListKeys.ALL_BOARDS_KEY,              // KEYS[2]
            categoryKey,                               // KEYS[3]
            BoardCountKeys.BOARD_TOTAL_COUNT_KEY,      // KEYS[4]
            boardEventTsKey(payload.boardId)           // KEYS[5]
        )
        val args = arrayOf(
            boardJson,                              // ARGV[1]
            boardIdStr,                             // ARGV[2]
            categoryIdStr,                          // ARGV[3]
            payload.eventTs.toDouble().toString(),  // ARGV[4] (score)
            payload.eventTs.toString(),             // ARGV[5] (eventTs)
            BoardListKeys.ALL_BOARD_LIMIT_SIZE.toString(),    // ARGV[6]
            BoardListKeys.CATEGORY_LIMIT_SIZE.toString(), // ARGV[7]
            eventTsTtlSeconds                       // ARGV[8]
        )
        return executeScript(applyBoardCreationScript, keys, args, "게시글 생성") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyBoardUpdate(payload: BoardUpdatedEventPayload, updatedBoardJson: String): Boolean {
        val keys = listOf(
            BoardDetailKeys.detailKey(payload.boardId), // KEYS[1]
            boardEventTsKey(payload.boardId)           // KEYS[2]
        )
        val args = arrayOf(
            updatedBoardJson,                     // ARGV[1]
            payload.eventTs.toString(),           // ARGV[2]
            eventTsTtlSeconds                     // ARGV[3]
        )
        return executeScript(applyBoardUpdateScript, keys, args, "게시글 수정") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyBoardDeletion(payload: BoardDeletedEventPayload): Boolean {
        val boardIdStr = payload.boardId.toString()
        val categoryIdStr = payload.categoryId?.toString() ?: ""
        val categoryKey = if (payload.categoryId != null) BoardListKeys.CATEGORY_BOARDS_KEY_FORMAT.format(payload.categoryId) else ""

        val keys = listOf(
            BoardDetailKeys.detailKey(payload.boardId), // KEYS[1]
            BoardListKeys.ALL_BOARDS_KEY,              // KEYS[2]
            categoryKey,                               // KEYS[3]
            BoardCountKeys.BOARD_TOTAL_COUNT_KEY,      // KEYS[4]
            boardEventTsKey(payload.boardId)           // KEYS[5]
        )
        val args = arrayOf(
            boardIdStr,                     // ARGV[1]
            categoryIdStr,                  // ARGV[2]
            payload.eventTs.toString()      // ARGV[3]
        )
        return executeScript(applyBoardDeletionScript, keys, args, "게시글 삭제") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyBoardLikeUpdate(payload: BoardLikedEventPayload, updatedBoardJson: String): Boolean {
        val keys = listOf(
            BoardDetailKeys.detailKey(payload.boardId),
            boardEventTsKey(payload.boardId)
        )
        val args = arrayOf(updatedBoardJson, payload.eventTs.toString(), eventTsTtlSeconds)
        return executeScript(applyBoardLikeUpdateScript, keys, args, "게시글 좋아요 업데이트") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyBoardViewUpdate(payload: BoardViewedEventPayload, updatedBoardJson: String): Boolean {
        val keys = listOf(
            BoardDetailKeys.detailKey(payload.boardId),
            boardEventTsKey(payload.boardId)
        )
        val args = arrayOf(updatedBoardJson, payload.eventTs.toString(), eventTsTtlSeconds)
        return executeScript(applyBoardViewUpdateScript, keys, args, "게시글 조회수 업데이트") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyBoardRankingUpdate(payload: BoardRankingUpdatedEventPayload): Boolean {
         val rankingKey = BoardListKeys.getRankingKey(payload.metric, payload.period) ?: return false
         val eventTsKey = rankingEventTsKey(payload.metric.name.lowercase(), payload.period.name.lowercase())

         val keys = listOf(rankingKey, eventTsKey)
         // ARGV 구성: eventTs, ttl, limit, [id1, score1, id2, score2, ...]
         val args = mutableListOf(
             payload.eventTs.toString(),
             eventTsTtlSeconds,
             BoardListKeys.RANKING_LIMIT_SIZE.toString()
         )
         payload.rankings.forEach { entry ->
             args.add(entry.boardId.toString())
             args.add(entry.score.toString())
         }

         return executeScript(applyBoardRankingUpdateScript, keys, args.toTypedArray(), "게시글 랭킹 업데이트") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyCommentCreation(payload: CommentCreatedEventPayload, commentJson: String, updatedBoardJson: String?): Boolean {
        val boardIdStr = payload.boardId.toString()
        val commentIdStr = payload.commentId.toString()
        val boardDetailKey = if (updatedBoardJson != null) BoardDetailKeys.detailKey(payload.boardId) else "" // 게시글 업데이트 필요 시에만 키 전달
        val boardEventTsKey = if (updatedBoardJson != null) boardEventTsKey(payload.boardId) else ""

        val keys = listOf(
            CommentDetailKeys.detailKey(payload.commentId),     // KEYS[1]
            CommentListKeys.getBoardCommentsKey(payload.boardId), // KEYS[2]
            CommentCountKeys.getBoardCommentCountKey(payload.boardId),// KEYS[3]
            CommentCountKeys.getTotalCommentCountKey(),         // KEYS[4]
            commentEventTsKey(payload.commentId),               // KEYS[5]
            boardDetailKey,                                    // KEYS[6]
            boardEventTsKey                                    // KEYS[7]
        )
        val args = arrayOf(
            commentJson,                                 // ARGV[1]
            commentIdStr,                                // ARGV[2]
            boardIdStr,                                  // ARGV[3]
            payload.eventTs.toDouble().toString(),       // ARGV[4] (score)
            payload.eventTs.toString(),                  // ARGV[5] (eventTs)
            CommentListKeys.COMMENT_LIMIT_SIZE.toString(), // ARGV[6]
            eventTsTtlSeconds,                           // ARGV[7]
            updatedBoardJson ?: ""                       // ARGV[8] (없으면 빈 문자열)
        )
        return executeScript(applyCommentCreationScript, keys, args, "댓글 생성") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyCommentUpdate(payload: CommentUpdatedEventPayload, updatedCommentJson: String): Boolean {
        val keys = listOf(
            CommentDetailKeys.detailKey(payload.commentId), // KEYS[1]
            commentEventTsKey(payload.commentId)            // KEYS[2]
        )
        val args = arrayOf(
            updatedCommentJson,                 // ARGV[1]
            payload.eventTs.toString(),         // ARGV[2]
            eventTsTtlSeconds                   // ARGV[3]
        )
        return executeScript(applyCommentUpdateScript, keys, args, "댓글 수정") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    override fun applyCommentDeletion(payload: CommentDeletedEventPayload, updatedBoardJson: String?): Boolean {
        val commentIdStr = payload.commentId.toString()
        val boardIdStr = payload.boardId.toString()
        val boardDetailKey = if (updatedBoardJson != null) BoardDetailKeys.detailKey(payload.boardId) else ""
        val boardEventTsKey = if (updatedBoardJson != null) boardEventTsKey(payload.boardId) else ""

        val keys = listOf(
            CommentDetailKeys.detailKey(payload.commentId),     // KEYS[1]
            CommentListKeys.getBoardCommentsKey(payload.boardId), // KEYS[2]
            CommentCountKeys.getBoardCommentCountKey(payload.boardId),// KEYS[3]
            CommentCountKeys.getTotalCommentCountKey(),         // KEYS[4]
            commentEventTsKey(payload.commentId),               // KEYS[5]
            boardDetailKey,                                    // KEYS[6]
            boardEventTsKey                                    // KEYS[7]
        )
        val args = arrayOf(
            commentIdStr,                        // ARGV[1]
            boardIdStr,                          // ARGV[2]
            payload.eventTs.toString(),          // ARGV[3]
            updatedBoardJson ?: ""               // ARGV[4]
        )
        return executeScript(applyCommentDeletionScript, keys, args, "댓글 삭제") == ReadSideLuaScriptConfig.SCRIPT_RESULT_SUCCESS
    }

    // Lua 스크립트 실행 공통 메소드
    private fun executeScript(script: RedisScript<Long>, keys: List<String>, args: Array<String>, operationDesc: String): Long {
        return try {
            val result = redisTemplate.execute(script, keys, *args)
            if (result == null) {
                log.error("Redis Lua 스크립트({}) 실행 결과 null: {}", script.sha1 ?: "N/A", operationDesc)
                throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED, "$operationDesc Lua 스크립트 결과 null")
            }
            // 스크립트가 0(Skipped) 또는 1(Success)을 반환한다고 가정
            if (result == ReadSideLuaScriptConfig.SCRIPT_RESULT_SKIPPED) {
                log.debug("Redis Lua 스크립트({}) 실행 건너뜀(오래된 이벤트): {}", script.sha1 ?: "N/A", operationDesc)
            }
            result
        } catch (e: Exception) {
            log.error("Redis Lua 스크립트({}) 실행 중 예외 발생: {}", script.sha1 ?: "N/A", operationDesc, e)
            // 예외를 다시 던지거나, 상황에 따라 -1 등 에러 코드를 반환하도록 처리 가능
            throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED, "$operationDesc 처리 중 오류 발생")
        }
    }
}