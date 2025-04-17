package com.example.jobstat.community.board.repository

import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.core.global.extension.orThrowNotFound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

internal interface CategoryJpaRepository : JpaRepository<BoardCategory, Long> {
    fun existsByName(name: String): Boolean

    fun findByName(name: String): Optional<BoardCategory>

    @Query("SELECT c, COUNT(b) FROM BoardCategory c LEFT JOIN c.boards b GROUP BY c")
    fun findAllWithBoardCount(): List<Array<Any>>
}

@Repository
internal class CategoryRepositoryImpl(
    private val categoryJpaRepository: CategoryJpaRepository,
) : CategoryRepository {
    override fun save(category: BoardCategory): BoardCategory = categoryJpaRepository.save(category)

    override fun findById(id: Long): BoardCategory = categoryJpaRepository.findById(id).orThrowNotFound("Category", id)

    override fun findAll(): List<BoardCategory> = categoryJpaRepository.findAll()

    override fun findAllWithBoardCount(): List<Pair<BoardCategory, Long>> =
        categoryJpaRepository
            .findAllWithBoardCount()
            .map { result -> Pair(result[0] as BoardCategory, result[1] as Long) }

    override fun deleteById(id: Long) = categoryJpaRepository.deleteById(id)

    override fun existsByName(name: String): Boolean = categoryJpaRepository.existsByName(name)

    override fun findByName(name: String): BoardCategory = categoryJpaRepository.findByName(name).orThrowNotFound("Category with name", name)
}
