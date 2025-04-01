package com.example.jobstat.community_read.repository.temp

import com.example.jobstat.community_read.model.CommentReadModel

interface CommentReadRepository {
    fun save(commentReadModel: CommentReadModel): CommentReadModel
    fun findById(commentId: Long): CommentReadModel?
    fun findByBoardId(boardId: Long): List<CommentReadModel>
    fun findByAuthor(author: String): List<CommentReadModel>
    fun delete(commentId: Long)
}