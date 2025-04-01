package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.BoardReadModel

interface BoardReadRepository {
    fun save(boardReadModel: BoardReadModel): BoardReadModel
    fun findById(boardId: Long): BoardReadModel?
    fun findAll(): List<BoardReadModel>
    fun findByAuthor(author: String): List<BoardReadModel>
    fun findByCategoryId(categoryId: Long): List<BoardReadModel>
    fun delete(boardId: Long)
    fun incrementViewCount(boardId: Long, amount: Int = 1): Int
    fun incrementLikeCount(boardId: Long, amount: Int = 1): Int
    fun decrementLikeCount(boardId: Long, amount: Int = 1): Int
    fun incrementCommentCount(boardId: Long, amount: Int = 1): Int
    fun findTopByViews(limit: Int): List<BoardReadModel>
    fun findTopByLikes(limit: Int): List<BoardReadModel>
    fun invalidateCache(boardId: Long)
    fun invalidateAllCaches()
}