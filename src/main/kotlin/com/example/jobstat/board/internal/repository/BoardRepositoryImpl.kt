package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Board
import com.example.jobstat.core.extension.orThrowNotFound
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface BoardJpaRepository : JpaRepository<Board, Long> {
    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board>

    @Query("SELECT b FROM Board b ORDER BY b.viewCount DESC Limit :limit")
    fun findTopNByOrderByViewCountDesc(
        @Param("limit") limit: Int,
    ): List<Board>

    @Query(
        """
        SELECT DISTINCT b 
        FROM Board b 
        LEFT JOIN FETCH b.comments c 
        LEFT JOIN FETCH b.category 
        WHERE (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) 
            OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """,
    )
    fun search(
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<Board>

    @Query("SELECT DISTINCT b FROM Board b LEFT JOIN FETCH b.comments LEFT JOIN FETCH b.category")
    fun findAllWithDetails(pageable: Pageable): Page<Board>

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.comments WHERE b.category.id = :categoryId")
    fun findByCategoryIdWithComments(
        @Param("categoryId") categoryId: Long,
    ): List<Board>

    @Query("SELECT b FROM Board b WHERE b.category.id = :categoryId")
    fun findByCategory(
        @Param("categoryId") categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    @Query("SELECT b FROM Board b WHERE b.author = :author AND b.category.id = :categoryId")
    fun findByAuthorAndCategory(
        @Param("author") author: String,
        @Param("categoryId") categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun countByAuthor(author: String): Long

    fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean
}

@Repository
internal class BoardRepositoryImpl(
    private val boardJpaRepository: BoardJpaRepository,
) : BoardRepository {
    override fun save(board: Board): Board = boardJpaRepository.save(board)

    override fun findById(id: Long): Board = boardJpaRepository.findById(id).orThrowNotFound("Board", id)

    override fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.findByAuthor(author, pageable)

    override fun findAll(pageable: Pageable): Page<Board> = boardJpaRepository.findAll(pageable)

    override fun deleteById(id: Long) = boardJpaRepository.deleteById(id)

    override fun delete(board: Board) = boardJpaRepository.delete(board)

    override fun findTopNByOrderByViewCountDesc(limit: Int): List<Board> = boardJpaRepository.findTopNByOrderByViewCountDesc(limit)

    override fun search(
        keyword: String?,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.search(keyword, pageable)

    override fun findAllWithDetails(pageable: Pageable): Page<Board> = boardJpaRepository.findAllWithDetails(pageable)

    override fun findByCategoryIdWithComments(categoryId: Long): List<Board> = boardJpaRepository.findByCategoryIdWithComments(categoryId)

    override fun findByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.findByCategory(categoryId, pageable)

    override fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.findByAuthorAndCategory(author, categoryId, pageable)

    override fun countByAuthor(author: String): Long = boardJpaRepository.countByAuthor(author)

    override fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean = boardJpaRepository.existsByAuthorAndTitle(author, title)
}
