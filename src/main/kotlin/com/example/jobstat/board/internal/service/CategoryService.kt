package com.example.jobstat.board.internal.service

import com.example.jobstat.board.internal.entity.ReadBoardCategory

interface CategoryService {
    fun createCategory(
        name: String,
        displayName: String,
        description: String,
    ): ReadBoardCategory

    fun getCategoryById(id: Long): ReadBoardCategory

    fun getAllCategories(): List<ReadBoardCategory>

    fun updateCategory(
        id: Long,
        name: String,
        displayName: String,
        description: String,
    ): ReadBoardCategory

    fun deleteCategory(id: Long)

    fun isCategoryNameAvailable(name: String): Boolean
}
