package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.entity.ReadBoardCategory

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
