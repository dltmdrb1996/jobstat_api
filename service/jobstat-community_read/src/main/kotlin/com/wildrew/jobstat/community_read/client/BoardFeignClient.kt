package com.wildrew.jobstat.community_read.client

import com.wildrew.jobstat.community_read.client.response.FetchBoardByIdResponse
import com.wildrew.jobstat.community_read.client.response.FetchBoardIdsResponse
import com.wildrew.jobstat.community_read.client.response.FetchBoardsByIdsResponse
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(name = "jobstat-community", contextId = "communityRead-boardFeignClient")
interface BoardFeignClient {
    @GetMapping("/api/v1/boards/{boardId}")
    fun fetchBoardById(
        @PathVariable("boardId") boardId: Long,
        @RequestParam("commentPage", required = false) commentPage: Int? = null,
    ): ApiResponse<FetchBoardByIdResponse>?

    @PostMapping("/api/v1/boards/bulk")
    fun fetchBoardsByIds(
        @RequestBody request: Map<String, List<Long>>,
    ): ApiResponse<FetchBoardsByIdsResponse>?

    @GetMapping("/api/v1/boards-fetch/ids")
    fun fetchLatestBoardIds(
        @RequestParam("page") page: Int,
        @RequestParam("size") limit: Int,
    ): ApiResponse<FetchBoardIdsResponse>?

    @GetMapping("/api/v1/boards-fetch/ids/after")
    fun fetchLatestBoardIdsAfter(
        @RequestParam("lastBoardId", required = false) lastBoardId: Long?,
        @RequestParam("limit") limit: Int,
    ): ApiResponse<FetchBoardIdsResponse>?

    @GetMapping("/api/v1/boards-fetch/ids")
    fun fetchCategoryBoardIds(
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("page") page: Int,
        @RequestParam("size") limit: Int,
    ): ApiResponse<FetchBoardIdsResponse>?

    @GetMapping("/api/v1/boards-fetch/ids/after")
    fun fetchCategoryBoardIdsAfter(
        @RequestParam("categoryId") categoryId: Long,
        @RequestParam("lastBoardId", required = false) lastBoardId: Long?,
        @RequestParam("limit") limit: Int,
    ): ApiResponse<FetchBoardIdsResponse>?

    @GetMapping("/api/v1/boards-fetch/ranks/{metric}/{period}/ids")
    fun fetchBoardIdsByRank(
        @PathVariable("metric") metric: String,
        @PathVariable("period") period: String,
        @RequestParam("page") page: Int,
        @RequestParam("size") limit: Int,
    ): ApiResponse<FetchBoardIdsResponse>?

    @GetMapping("/api/v1/boards-fetch/ranks/{metric}/{period}/ids/after")
    fun fetchBoardIdsByRankAfter(
        @PathVariable("metric") metric: String,
        @PathVariable("period") period: String,
        @RequestParam("lastBoardId", required = false) lastBoardId: Long?,
        @RequestParam("limit") limit: Int,
    ): ApiResponse<FetchBoardIdsResponse>?
}
