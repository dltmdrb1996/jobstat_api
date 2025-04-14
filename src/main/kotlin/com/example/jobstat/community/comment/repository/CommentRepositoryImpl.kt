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

/**
 * 댓글 JPA 저장소 인터페이스
 * Spring Data JPA가 제공하는 기능을 활용합니다.
 */
internal interface CommentJpaRepository : JpaRepository<Comment, Long> {
    // ===================================================
    // 게시글 기반 조회 메소드
    // ===================================================

    /**
     * 게시글 ID로 댓글 목록을 조회합니다.
     */
    fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID로 최근 댓글 목록을 조회합니다.
     */
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

    /**
     * 게시글 ID로 댓글 수를 조회합니다.
     */
    fun countByBoardId(boardId: Long): Long

    /**
     * 무한 스크롤을 위한 커서 기반 댓글 조회 메소드입니다.
     */
    @Query("SELECT c FROM Comment c WHERE c.board.id = :boardId AND (:lastCommentId IS NULL OR c.id < :lastCommentId) ORDER BY c.id DESC")
    fun findCommentsByBoardIdAfter(
        @Param("boardId") boardId: Long,
        @Param("lastCommentId") lastCommentId: Long?,
        pageable: Pageable,
    ): List<Comment>

    /**
     * 게시글 ID로 모든 댓글을 삭제합니다.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.board.id = :boardId")
    fun deleteByBoardId(
        @Param("boardId") boardId: Long,
    )

    // ===================================================
    // 작성자 기반 조회 메소드
    // ===================================================

    /**
     * 작성자로 댓글 목록을 조회합니다.
     */
    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 목록을 조회합니다.
     */
    fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 존재 여부를 확인합니다.
     */
    fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean

    /**
     * 무한 스크롤을 위한 작성자별 커서 기반 댓글 조회 메소드입니다.
     */
    @Query("SELECT c FROM Comment c WHERE c.author = :author AND (:lastCommentId IS NULL OR c.id < :lastCommentId) ORDER BY c.id DESC")
    fun findCommentsByAuthorAfter(
        @Param("author") author: String,
        @Param("lastCommentId") lastCommentId: Long?,
        pageable: Pageable,
    ): List<Comment>

    // ===================================================
    // ID 기반 조회 메소드
    // ===================================================

    /**
     * 여러 댓글 ID로 댓글 목록을 조회합니다.
     */
    @Query("SELECT c FROM Comment c WHERE c.id IN :ids")
    fun findAllByIdIn(
        @Param("ids") ids: List<Long>,
    ): List<Comment>
}

/**
 * 댓글 저장소 구현 클래스
 * CommentJpaRepository를 통해 실제 데이터 접근을 수행합니다.
 */
@Repository
internal class CommentRepositoryImpl(
    private val commentJpaRepository: CommentJpaRepository,
) : CommentRepository {
    // ===================================================
    // 기본 CRUD 메소드
    // ===================================================

    override fun save(comment: Comment): Comment = commentJpaRepository.save(comment)

    override fun deleteById(id: Long) = commentJpaRepository.deleteById(id)

    override fun deleteByBoardId(boardId: Long) = commentJpaRepository.deleteByBoardId(boardId)

    // ===================================================
    // 댓글 조회 관련 메소드 (전체)
    // ===================================================

    override fun findAll(pageable: Pageable): Page<Comment> = commentJpaRepository.findAll(pageable)

    // ===================================================
    // 댓글 조회 관련 메소드 (ID 기반)
    // ===================================================

    override fun findById(id: Long): Comment = commentJpaRepository.findById(id).orThrowNotFound("Comment", id)

    override fun findAllByIds(ids: List<Long>): List<Comment> = commentJpaRepository.findAllByIdIn(ids)

    // ===================================================
    // 댓글 조회 관련 메소드 (게시글 기반)
    // ===================================================

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

    // ===================================================
    // 댓글 조회 관련 메소드 (작성자 기반)
    // ===================================================

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
}
