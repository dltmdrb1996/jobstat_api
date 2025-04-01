package com.example.jobstat.community.board.repository

import com.example.jobstat.community.board.entity.Board
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface BoardRepository{
    fun save(board: Board): Board

    fun findById(id: Long): Board

    fun findByAuthor(author: String, pageable: Pageable): Page<Board>

    fun findByCategory(categoryId: Long, pageable: Pageable): Page<Board>

    fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable
    ): Page<Board>

    fun findTopNByOrderByViewCountDesc(limit: Int): List<Board>

    fun findAllWithDetails(pageable: Pageable): Page<Board>

    fun search(keyword: String, pageable: Pageable): Page<Board>

    fun countByAuthor(author: String): Long

    fun existsByAuthorAndTitle(author: String, title: String): Boolean

    fun findViewCountById(id: Long): Int?

    fun findLikeCountById(id: Long): Int?

    fun updateViewCount(boardId: Long, count: Int)

    fun updateLikeCount(boardId: Long, count: Int)

    fun findAllByCategoryId(categoryId: Long): List<Board>

    fun findAll(pageable: Pageable): Page<Board>

    fun deleteById(id: Long)

    fun findAllByIds(ids: List<Long>): List<Board>

}
