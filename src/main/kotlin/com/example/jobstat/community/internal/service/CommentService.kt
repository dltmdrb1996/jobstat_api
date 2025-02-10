package com.example.jobstat.community.internal.service

import com.example.jobstat.community.internal.entity.ReadComment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommentService {
    fun createComment(
        boardId: Long,
        content: String,
        author: String,
        password: String?,
        userId: Long?,
    ): ReadComment

    fun getCommentById(id: Long): ReadComment

    fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<ReadComment>

    fun getCommentsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<ReadComment>

    fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<ReadComment>

    fun getRecentCommentsByBoardId(boardId: Long): List<ReadComment>

    fun updateComment(
        id: Long,
        content: String,
    ): ReadComment

    fun deleteComment(id: Long)

    fun countCommentsByBoardId(boardId: Long): Long

    fun hasCommentedOnBoard(
        boardId: Long,
        author: String,
    ): Boolean
}
