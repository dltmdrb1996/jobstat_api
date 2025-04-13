package com.example.jobstat.community.comment.service

import com.example.jobstat.community.comment.utils.CommentConstants
import com.example.jobstat.community.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

internal interface CommentService {
    /**
     * 댓글 생성
     */
    fun createComment(
        boardId: Long,
        content: String,
        author: String,
        password: String?,
        userId: Long? = null,
    ): Comment

    /**
     * 댓글 ID로 조회
     */
    fun getCommentById(id: Long): Comment

    /**
     * 게시글 ID로 댓글 목록 조회 (페이지네이션)
     */
    fun getCommentsByBoardId(
        boardId: Long,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    /**
     * 작성자로 댓글 목록 조회 (페이지네이션)
     */
    fun getCommentsByAuthor(
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 목록 조회 (페이지네이션)
     */
    fun getCommentsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable = Pageable.ofSize(CommentConstants.DEFAULT_PAGE_SIZE),
    ): Page<Comment>

    /**
     * 댓글 내용 수정
     */
    fun updateComment(
        id: Long,
        content: String,
    ): Comment

    /**
     * 댓글 삭제
     */
    fun deleteComment(id: Long)

    /**
     * 게시글의 댓글 수 조회
     */
    fun countCommentsByBoardId(boardId: Long): Long

    /**
     * 특정 게시글에 작성자가 댓글을 작성했는지 확인
     */
    fun hasCommentedOnBoard(
        boardId: Long,
        author: String,
    ): Boolean

    /**
     * 여러 댓글 ID로 조회
     */
    fun getCommentsByIds(ids: List<Long>): List<Comment>
    
    /**
     * 무한 스크롤을 위한 메서드 - 커서 기반 페이지네이션
     */
    fun getCommentsByBoardIdAfter(boardId: Long, lastCommentId: Long?, limit: Int): List<Comment>
    
    /**
     * 무한 스크롤을 위한 메서드 - 작성자별 댓글 조회
     */
    fun getCommentsByAuthorAfter(author: String, lastCommentId: Long?, limit: Int): List<Comment>
}
