package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Board
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

internal interface BoardRepository {
    fun save(board: Board): Board

    fun findById(id: Long): Board

    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board>

    fun findAll(pageable: Pageable): Page<Board>

    fun deleteById(id: Long)

    fun delete(board: Board)

    fun findTopNByOrderByViewCountDesc(limit: Int): List<Board>

    fun search(
        keyword: String?,
        pageable: Pageable,
    ): Page<Board>

    fun findAllWithDetails(pageable: Pageable): Page<Board>

    fun findByCategoryIdWithComments(categoryId: Long): List<Board>

    fun findByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun countByAuthor(author: String): Long

    fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean
}
