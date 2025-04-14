package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.repository.CategoryRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
internal class CategoryServiceImpl(
    private val categoryRepository: CategoryRepository,
) : CategoryService {
    override fun createCategory(
        name: String,
        displayName: String,
        description: String,
    ): BoardCategory {
        if (categoryRepository.existsByName(name)) {
            throw AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE)
        }
        return BoardCategory
            .create(name, displayName, description)
            .let(categoryRepository::save)
    }

    @Transactional(readOnly = true)
    override fun getCategoryById(id: Long): BoardCategory = categoryRepository.findById(id)

    @Transactional(readOnly = true)
    override fun getAllCategories(): List<BoardCategory> = categoryRepository.findAll()

    override fun updateCategory(
        id: Long,
        name: String,
        displayName: String,
        description: String,
    ): BoardCategory =
        categoryRepository.findById(id).apply {
            updateCategory(name, displayName, description)
        }

    override fun deleteCategory(id: Long) {
        categoryRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    override fun isCategoryNameAvailable(name: String): Boolean = !categoryRepository.existsByName(name)
}
