// file: src/main/kotlin/com/example/jobstat/community_read/client/BoardClient.kt
package com.example.jobstat.community_read.client

import com.example.jobstat.community_read.client.response.*
import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.core.base.BaseClient
import com.example.jobstat.core.global.wrapper.ApiResponse // 서버 ApiResponse Wrapper import
import com.example.jobstat.core.state.BoardRankingMetric // BoardRankingMetric import
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference // Import
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException // Import
import org.springframework.web.util.UriComponentsBuilder // UriComponentsBuilder import

/**
 * 게시판 원본 데이터를 조회하는 클라이언트 (리팩토링 및 캐싱 적용)
 * 외부 마이크로서비스와 HTTP로 통신 (읽기 전용)
 */
@Component
class BoardClient : BaseClient() {
    @Value("\${endpoints.board-service.url:http://localhost:8081}") // 포트 확인 필요
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
                return FetchBoardByIdResponse.from(responseWrapper.data)
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
                    .uri("/api/v1/boards/bulk") // 경로는 Controller와 일치
                    .body(request)
                    .retrieve()
                    .body(typeRef)

            if (responseWrapper?.data != null) {
                return FetchBoardsByIdsResponse.from(responseWrapper.data)
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

    // === ID 목록 조회 메소드들 (Controller 기준으로 수정 및 통합) ===

    // 최신순 ID 목록 (Offset)
    fun fetchLatestBoardIds(
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeIdListGetRequest(
            logContext = "BoardClient.fetchLatestBoardIds",
            path = "/api/v1/boards-fetch/ids", // Controller 경로와 일치
            queryParams = mapOf("page" to page, "size" to limit), // Controller 파라미터명과 일치
        )

    // 최신순 ID 목록 (Cursor)
    fun fetchLatestBoardIdsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>("limit" to limit)
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        return executeIdListGetRequest(
            logContext = "BoardClient.fetchLatestBoardIdsAfter",
            path = "/api/v1/boards-fetch/ids/after", // Controller 경로와 일치
            queryParams = queryParams,
        )
    }

    // 카테고리별 ID 목록 (Offset) - 수정됨
    fun fetchCategoryBoardIds(
        categoryId: Long,
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeIdListGetRequest(
            logContext = "BoardClient.fetchCategoryBoardIds",
            path = "/api/v1/boards-fetch/ids", // Controller 경로 수정됨 (Query Param 사용)
            queryParams = mapOf(
                "categoryId" to categoryId, // Query Parameter로 categoryId 추가
                "page" to page,
                "size" to limit
            ),
        )

    // 카테고리별 ID 목록 (Cursor) - 수정됨
    fun fetchCategoryBoardIdsAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>(
            "categoryId" to categoryId, // Query Parameter로 categoryId 추가
            "limit" to limit
        )
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        return executeIdListGetRequest(
            logContext = "BoardClient.fetchCategoryBoardIdsAfter",
            path = "/api/v1/boards-fetch/ids/after", // Controller 경로 수정됨 (Query Param 사용)
            queryParams = queryParams,
        )
    }

    // --- 랭킹별 ID 목록 조회 메소드 통합 ---

    // 랭킹별 ID 목록 (Offset) - 통합됨
    fun fetchBoardIdsByRank(
        metric: BoardRankingMetric,
        period: String,
        page: Int,
        limit: Int,
    ): List<Long> =
        executeIdListGetRequest(
            logContext = "BoardClient.fetchBoardIdsByRank",
            // Controller 경로에 맞게 metric 값을 Path Variable로 사용
            path = "/api/v1/boards-fetch/ranks/${metric.name}/$period/ids", // metric.name 사용 (Enum 이름을 String으로 변환)
            queryParams = mapOf("page" to page, "size" to limit),
        ) ?: emptyList()

    // 랭킹별 ID 목록 (Cursor) - 통합됨
    fun fetchBoardIdsByRankAfter(
        metric: BoardRankingMetric,
        period: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>("limit" to limit)
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        // lastScore 파라미터 추가 로직 제거됨

        return executeIdListGetRequest(
            logContext = "BoardClient.fetchBoardIdsByRankAfter",
            // Controller 경로에 맞게 metric 값을 Path Variable로 사용
            path = "/api/v1/boards-fetch/ranks/${metric.name}/$period/ids/after", // metric.name 사용
            queryParams = queryParams,
        )
    }

    /**
     * ID 목록 조회 GET 요청을 위한 공통 헬퍼 메소드
     * 참고: FetchBoardIdsResponse.from 에서 String ID 목록을 Long 목록으로 변환합니다.
     * 만약 내부 모델에서도 String ID를 사용해야 한다면 해당 from 메소드 수정 필요.
     */
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
                    .uri(buildUri(path, queryParams)) // buildUri 사용 (Query Parameter 처리)
                    .retrieve()
                    .body(typeRef)

            if (responseWrapper?.data != null) {
                return FetchBoardIdsResponse.from(responseWrapper.data)
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