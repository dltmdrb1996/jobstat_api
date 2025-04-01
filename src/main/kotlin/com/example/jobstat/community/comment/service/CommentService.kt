package com.example.jobstat.community.comment.service

import com.example.jobstat.community.comment.CommentConstants
import com.example.jobstat.community.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

internal interface CommentService {
    fun createComment(
        boardId: Long,
        content: String,
        author: String,
        password: String?,
        userId: Long? = null,
    ): Comment

    fun getCommentById(id: Long): Comment

    fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    fun getCommentsByAuthor(
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    fun updateComment(
        id: Long,
        content: String,
    ): Comment

    fun deleteComment(id: Long)

    fun countCommentsByBoardId(boardId: Long): Long

    fun hasCommentedOnBoard(
        boardId: Long,
        author: String,
    ): Boolean

    fun getCommentsByIds(ids: List<Long>): List<Comment>
}
