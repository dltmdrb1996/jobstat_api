package com.wildrew.app.community_read.client

import com.wildrew.app.community_read.BaseClient
import com.wildrew.app.community_read.client.response.FetchBoardByIdResponse
import com.wildrew.app.community_read.client.response.FetchBoardIdsResponse
import com.wildrew.app.community_read.client.response.FetchBoardsByIdsResponse
import com.wildrew.app.community_read.model.BoardReadModel
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.util.UriComponentsBuilder

@Component
class BoardClient : BaseClient() {
    @Value("\${endpoints.board-service.url:http://localhost:8081}")
    private lateinit var boardServiceUrl: String

    override fun getServiceUrl(): String = boardServiceUrl

    companion object {
        private val ID_LIST_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<FetchBoardIdsResponse>>() {}

        private val BOARD_DETAIL_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<FetchBoardByIdResponse>>() {}

        private val BOARD_BULK_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<FetchBoardsByIdsResponse>>() {}
    }

    fun fetchBoardById(
        boardId: Long,
        commentPage: Int? = null,
    ): BoardReadModel? {
        val logContext = "BoardClient.fetchBoardById"
        try {
            val typeRef = BOARD_DETAIL_RESPONSE_TYPE
            val uriBuilder = UriComponentsBuilder.fromPath("/api/v1/boards/$boardId")
            commentPage?.let { uriBuilder.queryParam("commentPage", it) }

            val responseWrapper: ApiResponse<FetchBoardByIdResponse>? =
                restClient
                    .get()
                    .uri(uriBuilder.build().toUriString())
                    .retrieve()
                    .body(typeRef)

            if (responseWrapper?.data != null) {
                return FetchBoardByIdResponse.from(responseWrapper.data!!)
            } else {
                log.warn("[{}] API call returned null data or indicated failure: {}", logContext, responseWrapper)
                return null
            }
        } catch (e: RestClientException) {
            log.error("[{}] Error during GET request: {}", logContext, e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error: {}", logContext, e.message, e)
            return null
        }
    }

    fun fetchBoardsByIds(boardIds: List<Long>): List<BoardReadModel>? {
        val logContext = "BoardClient.fetchBoardsByIds"
        if (boardIds.isEmpty()) return emptyList()

        val request = mapOf("boardIds" to boardIds)
        try {
            val typeRef = BOARD_BULK_RESPONSE_TYPE

            val responseWrapper: ApiResponse<FetchBoardsByIdsResponse>? =
                restClient
                    .post()
                    .uri("/api/v1/boards/bulk")
                    .body(request)
                    .retrieve()
                    .body(typeRef)

            if (responseWrapper?.data != null) {
                return FetchBoardsByIdsResponse.from(responseWrapper.data!!)
            } else {
                log.warn("[{}] API call returned null data or indicated failure: {}", logContext, responseWrapper)
                return null
            }
        } catch (e: RestClientException) {
            log.error("[{}] Error during POST request: {}", logContext, e.message, e)
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
        executeIdListGetRequest(
            logContext = "BoardClient.fetchLatestBoardIds",
            path = "/api/v1/boards-fetch/ids",
            queryParams = mapOf("page" to page, "size" to limit),
        )

    fun fetchLatestBoardIdsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>("limit" to limit)
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        return executeIdListGetRequest(
            logContext = "BoardClient.fetchLatestBoardIdsAfter",
            path = "/api/v1/boards-fetch/ids/after",
            queryParams = queryParams,
        )
    }

    fun fetchCategoryBoardIds(
        categoryId: Long,
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeIdListGetRequest(
            logContext = "BoardClient.fetchCategoryBoardIds",
            path = "/api/v1/boards-fetch/ids",
            queryParams =
                mapOf(
                    "categoryId" to categoryId,
                    "page" to page,
                    "size" to limit,
                ),
        )

    fun fetchCategoryBoardIdsAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? {
        val queryParams =
            mutableMapOf<String, Any?>(
                "categoryId" to categoryId,
                "limit" to limit,
            )
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        return executeIdListGetRequest(
            logContext = "BoardClient.fetchCategoryBoardIdsAfter",
            path = "/api/v1/boards-fetch/ids/after",
            queryParams = queryParams,
        )
    }

    fun fetchBoardIdsByRank(
        metric: BoardRankingMetric,
        period: String,
        page: Int,
        limit: Int,
    ): List<Long> =
        executeIdListGetRequest(
            logContext = "BoardClient.fetchBoardIdsByRank",
            path = "/api/v1/boards-fetch/ranks/${metric.name}/$period/ids",
            queryParams = mapOf("page" to page, "size" to limit),
        ) ?: emptyList()

    fun fetchBoardIdsByRankAfter(
        metric: BoardRankingMetric,
        period: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>("limit" to limit)
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId

        return executeIdListGetRequest(
            logContext = "BoardClient.fetchBoardIdsByRankAfter",
            path = "/api/v1/boards-fetch/ranks/${metric.name}/$period/ids/after",
            queryParams = queryParams,
        )
    }

    private fun executeIdListGetRequest(
        logContext: String,
        path: String,
        queryParams: Map<String, Any?>,
    ): List<Long>? {
        try {
            val typeRef = ID_LIST_RESPONSE_TYPE

            val responseWrapper: ApiResponse<FetchBoardIdsResponse>? =
                restClient
                    .get()
                    .uri(buildUri(path, queryParams))
                    .retrieve()
                    .body(typeRef)

            if (responseWrapper?.data != null) {
                return FetchBoardIdsResponse.from(responseWrapper.data!!)
            } else {
                log.warn("[{}] API call returned null data or indicated failure: {}", logContext, responseWrapper)
                return null
            }
        } catch (e: RestClientException) {
            log.error("[{}] Error during GET request: {}", logContext, e.message, e)
            return null
        } catch (e: Exception) {
            log.error("[{}] Unexpected error: {}", logContext, e.message, e)
            return null
        }
    }
}
