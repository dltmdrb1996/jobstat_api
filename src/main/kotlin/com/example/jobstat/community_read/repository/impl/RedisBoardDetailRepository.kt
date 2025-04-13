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
        // 키 이름은 그대로 유지 (JSON 문자열 저장)
        fun detailKey(boardId: Long) = "board:detail:json::$boardId"
        fun detailStateKey(boardId: Long) = "board:detailstate::$boardId"
    }

    // findBoardDetail: 변경 없음
    override fun findBoardDetail(boardId: Long): BoardReadModel? {
        return redisTemplate.opsForValue().get(detailKey(boardId))
            ?.let { dataSerializer.deserialize(it, BoardReadModel::class) }
    }

    // findBoardDetails: 변경 없음
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

    // saveBoardDetail: 변경 없음
    override fun saveBoardDetail(board: BoardReadModel, eventTs: Long) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            saveBoardDetailInPipeline(stringConn, board)
            null
        }
    }

    // saveBoardDetails: 변경 없음
    override fun saveBoardDetails(boards: List<BoardReadModel>, eventTs: Long) {
        if (boards.isEmpty()) return
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            boards.forEach { b ->
                saveBoardDetailInPipeline(stringConn, b)
            }
            null
        }
    }

    // saveBoardDetailInPipeline: 변경 없음 (JSON 문자열을 SET으로 저장)
    override fun saveBoardDetailInPipeline(conn: StringRedisConnection, board: BoardReadModel) {
        val json = dataSerializer.serialize(board)
            ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        conn.set(detailKey(board.id), json)
        log.debug("게시글 상세 정보 저장/업데이트 (SET): boardId=${board.id}") // 로그 수정
    }

    // updateBoardContentInPipeline: 삭제! 이 로직은 더 이상 유효하지 않음
    // override fun updateBoardContentInPipeline(...) { ... }
}