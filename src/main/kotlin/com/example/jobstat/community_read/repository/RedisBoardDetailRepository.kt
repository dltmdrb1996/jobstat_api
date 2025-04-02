package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisBoardDetailRepository(
    private val redisTemplate: StringRedisTemplate,
    private val dataSerializer: DataSerializer,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        fun detailKey(boardId: Long) = "board:detail:json::$boardId"
        fun detailStateKey(boardId: Long) = "board:detailstate::$boardId"
    }

    // ========================
    // 1) Read
    // ========================
    fun findBoardDetail(boardId: Long): BoardReadModel? {
        return redisTemplate.opsForValue().get(detailKey(boardId))
            ?.let { dataSerializer.deserialize(it, BoardReadModel::class) }
    }

    // bulk read (pipeline) – optional
    fun findBoardDetails(boardIds: List<Long>): Map<Long, BoardReadModel> {
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

    // ========================
    // 2) Write (with eventTs)
    // ========================
    fun saveBoardDetail(board: BoardReadModel, eventTs: Long) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            saveBoardDetailInPipeline(stringConn, board, eventTs)
            null
        }
    }

    fun saveBoardDetails(boards: List<BoardReadModel>, eventTs: Long) {
        if (boards.isEmpty()) return
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            boards.forEach { b ->
                saveBoardDetailInPipeline(stringConn, b, eventTs)
            }
            null
        }
    }

    /**
     * 파이프라인 내부 – eventTs 비교 후 JSON 직렬화
     */
    fun saveBoardDetailInPipeline(conn: StringRedisConnection, board: BoardReadModel, eventTs: Long) {
        val stateKey = detailStateKey(board.id)
        val json = dataSerializer.serialize(board)
            ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        conn.set(detailKey(board.id), json)
        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }

    /**
     * 게시글 수정 (제목, 내용만 업데이트)
     */
    fun updateBoardContentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        title: String,
        content: String,
        eventTs: Long
    ) {
        val detailKey = detailKey(boardId)
        val stateKey = detailStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "lastDetailUpdateTs")?.toLongOrNull() ?: 0

        if (eventTs <= currentTs) {
            // 이미 처리된 이벤트면 무시
            return
        }

        // 제목, 내용 업데이트
        conn.hSet(detailKey, "title", title)
        conn.hSet(detailKey, "content", content)

        // 이벤트 타임스탬프 업데이트
        conn.hSet(stateKey, "lastDetailUpdateTs", eventTs.toString())

        log.debug("Updated board content: boardId={}, eventTs={}", boardId, eventTs)
    }
}