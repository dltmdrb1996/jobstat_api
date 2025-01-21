package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Board

interface BoardRepository {
    fun save(board: Board): Board

    fun findById(id: Long): Board

    fun findByAuthor(author: String): List<Board>

    fun findAll(): List<Board>

    fun deleteById(id: Long)

    fun delete(board: Board)

    fun findTopNByOrderByViewCountDesc(n: Int): List<Board>

    fun findByTitleContainingOrContentContaining(keyword: String): List<Board>
}
