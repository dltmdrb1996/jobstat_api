package com.example.jobstat.community.fake.repository

import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.repository.CategoryRepository
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.utils.IndexManager
import com.example.jobstat.utils.base.BaseFakeRepository
import jakarta.persistence.EntityNotFoundException

internal class FakeCategoryRepository : CategoryRepository {
    private val baseRepo =
        object : BaseFakeRepository<com.example.jobstat.community.board.entity.BoardCategory, CategoryFixture>() {
            override fun fixture() = CategoryFixture.aCategory()

            override fun createNewEntity(entity: com.example.jobstat.community.board.entity.BoardCategory): com.example.jobstat.community.board.entity.BoardCategory {
                if (!isValidId(entity.id)) {
                    setEntityId(entity, nextId())
                }
                return entity
            }

            override fun updateEntity(entity: com.example.jobstat.community.board.entity.BoardCategory): com.example.jobstat.community.board.entity.BoardCategory = entity

            override fun clearAdditionalState() {
                nameIndex.clear()
            }
        }
    private val nameIndex = IndexManager<String, Long>()

    override fun save(category: com.example.jobstat.community.board.entity.BoardCategory): com.example.jobstat.community.board.entity.BoardCategory {
        checkUniqueConstraints(category)
        val savedCategory = baseRepo.save(category)
        nameIndex.put(savedCategory.name, savedCategory.id)
        return savedCategory
    }

    override fun findById(id: Long): com.example.jobstat.community.board.entity.BoardCategory = baseRepo.findById(id)

    override fun findAll(): List<com.example.jobstat.community.board.entity.BoardCategory> = baseRepo.findAll()

    override fun findAllWithBoardCount(): List<Pair<com.example.jobstat.community.board.entity.BoardCategory, Long>> =
        baseRepo.findAll().map { category ->
            Pair(category, category.boards.size.toLong())
        }

    override fun deleteById(id: Long) {
        val category = baseRepo.findByIdOrNull(id) ?: return
        delete(category)
    }

    private fun delete(category: com.example.jobstat.community.board.entity.BoardCategory) {
        nameIndex.remove(category.name)
        baseRepo.delete(category)
    }

    override fun existsByName(name: String): Boolean = nameIndex.get(name) != null

    override fun findByName(name: String): com.example.jobstat.community.board.entity.BoardCategory {
        val categoryId =
            nameIndex.get(name)
                ?: throw EntityNotFoundException("해당 이름의 카테고리를 찾을 수 없습니다: $name")
        return findById(categoryId)
    }

    private fun checkUniqueConstraints(category: com.example.jobstat.community.board.entity.BoardCategory) {
        nameIndex.get(category.name)?.let { existingId ->
            if (existingId != category.id) {
                throw AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE)
            }
        }
    }

    fun clear() {
        baseRepo.clear()
        nameIndex.clear()
    }
}
