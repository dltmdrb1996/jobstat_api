package com.wildrew.app.community_read.repository

import com.wildrew.app.community_read.model.BoardReadModel
import org.springframework.data.redis.connection.StringRedisConnection

interface BoardDetailRepository {
    fun findBoardDetail(boardId: Long): BoardReadModel?

    fun findBoardDetails(boardIds: List<Long>): Map<Long, BoardReadModel>

    fun saveBoardDetail(
        board: BoardReadModel,
        eventTs: Long,
    )

    fun saveBoardDetails(
        boards: List<BoardReadModel>,
        eventTs: Long,
    )

    fun saveBoardDetailInPipeline(
        conn: StringRedisConnection,
        board: BoardReadModel,
    )
}
