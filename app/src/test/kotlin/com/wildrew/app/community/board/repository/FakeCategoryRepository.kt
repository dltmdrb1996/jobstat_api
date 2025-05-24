package com.wildrew.app.community.board.repository // 패키지 조정

import com.wildrew.app.community.board.entity.BoardCategory
import com.wildrew.app.community.board.fixture.CategoryFixture
import com.wildrew.app.utils.IndexManager // IndexManager 임포트 가정
import com.wildrew.app.utils.base.BaseFakeRepository // 제공된 BaseFakeRepository 임포트
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException

class FakeCategoryRepository : CategoryRepository {
    private val baseRepo =
        object : BaseFakeRepository<BoardCategory, CategoryFixture>() {
            override fun fixture(): CategoryFixture = CategoryFixture.aCategory()

            override fun createNewEntity(entity: BoardCategory): BoardCategory {
                if (!isValidId(entity.id)) {
                    setEntityId(entity, nextId())
                }
                return entity
            }

            override fun updateEntity(entity: BoardCategory): BoardCategory = entity

            override fun clearAdditionalState() {
                nameIndex.clear()
            }
        }

    private val nameIndex = IndexManager<String, Long>()

    override fun save(category: BoardCategory): BoardCategory {
        checkUniqueConstraints(category)
        val savedCategory = baseRepo.save(category)
        nameIndex.put(savedCategory.name, savedCategory.id)
        return savedCategory
    }

    override fun findById(id: Long): BoardCategory = baseRepo.findById(id)

    override fun findAll(): List<BoardCategory> = baseRepo.findAll()

    override fun findAllWithBoardCount(): List<Pair<BoardCategory, Long>> =
        baseRepo.findAll().map { category ->
            Pair(category, 0L)
        }

    override fun deleteById(id: Long) {
        val category = baseRepo.findByIdOrNull(id)
        category?.let {
            nameIndex.remove(it.name)
            baseRepo.deleteById(id)
        }
    }

    override fun existsByName(name: String): Boolean = nameIndex.containsKey(name)

    override fun findByName(name: String): BoardCategory {
        val categoryId =
            nameIndex.get(name)
                ?: throw EntityNotFoundException("Category not found with name: $name")
        return findById(categoryId)
    }

    private fun checkUniqueConstraints(category: BoardCategory) {
        nameIndex.get(category.name)?.let { existingId ->
            if (existingId != category.id) {
                if (baseRepo.isValidId(category.id)) {
                    throw DuplicateKeyException("Category name '${category.name}' already exists.")
                } else if (existingId > 0L) {
                    throw DuplicateKeyException("Category name '${category.name}' already exists.")
                }
            }
        }
    }

    fun clear() {
        baseRepo.clear()
    }
}
