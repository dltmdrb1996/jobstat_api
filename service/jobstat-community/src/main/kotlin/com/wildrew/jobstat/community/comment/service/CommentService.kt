package com.wildrew.jobstat.community.comment.service

import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.utils.CommentConstants
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommentService {
    fun createComment(
        boardId: Long,
        content: String,
        author: String,
        password: String?,
        userId: Long? = null,
    ): Comment

    fun updateComment(
        id: Long,
        content: String,
    ): Comment

    fun deleteComment(id: Long)

    fun getCommentById(id: Long): Comment

    fun getCommentsByIds(ids: List<Long>): List<Comment>

    fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    fun getCommentsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment>

    fun countCommentsByBoardId(boardId: Long): Long

    fun getCommentsByAuthor(
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    fun hasCommentedOnBoard(
        boardId: Long,
        author: String,
    ): Boolean

    fun getCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment>
}
