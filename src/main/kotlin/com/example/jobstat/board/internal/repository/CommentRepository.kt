package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Comment

internal interface CommentRepository {
    fun save(comment: Comment): Comment

    fun findById(id: Long): Comment

    fun findByBoardId(boardId: Long): List<Comment>

    fun findTop5ByBoardIdOrderByCreatedAtDesc(boardId: Long): List<Comment>

    fun deleteById(id: Long)
}
