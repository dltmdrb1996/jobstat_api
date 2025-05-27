package com.wildrew.jobstat.community_read.client

import com.wildrew.jobstat.community_read.client.response.CommentDTO
import com.wildrew.jobstat.community_read.client.response.FetchCommentIdsResponse // Assuming this is the target type
import com.wildrew.jobstat.community_read.client.response.GetCommentsByBoardIdAfterResponse
import com.wildrew.jobstat.community_read.client.response.GetCommentsByBoardIdResponse
import com.wildrew.jobstat.community_read.model.CommentReadModel
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CommentClientDecorator(
    private val commentFeignClient: CommentFeignClient,
) {
    private val log = LoggerFactory.getLogger(CommentClientDecorator::class.java)

    fun fetchCommentById(commentId: Long): CommentReadModel? {
        val logContext = "CommentClientDecorator.fetchCommentById"
        try {
            val responseWrapper: ApiResponse<CommentDTO>? = commentFeignClient.fetchCommentById(commentId)

            return if (responseWrapper?.data != null) {
                CommentDTO.from(responseWrapper.data!!)
            } else {
                log.warn("[{}] Feign API call returned null data or indicated failure: {}", logContext, responseWrapper)
                null
            }
        } catch (e: FeignException) {
            log.error("[{}] Error during Feign GET request: status={}, message={}", logContext, e.status(), e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error for commentId {}: {}", logContext, commentId, e.message, e)
            return null
        }
    }

    fun fetchCommentsByIds(commentIds: List<Long>): List<CommentReadModel>? {
        val logContext = "CommentClientDecorator.fetchCommentsByIds"
        if (commentIds.isEmpty()) return emptyList()

        val request = mapOf("commentIds" to commentIds)
        try {
            val responseWrapper: ApiResponse<List<CommentDTO>>? = commentFeignClient.fetchCommentsByIds(request)

            return if (responseWrapper?.data != null) {
                CommentDTO.fromList(responseWrapper.data!!)
            } else {
                log.warn("[{}] Feign API call returned null data or indicated failure: {}", logContext, responseWrapper)
                null
            }
        } catch (e: FeignException) {
            log.error("[{}] Error during Feign POST request: status={}, message={}", logContext, e.status(), e.message, e)
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
        val logContext = "CommentClientDecorator.fetchCommentIdsByBoardId"
        try {
            val responseWrapper: ApiResponse<GetCommentsByBoardIdResponse>? =
                commentFeignClient.getCommentsByBoardId(boardId, page)

            return if (responseWrapper?.data != null) {
                responseWrapper.data?.let {
                    FetchCommentIdsResponse(
                        ids = it.items.content.map { comment -> comment.id },
                        hasNext = it.items.hasNext(),
                    )
                }
            } else {
                log.warn("[{}] Feign API call returned null data or indicated failure: {}", logContext, responseWrapper)
                null
            }
        } catch (e: FeignException) {
            log.error("[{}] Error during Feign GET request: status={}, message={}", logContext, e.status(), e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error during GET for boardId {}: {}", logContext, boardId, e.message, e)
            return null
        }
    }

    fun fetchCommentIdsByBoardIdAfter(
        boardId: Long,
        lastCommentId: Long?,
        limit: Int,
    ): FetchCommentIdsResponse? {
        val logContext = "CommentClientDecorator.fetchCommentIdsByBoardIdAfter"
        try {
            val responseWrapper: ApiResponse<GetCommentsByBoardIdAfterResponse>? =
                commentFeignClient.getCommentsByBoardIdAfter(boardId, lastCommentId, limit)

            return if (responseWrapper?.data != null) {
                responseWrapper.data?.let {
                    FetchCommentIdsResponse(
                        ids = it.items.map { item -> item.id },
                        hasNext = it.hasNext,
                    )
                }
            } else {
                log.warn("[{}] Feign API call returned null data or indicated failure: {}", logContext, responseWrapper)
                null
            }
        } catch (e: FeignException) {
            log.error("[{}] Error during Feign GET request: status={}, message={}", logContext, e.status(), e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error during GET for boardId {}: {}", logContext, boardId, e.message, e)
            return null
        }
    }
}
