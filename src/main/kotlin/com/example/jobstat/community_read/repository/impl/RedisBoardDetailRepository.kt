package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.repository.BoardDetailRepository
import com.example.jobstat.community_read.repository.impl.RedisCommentCountRepository.Companion.getBoardCommentCountKey
import com.example.jobstat.community_read.repository.impl.RedisCommunityEventUpdateRepository.Companion.boardEventTsKey
import com.example.jobstat.community_read.service.CommunityEventHandlerVerPipe.Companion.EVENT_TS_TTL_SECONDS
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisBoardDetailRepository(
    private val redisTemplate: StringRedisTemplate,
    private val dataSerializer: DataSerializer,
) : BoardDetailRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun detailKey(boardId: Long) = "board:detail:json::$boardId"

        fun detailStateKey(boardId: Long) = "board:detailstate::$boardId"
    }

    override fun findBoardDetail(boardId: Long): BoardReadModel? =
        redisTemplate
            .opsForValue()
            .get(detailKey(boardId))
            ?.let { dataSerializer.deserialize(it, BoardReadModel::class) }

    override fun findBoardDetails(boardIds: List<Long>): Map<Long, BoardReadModel> {
        if (boardIds.isEmpty()) return emptyMap()

        val keys = boardIds.map { detailKey(it) }
        val values = redisTemplate.opsForValue().multiGet(keys)
        val resultMap = mutableMapOf<Long, BoardReadModel>()
        boardIds.forEachIndexed { index, boardId ->
            val data = values?.get(index)
            if (data != null) {
                dataSerializer.deserialize(data, BoardReadModel::class)?.let {
                    resultMap[boardId] = it
                }
            }
        }
        return resultMap
    }

    override fun saveBoardDetail(
        board: BoardReadModel,
        eventTs: Long,
    ) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            saveBoardDetailInPipeline(stringConn, board)
            null
        }
    }

    override fun saveBoardDetails(
        boards: List<BoardReadModel>,
        eventTs: Long,
    ) {
        if (boards.isEmpty()) return
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            boards.forEach { b ->
                saveBoardDetailInPipeline(stringConn, b)
            }
            null
        }
    }

    override fun saveBoardDetailInPipeline(
        conn: StringRedisConnection,
        board: BoardReadModel,
    ) {
        val json =
            dataSerializer.serialize(board)
                ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        val key = detailKey(board.id)
        val tsKey = boardEventTsKey(board.id) // 타임스탬프 키

        val commentCountKey = getBoardCommentCountKey(board.id)
        val commentCountValue = board.commentCount.toString()

        conn.set(key, json)
        conn.set(commentCountKey, commentCountValue)
        conn.hSet(tsKey, "ts", board.eventTs.toString())
        conn.expire(tsKey, EVENT_TS_TTL_SECONDS)
    }
}
