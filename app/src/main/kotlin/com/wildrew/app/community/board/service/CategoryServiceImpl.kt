package com.wildrew.app.community.board.service

import com.wildrew.app.community.board.entity.BoardCategory
import com.wildrew.app.community.board.repository.CategoryRepository
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CategoryServiceImpl(
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
    ): BoardCategory {
        val category = categoryRepository.findById(id)

        if (name != category.name && categoryRepository.existsByName(name)) {
            throw AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "카테고리 이름 '$name'은(는) 이미 다른 카테고리에 존재합니다.")
        }

        category.apply {
            updateCategory(name, displayName, description)
        }

        return categoryRepository.save(category)
    }

    override fun deleteCategory(id: Long) {
        categoryRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    override fun isCategoryNameAvailable(name: String): Boolean = !categoryRepository.existsByName(name)
}
