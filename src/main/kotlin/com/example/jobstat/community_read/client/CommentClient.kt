// file: src/main/kotlin/com/example/jobstat/community_read/client/CommentClient.kt
package com.example.jobstat.community_read.client

import com.example.jobstat.community.comment.usecase.get.GetCommentsByBoardId
import com.example.jobstat.community.comment.usecase.get.GetCommentsByBoardIdAfter
import com.example.jobstat.community_read.client.response.CommentDTO
import com.example.jobstat.community_read.client.response.FetchCommentIdsResponse
import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.core.base.BaseClient
import com.example.jobstat.core.global.wrapper.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException

/**
 * 댓글 데이터 조회를 위한 클라이언트 구현체 (API 경로 수정, Fallback 메소드 구현 방식 변경)
 */
@Component
class CommentClient : BaseClient() {
    @Value("\${endpoints.comment-service.url:http://localhost:8080}") // Command 서버 주소 (댓글 API 담당)
    private lateinit var commentServiceUrl: String

    override fun getServiceUrl(): String = commentServiceUrl

    companion object {
        private val log = LoggerFactory.getLogger(CommentClient::class.java)

        private val COMMENT_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<CommentDTO>>() {}

        // 댓글 목록(배열 or 리스트) 조회 응답 타입
        private val COMMENTS_ARRAY_RESPONSE_TYPE = // Command 서버 GetCommentsByIds가 List<CommentItem> 반환 가정 시 수정 필요
            object : ParameterizedTypeReference<ApiResponse<Array<CommentDTO>>>() {}

        // 게시글별 댓글 목록 조회 응답 타입 (Offset)
        private val COMMENTS_BY_BOARD_ID_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<GetCommentsByBoardId.Response>>() {}

        // 게시글별 댓글 목록 조회 응답 타입 (Cursor)
        private val COMMENTS_BY_BOARD_ID_AFTER_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<GetCommentsByBoardIdAfter.Response>>() {}

        // 댓글 ID 목록 Fallback 조회 시 임시로 사용할 타입 (실제 응답은 위 타입들 중 하나 사용)
        // private val COMMENT_ID_LIST_RESPONSE_TYPE = ... // 더 이상 직접 사용 안 함
    }

    fun fetchCommentById(commentId: Long): CommentReadModel? {
        val logContext = "CommentClient.fetchCommentById"
        try {
            val typeRef = COMMENT_RESPONSE_TYPE
            val uri = "/api/v1/comments/$commentId" // Command 서버 경로와 일치
            val responseWrapper: ApiResponse<CommentDTO>? =
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(typeRef)

            return if (responseWrapper?.data != null) {
                CommentDTO.from(responseWrapper.data) // CommentReadModel로 변환
            } else {
                log.warn("[{}] API call ({}) returned null data or indicated failure: {}", logContext, uri, responseWrapper)
                null
            }
        } catch (e: RestClientException) {
            log.error("[{}] Error during GET request to {}: {}", logContext, getServiceUrl() + "/api/v1/comments/$commentId", e.message)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error for commentId {}: {}", logContext, commentId, e.message, e)
            return null
        }
    }

    fun fetchCommentsByIds(commentIds: List<Long>): List<CommentReadModel>? {
        val logContext = "CommentClient.fetchCommentsByIds"
        if (commentIds.isEmpty()) return emptyList()

        val request = mapOf("commentIds" to commentIds)
        try {
            val uri = "/api/v1/comments/bulk"
            val typeRef = COMMENTS_ARRAY_RESPONSE_TYPE
            val responseWrapper: ApiResponse<Array<CommentDTO>>? =
                restClient
                    .post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(typeRef)

            return if (responseWrapper?.data != null) {
                CommentDTO.fromList(responseWrapper.data.toList()) // CommentReadModel 리스트로 변환
            } else {
                log.warn("[{}] API call ({}) returned null data or indicated failure: {}", logContext, uri, responseWrapper)
                null
            }
        } catch (e: RestClientException) {
            log.error("[{}] Error during POST request to {}: {}", logContext, getServiceUrl() + "/api/v1/comments/batch", e.message)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error for commentIds {}: {}", logContext, commentIds, e.message, e)
            return null
        }
    }

