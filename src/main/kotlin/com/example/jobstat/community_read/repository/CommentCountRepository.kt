package com.example.jobstat.community_read.repository

import org.springframework.data.redis.connection.StringRedisConnection

interface CommentCountRepository {
    fun getCommentCountByBoardId(boardId: Long): Long

    fun getTotalCount(): Long

    fun applyBoardCommentCountInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        delta: Long,
    )

    fun applyTotalCountInPipeline(
        conn: StringRedisConnection,
        delta: Long,
    )
}
