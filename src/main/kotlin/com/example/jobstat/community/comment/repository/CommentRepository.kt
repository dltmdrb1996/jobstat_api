package com.example.jobstat.community.comment.repository

import com.example.jobstat.community.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

internal interface CommentRepository {
    // ===================================================
    // 기본 CRUD 메소드
    // ===================================================

    /**
     * 댓글을 저장합니다.
     *
     * @param comment 저장할 댓글 엔티티
     * @return 저장된 댓글 엔티티
     */
    fun save(comment: Comment): Comment

    /**
     * 댓글 ID로 댓글을 삭제합니다.
     *
     * @param id 삭제할 댓글 ID
     */
    fun deleteById(id: Long)

    /**
     * 게시글 ID로 모든 댓글을 삭제합니다.
     *
     * @param boardId 게시글 ID
     */
    fun deleteByBoardId(boardId: Long)

    // ===================================================
    // 댓글 조회 관련 메소드 (전체)
    // ===================================================

    /**
     * 페이징된 모든 댓글을 조회합니다.
     *
     * @param pageable 페이지 정보
     * @return 댓글 페이지
     */
    fun findAll(pageable: Pageable): Page<Comment>

    // ===================================================
    // 댓글 조회 관련 메소드 (ID 기반)
    // ===================================================

    /**
     * 댓글 ID로 댓글을 조회합니다.
     *
     * @param id 댓글 ID
     * @return 찾은 댓글 엔티티
     */
    fun findById(id: Long): Comment

    /**
     * 여러 댓글 ID로 댓글 목록을 조회합니다.
     *
     * @param ids 댓글 ID 목록
     * @return 찾은 댓글 엔티티 목록
     */
    fun findAllByIds(ids: List<Long>): List<Comment>

    // ===================================================
    // 댓글 조회 관련 메소드 (게시글 기반)
    // ===================================================

    /**
     * 게시글 ID로 댓글 목록을 조회합니다.
     *
     * @param boardId 게시글 ID
     * @param pageable 페이지 정보
     * @return 댓글 페이지
     */
    fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID로 최근 댓글 목록을 조회합니다.
     *
     * @param boardId 게시글 ID
     * @param pageable 페이지 정보
     * @return 최근 댓글 목록
     */
    fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<Comment>

    /**
     * 무한 스크롤을 위한 커서 기반 댓글 조회 메소드입니다.
     *
     * @param boardId 게시글 ID
     * @param lastCommentId 마지막으로 조회한 댓글 ID (시작점일 경우 null)
     * @param limit 조회할 댓글 수
     * @return 댓글 엔티티 목록
     */
    fun findCommentsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment>

    /**
     * 게시글의 댓글 수를 조회합니다.
     *
     * @param boardId 게시글 ID
     * @return 댓글 수
     */
    fun countByBoardId(boardId: Long): Long

    // ===================================================
    // 댓글 조회 관련 메소드 (작성자 기반)
    // ===================================================

    /**
     * 작성자로 댓글 목록을 조회합니다.
     *
     * @param author 작성자
     * @param pageable 페이지 정보
     * @return 댓글 페이지
     */
    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 목록을 조회합니다.
     *
     * @param boardId 게시글 ID
     * @param author 작성자
     * @param pageable 페이지 정보
     * @return 댓글 페이지
     */
    fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 존재 여부를 확인합니다.
     *
     * @param boardId 게시글 ID
     * @param author 작성자
     * @return 댓글 존재 여부
     */
    fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean

    /**
     * 무한 스크롤을 위한 작성자별 댓글 조회 메소드입니다.
     *
     * @param author 작성자
     * @param lastCommentId 마지막으로 조회한 댓글 ID (시작점일 경우 null)
     * @param limit 조회할 댓글 수
     * @return 댓글 엔티티 목록
     */
    fun findCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment>
}