    /**
     * [Fallback] 게시글의 댓글 ID 목록 조회 (Offset 기반)
     * 기존 Command 서버 API(GET /boards/{boardId}/comments)를 사용하여 구현. (댓글 전체 내용 조회)
     */
    fun fetchCommentIdsByBoardId(
        boardId: Long,
        page: Int,
        limit: Int,
    ): FetchCommentIdsResponse? {
        val logContext = "CommentClient.fetchCommentIdsByBoardId"
        try {
            // Command 서버의 실제 응답 DTO 타입 사용
            val typeRef = COMMENTS_BY_BOARD_ID_RESPONSE_TYPE
            val uri = buildUri("/api/v1/boards/$boardId/comments", mapOf("page" to page, "size" to limit))

            log.debug("[{}] Calling Fallback API (fetches full comment data): {}", logContext, uri)

            val responseWrapper: ApiResponse<GetCommentsByBoardId.Response>? =
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(typeRef)

            return if (responseWrapper?.data != null) {
                val responseData = responseWrapper.data
                // 상세 정보에서 ID만 추출하여 FetchCommentIdsResponse 형태로 변환
                FetchCommentIdsResponse(
                    ids = responseData.items.content.map { it.id }, // CommentListItem에서 id 추출
                    hasNext = responseData.items.hasNext(),
                )
            } else {
                log.warn("[{}] Fallback API call ({}) returned null data or indicated failure: {}", logContext, uri, responseWrapper)
                null
            }
        } catch (e: RestClientException) {
            log.error("[{}] Error during Fallback GET request to {}: {}", logContext, getServiceUrl() + "/api/v1/boards/$boardId/comments", e.message)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error during Fallback GET for boardId {}: {}", logContext, boardId, e.message, e)
            return null
        }
    }

    /**
     * [Fallback] 게시글의 댓글 ID 목록 조회 (Cursor 기반)
     * 기존 Command 서버 API(GET /boards/{boardId}/comments/after)를 사용하여 구현. (댓글 전체 내용 조회)
     */
    fun fetchCommentIdsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): FetchCommentIdsResponse? {
        val logContext = "CommentClient.fetchCommentIdsByBoardIdAfter"
        val queryParams = mutableMapOf<String, Any>("limit" to limit)
        lastCommentId?.let { queryParams["lastCommentId"] = it }

        try {
            // Command 서버의 실제 응답 DTO 타입 사용
            val typeRef = COMMENTS_BY_BOARD_ID_AFTER_RESPONSE_TYPE
            val uri = buildUri("/api/v1/boards/$boardId/comments/after", queryParams)

            log.debug("[{}] Calling Fallback API (fetches full comment data): {}", logContext, uri)

            val responseWrapper: ApiResponse<GetCommentsByBoardIdAfter.Response>? =
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(typeRef)

            return if (responseWrapper?.data != null) {
                val responseData = responseWrapper.data
                // 상세 정보에서 ID만 추출하여 FetchCommentIdsResponse 형태로 변환
                FetchCommentIdsResponse(
                    ids = responseData.items.map { it.id }, // CommentItem에서 id 추출
                    hasNext = responseData.hasNext,
                )
            } else {
                log.warn("[{}] Fallback API call ({}) returned null data or indicated failure: {}", logContext, uri, responseWrapper)
                null
            }
        } catch (e: RestClientException) {
            log.error("[{}] Error during Fallback GET request to {}: {}", logContext, getServiceUrl() + "/api/v1/boards/$boardId/comments/after", e.message)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error during Fallback GET for boardId {}: {}", logContext, boardId, e.message, e)
            return null
        }
    }

    // 사용되지 않는 것으로 보이는 메소드 (주석 처리)
    // fun fetchCommentsByBoardId(...)
    // fun fetchCommentsByBoardIdAfter(...)
}
