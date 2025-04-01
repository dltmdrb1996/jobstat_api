package com.example.jobstat.community_read.client

import com.example.jobstat.community_read.client.response.CommentResponseDto
import com.example.jobstat.community_read.client.response.CommentListResponseDto
import com.example.jobstat.community_read.client.response.RecentCommentResponseDto
import com.example.jobstat.community_read.client.response.BulkCommentsResponseDto
import com.example.jobstat.core.base.BaseClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 댓글 원본 데이터를 조회하는 클라이언트
 * 외부 마이크로서비스와 HTTP로 통신
 */
@Component
class CommentClient : BaseClient() {
    
    @Value("\${endpoints.comment-service.url:http://localhost:8080}")
    private lateinit var commentServiceUrl: String
    
    override fun getServiceUrl(): String = commentServiceUrl

    /**
     * 댓글 ID로 댓글 조회
     */
    fun getComment(commentId: Long): CommentResponseDto? {
        return executeGet(
            uri = "/api/v1/comments/$commentId",
            responseType = CommentResponseDto::class.java,
            logContext = "CommentClient.getComment"
        )
    }

    /**
     * 게시글별 댓글 조회
     */
    fun getCommentsByBoardId(boardId: Long, page: Int? = null): CommentListResponseDto? {
        val queryParams = if (page != null) {
            mapOf("page" to page)
        } else {
            emptyMap()
        }
        
        val uri = buildUri("/api/v1/boards/$boardId/comments", queryParams)
        
        return executeGet(
            uri = uri,
            responseType = CommentListResponseDto::class.java,
            logContext = "CommentClient.getCommentsByBoardId"
        )
    }

    /**
     * 작성자별 댓글 조회
     */
    fun getCommentsByAuthor(author: String, page: Int? = null): CommentListResponseDto? {
        val queryParams = if (page != null) {
            mapOf("page" to page)
        } else {
            emptyMap()
        }
        
        val uri = buildUri("/api/v1/authors/$author/comments", queryParams)
        
        return executeGet(
            uri = uri,
            responseType = CommentListResponseDto::class.java,
            logContext = "CommentClient.getCommentsByAuthor"
        )
    }

    /**
     * 게시글별 댓글 수 조회 (현재 API에 구현되어 있지 않음 - 추가 필요)
     */
    fun getCommentCountByBoardId(boardId: Long): Int {
        // 실제 API가 없으므로 게시글별 댓글 목록을 가져와서 totalCount를 반환하도록 수정
        val commentList = getCommentsByBoardId(boardId)
        return commentList?.totalCount?.toInt() ?: 0
    }

    /**
     * 작성자별 댓글 수 조회 (현재 API에 구현되어 있지 않음 - 추가 필요)
     */
    fun getCommentCountByAuthor(author: String): Int {
        // 실제 API가 없으므로 작성자별 댓글 목록을 가져와서 totalCount를 반환하도록 수정
        val commentList = getCommentsByAuthor(author)
        return commentList?.totalCount?.toInt() ?: 0
    }

    /**
     * 최근 댓글 조회 (현재 API에 구현되어 있지 않음 - 추가 필요)
     * 임시로 게시글별 댓글 목록을 가져와서 처리
     */
    fun getRecentComments(boardId: Long): List<RecentCommentResponseDto>? {
        val commentList = getCommentsByBoardId(boardId)
        
        return commentList?.items?.map { 
            RecentCommentResponseDto(
                id = it.id,
                content = it.content,
                author = it.author,
                createdAt = it.createdAt
            )
        }
    }
    /**
     * 여러 댓글 ID로 댓글 목록 한번에 조회
     */
    fun getCommentsByIds(commentIds: List<Long>): BulkCommentsResponseDto? {
        val request = mapOf("commentIds" to commentIds)
        
        return executePost(
            uri = "/api/v1/comments/batch",
            body = request,
            responseType = BulkCommentsResponseDto::class.java,
            logContext = "CommentClient.getCommentsByIds"
        )
    }
} 