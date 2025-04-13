package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.CommentReadModel
import org.springframework.data.redis.connection.StringRedisConnection

/**
 * 댓글 상세 정보 저장소 인터페이스
 */
interface CommentDetailRepository {
    /**
     * 댓글 상세 정보 조회
     */
    fun findCommentDetail(commentId: Long): CommentReadModel?
    
    /**
     * 여러 댓글 상세 정보 조회
     */
    fun findCommentDetails(commentIds: List<Long>): Map<Long, CommentReadModel>
    
    /**
     * 댓글 상세 정보 저장
     */
    fun saveCommentDetail(comment: CommentReadModel, eventTs: Long)
    
    /**
     * 여러 댓글 상세 정보 저장
     */
    fun saveCommentDetails(comments: List<CommentReadModel>, eventTs: Long)
    
    /**
     * 파이프라인에서 댓글 상세 정보 저장
     */
    fun saveCommentDetailInPipeline(conn: StringRedisConnection, comment: CommentReadModel)
    
}