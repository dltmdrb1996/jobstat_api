package com.wildrew.jobstat.community_read.client

import com.wildrew.jobstat.community_read.client.response.FetchBoardByIdResponse
import com.wildrew.jobstat.community_read.client.response.FetchBoardIdsResponse
import com.wildrew.jobstat.community_read.client.response.FetchBoardsByIdsResponse
import com.wildrew.jobstat.community_read.model.BoardReadModel
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import feign.FeignException // Import FeignException for more specific error handling
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BoardClientDecorator(
    private val boardFeignClient: BoardFeignClient,
) {
    private val log = LoggerFactory.getLogger(BoardClientDecorator::class.java)

    fun fetchBoardById(
        boardId: Long,
        commentPage: Int? = null,
    ): BoardReadModel? {
        val logContext = "BoardClientDecorator.fetchBoardById"
        try {
            val responseWrapper = boardFeignClient.fetchBoardById(boardId, commentPage)

            if (responseWrapper?.data != null) {
                return FetchBoardByIdResponse.from(responseWrapper.data!!)
            } else {
                log.warn("[{}] Feign API call returned null data or indicated failure: {}", logContext, responseWrapper)
                return null
            }
        } catch (e: FeignException) {
            log.error("[{}] Error during Feign GET request: status={}, message={}", logContext, e.status(), e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error: {}", logContext, e.message, e)
            return null
        }
    }

    fun fetchBoardsByIds(boardIds: List<Long>): List<BoardReadModel>? {
        val logContext = "BoardClientDecorator.fetchBoardsByIds"
        if (boardIds.isEmpty()) return emptyList()

        val request = mapOf("boardIds" to boardIds)
        try {
            val responseWrapper = boardFeignClient.fetchBoardsByIds(request)

            if (responseWrapper?.data != null) {
                return FetchBoardsByIdsResponse.from(responseWrapper.data!!)
            } else {
                log.warn("[{}] Feign API call returned null data or indicated failure: {}", logContext, responseWrapper)
                return null
            }
        } catch (e: FeignException) {
            log.error("[{}] Error during Feign POST request: status={}, message={}", logContext, e.status(), e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error: {}", logContext, e.message, e)
            return null
        }
    }

    fun fetchLatestBoardIds(
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeFeignIdListGetRequest(
            logContext = "BoardClientDecorator.fetchLatestBoardIds",
            feignCall = { boardFeignClient.fetchLatestBoardIds(page, limit) },
        )

    fun fetchLatestBoardIdsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? =
        executeFeignIdListGetRequest(
            logContext = "BoardClientDecorator.fetchLatestBoardIdsAfter",
            feignCall = { boardFeignClient.fetchLatestBoardIdsAfter(lastBoardId, limit) },
        )

    fun fetchCategoryBoardIds(
        categoryId: Long,
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeFeignIdListGetRequest(
            logContext = "BoardClientDecorator.fetchCategoryBoardIds",
            feignCall = { boardFeignClient.fetchCategoryBoardIds(categoryId, page, limit) },
        )

    fun fetchCategoryBoardIdsAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? =
        executeFeignIdListGetRequest(
            logContext = "BoardClientDecorator.fetchCategoryBoardIdsAfter",
            feignCall = { boardFeignClient.fetchCategoryBoardIdsAfter(categoryId, lastBoardId, limit) },
        )

    fun fetchBoardIdsByRank(
        metric: BoardRankingMetric,
        period: String,
        page: Int,
        limit: Int,
    ): List<Long> =
        // Note: Original returns non-nullable List<Long>
        executeFeignIdListGetRequest(
            logContext = "BoardClientDecorator.fetchBoardIdsByRank",
            feignCall = { boardFeignClient.fetchBoardIdsByRank(metric.name, period, page, limit) },
        ) ?: emptyList() // Ensure non-nullable return, defaulting to emptyList

    fun fetchBoardIdsByRankAfter(
        metric: BoardRankingMetric,
        period: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? =
        executeFeignIdListGetRequest(
            logContext = "BoardClientDecorator.fetchBoardIdsByRankAfter",
            feignCall = { boardFeignClient.fetchBoardIdsByRankAfter(metric.name, period, lastBoardId, limit) },
        )

    private fun executeFeignIdListGetRequest(
        logContext: String,
        feignCall: () -> ApiResponse<FetchBoardIdsResponse>?,
    ): List<Long>? {
        try {
            val responseWrapper = feignCall.invoke()

            if (responseWrapper?.data != null) {
                return FetchBoardIdsResponse.from(responseWrapper.data!!)
            } else {
                log.warn("[{}] Feign API call returned null data or indicated failure: {}", logContext, responseWrapper)
                return null
            }
        } catch (e: FeignException) {
            log.error("[{}] Error during Feign GET request: status={}, message={}", logContext, e.status(), e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error: {}", logContext, e.message, e)
            return null
        }
    }
}
