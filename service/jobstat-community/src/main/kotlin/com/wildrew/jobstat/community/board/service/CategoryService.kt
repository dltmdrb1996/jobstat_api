package com.wildrew.jobstat.community.board.service

import com.wildrew.jobstat.community.board.entity.BoardCategory

interface CategoryService {
    fun createCategory(
        name: String,
        displayName: String,
        description: String,
    ): BoardCategory

    fun getCategoryById(id: Long): BoardCategory

    fun getAllCategories(): List<BoardCategory>

    fun updateCategory(
        id: Long,
        name: String,
        displayName: String,
        description: String,
    ): BoardCategory

    fun deleteCategory(id: Long)

    fun isCategoryNameAvailable(name: String): Boolean
}
