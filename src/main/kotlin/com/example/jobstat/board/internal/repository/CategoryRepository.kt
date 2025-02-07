package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.BoardCategory

interface CategoryRepository {
    fun save(category: BoardCategory): BoardCategory

    fun findById(id: Long): BoardCategory

    fun findAll(): List<BoardCategory>

    fun findAllWithBoardCount(): List<Pair<BoardCategory, Long>>

    fun deleteById(id: Long)

    fun existsByName(name: String): Boolean

    fun findByName(name: String): BoardCategory
}
