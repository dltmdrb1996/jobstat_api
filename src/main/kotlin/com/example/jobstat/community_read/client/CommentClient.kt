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

@Component
class CommentClient : BaseClient() {
    @Value("\${endpoints.comment-service.url:http://localhost:8080}")
    private lateinit var commentServiceUrl: String

    override fun getServiceUrl(): String = commentServiceUrl

    companion object {
        private val log = LoggerFactory.getLogger(CommentClient::class.java)

        private val COMMENT_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<CommentDTO>>() {}

        private val COMMENTS_ARRAY_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Array<CommentDTO>>>() {}

        private val COMMENTS_BY_BOARD_ID_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<GetCommentsByBoardId.Response>>() {}

        private val COMMENTS_BY_BOARD_ID_AFTER_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<GetCommentsByBoardIdAfter.Response>>() {}
    }

    fun fetchCommentById(commentId: Long): CommentReadModel? {
        val logContext = "CommentClient.fetchCommentById"
        try {
            val typeRef = COMMENT_RESPONSE_TYPE
            val uri = "/api/v1/comments/$commentId"
            val responseWrapper: ApiResponse<CommentDTO>? =
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(typeRef)

            return if (responseWrapper?.data != null) {
                CommentDTO.from(responseWrapper.data)
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
                CommentDTO.fromList(responseWrapper.data.toList())
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

    fun fetchCommentIdsByBoardId(
        boardId: Long,
        page: Int,
        limit: Int,
    ): FetchCommentIdsResponse? {
        val logContext = "CommentClient.fetchCommentIdsByBoardId"
        try {
            val typeRef = COMMENTS_BY_BOARD_ID_RESPONSE_TYPE
            val uri = buildUri("/api/v1/boards/$boardId/comments", mapOf("page" to page))

            log.debug("[{}] Calling Fallback API (fetches full comment data): {}", logContext, uri)

            val responseWrapper: ApiResponse<GetCommentsByBoardId.Response>? =
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(typeRef)

            return if (responseWrapper?.data != null) {
                val responseData = responseWrapper.data
                FetchCommentIdsResponse(
                    ids = responseData.items.content.map { it.id },
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

    fun fetchCommentIdsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): FetchCommentIdsResponse? {
        val logContext = "CommentClient.fetchCommentIdsByBoardIdAfter"
        val queryParams = mutableMapOf<String, Any>("limit" to limit)
        lastCommentId?.let { queryParams["lastCommentId"] = it }

        try {
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
}
