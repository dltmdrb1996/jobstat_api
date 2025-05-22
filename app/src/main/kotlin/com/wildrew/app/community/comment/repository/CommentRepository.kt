package com.wildrew.app.community.comment.repository

import com.wildrew.app.community.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommentRepository {
    fun save(comment: Comment): Comment

    fun deleteById(id: Long)

    fun delete(comment: Comment)

    fun deleteByBoardId(boardId: Long)

    fun findAll(pageable: Pageable): Page<Comment>

    fun findById(id: Long): Comment

    fun findAllByIds(ids: List<Long>): List<Comment>

    fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment>

    fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<Comment>

    fun findCommentsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment>

    fun countByBoardId(boardId: Long): Long

    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean

    fun findCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment>

    fun existsById(id: Long): Boolean
}
