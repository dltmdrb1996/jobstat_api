package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommentRepository {
    fun findAll(pageable: Pageable): Page<Comment>

    fun save(comment: Comment): Comment

    fun findById(id: Long): Comment

    fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment>

    fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<Comment>

    fun deleteById(id: Long)

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

    fun deleteByBoardId(boardId: Long)
}
