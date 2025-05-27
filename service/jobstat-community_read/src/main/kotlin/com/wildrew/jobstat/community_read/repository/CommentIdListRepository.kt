package com.wildrew.jobstat.community_read.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection

interface CommentIdListRepository {
    fun readAllByBoard(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long>

    fun readAllByBoardInfiniteScroll(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long>

    fun readCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long>

    fun readCommentsByBoardIdByCursor(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long>

    fun getBoardCommentsKey(boardId: Long): String

    fun add(
        boardId: Long,
        commentId: Long,
        sortValue: Double,
    )

    fun delete(
        boardId: Long,
        commentId: Long,
    )

    fun addCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
        score: Double,
    )

    fun removeCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
    )
}
