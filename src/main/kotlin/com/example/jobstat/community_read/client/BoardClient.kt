package com.example.jobstat.community_read.client

import com.example.jobstat.community_read.client.response.*
import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.core.base.BaseClient
import com.example.jobstat.core.global.wrapper.ApiResponse // 서버 ApiResponse Wrapper import
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

    // --- ParameterizedTypeReference 캐싱 ---
    companion object {
        private val ID_LIST_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<FetchBoardIdsResponse>>() {}

        private val BOARD_DETAIL_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<FetchBoardByIdResponse>>() {}

        private val BOARD_BULK_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<FetchBoardsByIdsResponse>>() {}
    }
    // --- 캐싱 끝 ---

    // commentPage 파라미터 추가
    fun fetchBoardById(
        boardId: Long,
        commentPage: Int? = null,
    ): BoardReadModel? {
        val logContext = "BoardGetClient.getBoardById"
        try {
            // 캐싱된 타입 참조 사용
            val typeRef = BOARD_DETAIL_RESPONSE_TYPE

            // URI 빌더 생성
            val uriBuilder = UriComponentsBuilder.fromPath("/api/v1/boards/$boardId")

            // commentPage 파라미터가 있으면 추가
            commentPage?.let { uriBuilder.queryParam("commentPage", it) }

            // restClient 직접 사용
            val responseWrapper: ApiResponse<FetchBoardByIdResponse>? =
                restClient
                    .get()
                    .uri(uriBuilder.build().toUriString()) // 빌드된 URI 사용
                    .retrieve()
                    .body(typeRef)

            // 결과 처리
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
        val logContext = "BoardGetClient.getBoardsByIds"
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

            // 결과 처리
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

    // === ID 목록 조회 메소드들 (동일 패턴 적용) ===
    // 참고: Controller의 `/boards`, `/authors/{author}/boards/stats`, `/boards-fetch`, `/boards-fetch/infinite-scroll` 등의 API는
    // 현재 BoardClient에 직접 호출하는 메소드가 구현되어 있지 않습니다. 필요시 추가 구현이 필요합니다.

    // 참고: 아래 ID 목록 조회 메소드들은 Controller에서 제공하는 categoryId, author 필터를 사용하지 않습니다. 필요시 파라미터 추가가 필요합니다.
    fun fetchLatestBoardIds(
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeIdListGetRequest(
            logContext = "BoardGetClient.fetchLatestBoardIds",
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
            logContext = "BoardGetClient.fetchLatestBoardIdsAfter",
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
            logContext = "BoardGetClient.fetchCategoryBoardIds",
            path = "/api/v1/boards-fetch/category/$categoryId/ids",
            queryParams = mapOf("page" to page, "size" to limit),
        )

    fun fetchCategoryBoardIdsAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>("limit" to limit)
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        return executeIdListGetRequest(
            logContext = "BoardGetClient.fetchCategoryBoardIdsAfter",
            path = "/api/v1/boards-fetch/category/$categoryId/ids/after",
            queryParams = queryParams,
        )
    }

    fun fetchBoardIdsByLikes(
        period: String,
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeIdListGetRequest(
            logContext = "BoardGetClient.fetchBoardIdsByLikes",
            path = "/api/v1/boards-fetch/ranks/likes/$period/ids",
            queryParams = mapOf("page" to page, "size" to limit),
        )

    fun fetchBoardIdsByLikesAfter(
        period: String,
        lastBoardId: Long?,
        lastScore: Double?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>("limit" to limit)
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        if (lastScore != null) queryParams["lastScore"] = lastScore
        return executeIdListGetRequest(
            logContext = "BoardGetClient.fetchBoardIdsByLikesAfter",
            path = "/api/v1/boards-fetch/ranks/likes/$period/ids/after",
            queryParams = queryParams,
        )
    }

    fun fetchBoardIdsByViews(
        period: String,
        page: Int,
        limit: Int,
    ): List<Long>? =
        executeIdListGetRequest(
            logContext = "BoardGetClient.fetchBoardIdsByViews",
            path = "/api/v1/boards-fetch/ranks/views/$period/ids",
            queryParams = mapOf("page" to page, "size" to limit),
        )

    fun fetchBoardIdsByViewsAfter(
        period: String,
        lastBoardId: Long?,
        lastScore: Double?,
        limit: Int,
    ): List<Long>? {
        val queryParams = mutableMapOf<String, Any?>("limit" to limit)
        if (lastBoardId != null) queryParams["lastBoardId"] = lastBoardId
        if (lastScore != null) queryParams["lastScore"] = lastScore
        return executeIdListGetRequest(
            logContext = "BoardGetClient.fetchBoardIdsByViewsAfter",
            path = "/api/v1/boards-fetch/ranks/views/$period/ids/after",
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
            // 캐싱된 ID 목록 응답 타입 참조 사용
            val typeRef = ID_LIST_RESPONSE_TYPE

            val responseWrapper: ApiResponse<FetchBoardIdsResponse>? =
                restClient
                    .get()
                    .uri(buildUri(path, queryParams)) // buildUri 사용
                    .retrieve()
                    .body(typeRef)

            // 결과 처리
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
