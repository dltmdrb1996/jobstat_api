package com.example.jobstat.community.comment.repository

import com.example.jobstat.community.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

internal interface CommentRepository {
    /**
     * 페이징된 모든 댓글 조회
     */
    fun findAll(pageable: Pageable): Page<Comment>

    /**
     * 댓글 저장
     */
    fun save(comment: Comment): Comment

    /**
     * 댓글 ID로 조회
     */
    fun findById(id: Long): Comment

    /**
     * 게시글 ID로 댓글 목록 조회
     */
    fun findByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID로 최근 댓글 목록 조회
     */
    fun findRecentComments(
        boardId: Long,
        pageable: Pageable,
    ): List<Comment>

    /**
     * 댓글 ID로 삭제
     */
    fun deleteById(id: Long)

    /**
     * 게시글 ID로 댓글 수 조회
     */
    fun countByBoardId(boardId: Long): Long

    /**
     * 작성자로 댓글 목록 조회
     */
    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 목록 조회
     */
    fun findByBoardIdAndAuthor(
        boardId: Long,
        author: String,
        pageable: Pageable,
    ): Page<Comment>

    /**
     * 게시글 ID와 작성자로 댓글 존재 여부 확인
     */
    fun existsByBoardIdAndAuthor(
        boardId: Long,
        author: String,
    ): Boolean

    /**
     * 게시글 ID로 모든 댓글 삭제
     */
    fun deleteByBoardId(boardId: Long)

    /**
     * 여러 댓글 ID로 댓글 목록 조회
     */
    fun findAllByIds(ids: List<Long>): List<Comment>
    
    /**
     * 무한 스크롤을 위한 메서드 - 게시글 ID와 마지막 댓글 ID로 다음 댓글 목록 조회
     */
    fun findCommentsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int
    ): List<Comment>
    
    /**
     * 무한 스크롤을 위한 메서드 - 작성자와 마지막 댓글 ID로 다음 댓글 목록 조회
     */
    fun findCommentsByAuthorAfter(
        author: String,
        lastCommentId: Long?,
        limit: Int
    ): List<Comment>
}
