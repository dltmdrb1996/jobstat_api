package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Comment
import com.example.jobstat.core.extension.orThrowNotFound
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface CommentJpaRepository : JpaRepository<Comment, Long> {
    fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment>

    @Query(
        """
        SELECT c FROM Comment c 
        WHERE c.board.id = :boardId 
        ORDER BY c.createdAt DESC
        """,
    )
    fun findRecentComments(
        @Param("boardId") boardId: Long,
        pageable: Pageable,
    ): List<Comment>

    fun countByBoardId(boardId: Long): Long

    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.board.id = :boardId")
    fun deleteByBoardId(
        @Param("boardId") boardId: Long,
    )
}

@Repository
internal class CommentRepositoryImpl(
    private val commentJpaRepository: CommentJpaRepository,
) : CommentRepository {
    override fun findAll(pageable: Pageable): Page<Comment> = commentJpaRepository.findAll(pageable)

    override fun save(comment: Comment): Comment = commentJpaRepository.save(comment)

    override fun findById(id: Long): Comment = commentJpaRepository.findById(id).orThrowNotFound("Comment", id)

    override fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment> = commentJpaRepository.findByBoardId(boardId, pageable)

    override fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<Comment> = commentJpaRepository.findRecentComments(boardId, pageable)

    override fun deleteById(id: Long) = commentJpaRepository.deleteById(id)

    override fun countByBoardId(boardId: Long): Long = commentJpaRepository.countByBoardId(boardId)

    override fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment> = commentJpaRepository.findByAuthor(author, pageable)

    override fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment> = commentJpaRepository.findByBoardIdAndAuthor(boardId, author, pageable)

    override fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean = commentJpaRepository.existsByBoardIdAndAuthor(boardId, author)

    override fun deleteByBoardId(boardId: Long) = commentJpaRepository.deleteByBoardId(boardId)
}
