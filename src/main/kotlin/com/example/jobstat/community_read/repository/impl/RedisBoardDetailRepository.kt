package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.repository.BoardDetailRepository
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
) : BoardDetailRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        // 키 포맷 정의
        fun detailKey(boardId: Long) = "board:detail:json::$boardId"

        fun detailStateKey(boardId: Long) = "board:detailstate::$boardId"
    }

    // --------------------------
    // 조회 관련 메소드
    // --------------------------

    /**
     * 게시글 상세 정보 조회
     */
    override fun findBoardDetail(boardId: Long): BoardReadModel? =
        redisTemplate
            .opsForValue()
            .get(detailKey(boardId))
            ?.let { dataSerializer.deserialize(it, BoardReadModel::class) }

    /**
     * 여러 게시글 상세 정보 조회
     */
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

    // --------------------------
    // 저장 관련 메소드
    // --------------------------

    /**
     * 게시글 상세 정보 저장
     */
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

    /**
     * 여러 게시글 상세 정보 저장
     */
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

    // --------------------------
    // 파이프라인 관련 메소드
    // --------------------------

    /**
     * 파이프라인에서 게시글 상세 정보 저장
     */
    override fun saveBoardDetailInPipeline(
        conn: StringRedisConnection,
        board: BoardReadModel,
    ) {
        val json =
            dataSerializer.serialize(board)
                ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        conn.set(detailKey(board.id), json)
        log.debug("게시글 상세 정보 저장/업데이트 (SET): boardId=${board.id}")
    }
}
