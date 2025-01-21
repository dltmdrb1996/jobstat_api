package com.example.jobstat.board

import com.example.jobstat.board.internal.entity.Comment

interface CommentService {
    fun createComment(
        boardId: Long,
        content: String,
        author: String,
    ): Comment

    fun getCommentById(id: Long): Comment?

    fun getCommentsByBoardId(boardId: Long): List<Comment>

    fun getRecentCommentsByBoardId(boardId: Long): List<Comment>

    fun updateComment(
        id: Long,
        content: String,
    ): Comment

    fun deleteComment(id: Long)
}
