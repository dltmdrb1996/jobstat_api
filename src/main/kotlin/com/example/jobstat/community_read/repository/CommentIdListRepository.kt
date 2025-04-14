package com.example.jobstat.community_read.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection

/**
 * 댓글 ID 리스트 저장소 인터페이스
 */
interface CommentIdListRepository {

    /**
     * 게시글별 댓글 ID 리스트 조회
     */
    fun readAllByBoard(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long>

    /**
     * 무한 스크롤 방식의 게시글별 댓글 ID 리스트 조회
     */
    fun readAllByBoardInfiniteScroll(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long>

    /**
     * 게시글별 댓글 ID 목록 조회 (Pageable 기반)
     */
    fun readCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long>

    /**
     * 게시글별 댓글 ID 목록 조회 (커서 기반)
     */
    fun readCommentsByBoardIdByCursor(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long>

    /**
     * 게시글별 댓글 목록 키 반환
     */
    fun getBoardCommentsKey(boardId: Long): String

    // 수정 관련 메소드

    /**
     * 게시글별 댓글 ID 리스트에 추가
     */
    fun add(
        boardId: Long,
        commentId: Long,
        sortValue: Double,
    )

    /**
     * 게시글별 댓글 ID 리스트에서 삭제
     */
    fun delete(
        boardId: Long,
        commentId: Long,
    )

    // 파이프라인 관련 메소드

    /**
     * 댓글 추가 (파이프라인 사용)
     */
    fun addCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
        score: Double,
    )

    /**
     * 댓글 삭제 (파이프라인 사용)
     */
    fun removeCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
    )
}
