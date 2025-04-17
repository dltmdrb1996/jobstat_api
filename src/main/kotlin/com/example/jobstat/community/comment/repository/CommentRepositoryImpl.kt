package com.example.jobstat.community.comment.repository

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.core.global.extension.orThrowNotFound
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

internal interface CommentJpaRepository : JpaRepository<Comment, Long> {
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

    @Query("SELECT c FROM Comment c WHERE c.board.id = :boardId AND (:lastCommentId IS NULL OR c.id < :lastCommentId) ORDER BY c.id DESC")
    fun findCommentsByBoardIdAfter(
        @Param("boardId") boardId: Long,
        @Param("lastCommentId") lastCommentId: Long?,
        pageable: Pageable,
    ): List<Comment>

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.board.id = :boardId")
    fun deleteByBoardId(
        @Param("boardId") boardId: Long,
    )

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

    @Query("SELECT c FROM Comment c WHERE c.author = :author AND (:lastCommentId IS NULL OR c.id < :lastCommentId) ORDER BY c.id DESC")
    fun findCommentsByAuthorAfter(
        @Param("author") author: String,
        @Param("lastCommentId") lastCommentId: Long?,
        pageable: Pageable,
    ): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.id IN :ids")
    fun findAllByIdIn(
        @Param("ids") ids: List<Long>,
    ): List<Comment>
}

@Repository
internal class CommentRepositoryImpl(
    private val commentJpaRepository: CommentJpaRepository,
) : CommentRepository {
    override fun save(comment: Comment): Comment = commentJpaRepository.save(comment)

    override fun deleteById(id: Long) = commentJpaRepository.deleteById(id)

    override fun delete(comment: Comment) = commentJpaRepository.delete(comment)

    override fun deleteByBoardId(boardId: Long) = commentJpaRepository.deleteByBoardId(boardId)

    override fun findAll(pageable: Pageable): Page<Comment> = commentJpaRepository.findAll(pageable)

    override fun findById(id: Long): Comment = commentJpaRepository.findById(id).orThrowNotFound("Comment", id)

    override fun findAllByIds(ids: List<Long>): List<Comment> = commentJpaRepository.findAllByIdIn(ids)

    override fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment> = commentJpaRepository.findByBoardId(boardId, pageable)

    override fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<Comment> = commentJpaRepository.findRecentComments(boardId, pageable)

    override fun findCommentsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment> = commentJpaRepository.findCommentsByBoardIdAfter(boardId, lastCommentId, Pageable.ofSize(limit))

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

    override fun findCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment> = commentJpaRepository.findCommentsByAuthorAfter(author, lastCommentId, Pageable.ofSize(limit))

    override fun existsById(id: Long): Boolean = commentJpaRepository.existsById(id)
}
