package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Comment
import com.example.jobstat.core.extension.orThrowNotFound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

interface CommentJpaRepository : JpaRepository<Comment, Long> {
    fun findByBoardId(boardId: Long): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.board.id = :boardId ORDER BY c.createdAt DESC LIMIT 5")
    fun findTop5ByBoardIdOrderByCreatedAtDesc(
        @Param("boardId") boardId: Long,
    ): List<Comment>
}

@Repository
internal class CommentRepositoryImpl(
    private val commentJpaRepository: CommentJpaRepository,
) : CommentRepository {
    override fun save(comment: Comment): Comment = commentJpaRepository.save(comment)

    override fun findById(id: Long): Comment = commentJpaRepository.findById(id).orThrowNotFound("Comment", id)

    override fun findByBoardId(boardId: Long): List<Comment> = commentJpaRepository.findByBoardId(boardId)

    override fun findTop5ByBoardIdOrderByCreatedAtDesc(boardId: Long): List<Comment> =
        commentJpaRepository.findTop5ByBoardIdOrderByCreatedAtDesc(boardId)

    override fun deleteById(id: Long) = commentJpaRepository.deleteById(id)
}
