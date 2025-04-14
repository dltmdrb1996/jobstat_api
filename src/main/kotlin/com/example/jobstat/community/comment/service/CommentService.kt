package com.example.jobstat.community.comment.service

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.utils.CommentConstants
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

internal interface CommentService {
    // ===================================================
    // 댓글 생성 및 수정/삭제 관련 메소드
    // ===================================================

    /**
     * 댓글을 생성합니다.
     *
     * @param boardId 게시글 ID
     * @param content 댓글 내용
     * @param author 작성자
     * @param password 비밀번호 (비회원용)
     * @param userId 사용자 ID (회원용)
     * @return 생성된 댓글 엔티티
     */
    fun createComment(
        boardId: Long,
        content: String,
        author: String,
        password: String?,
        userId: Long? = null,
    ): Comment

    /**
     * 댓글 내용을 수정합니다.
     *
     * @param id 댓글 ID
     * @param content 수정할 내용
     * @return 수정된 댓글 엔티티
     */
    fun updateComment(
        id: Long,
        content: String,
    ): Comment

    /**
     * 댓글을 삭제합니다.
     *
     * @param id 삭제할 댓글 ID
     */
    fun deleteComment(id: Long)

    // ===================================================
    // 댓글 조회 관련 메소드 (ID 기반)
    // ===================================================

    /**
     * 댓글 ID로 조회합니다.
     *
     * @param id 댓글 ID
     * @return 찾은 댓글 엔티티
     */
    fun getCommentById(id: Long): Comment

    /**
     * 여러 댓글 ID로 댓글 목록을 조회합니다.
     *
     * @param ids 댓글 ID 목록
     * @return 찾은 댓글 엔티티 목록
     */
    fun getCommentsByIds(ids: List<Long>): List<Comment>

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
    fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    /**
     * 무한 스크롤을 위한 커서 기반 게시글 댓글 조회 메소드입니다.
     *
     * @param boardId 게시글 ID
     * @param lastCommentId 마지막으로 조회한 댓글 ID (시작점일 경우 null)
     * @param limit 조회할 댓글 수
     * @return 댓글 엔티티 목록
     */
    fun getCommentsByBoardIdAfter(
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
    fun countCommentsByBoardId(boardId: Long): Long

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
    fun getCommentsByAuthor(
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 목록을 조회합니다.
     *
     * @param boardId 게시글 ID
     * @param author 작성자
     * @param pageable 페이지 정보
     * @return 댓글 페이지
     */
    fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    /**
     * 특정 게시글에 작성자가 댓글을 작성했는지 확인합니다.
     *
     * @param boardId 게시글 ID
     * @param author 작성자
     * @return 댓글 작성 여부
     */
    fun hasCommentedOnBoard(
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
    fun getCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int,
    ): List<Comment>
}
