package com.example.jobstat.community_read.repository

import org.springframework.data.redis.connection.StringRedisConnection

/**
 * 댓글 카운트 저장소 인터페이스
 */
interface CommentCountRepository {
    /**
     * 게시글별 댓글 총 개수 조회
     */
    fun getCommentCountByBoardId(boardId: Long): Long
    
    /**
     * 전체 댓글 개수 조회
     */
    fun getTotalCount(): Long
    
    /**
     * 파이프라인에서 게시글별 댓글 카운트 증감
     */
    fun applyBoardCommentCountInPipeline(conn: StringRedisConnection, boardId: Long, delta: Long)
    
    /**
     * 파이프라인에서 전체 댓글 카운트 증감
     */
    fun applyTotalCountInPipeline(conn: StringRedisConnection, delta: Long)
} 