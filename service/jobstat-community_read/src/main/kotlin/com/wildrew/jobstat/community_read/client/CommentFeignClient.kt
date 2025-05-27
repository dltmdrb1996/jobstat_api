package com.wildrew.jobstat.community_read.client

import com.wildrew.jobstat.community_read.client.response.CommentDTO // 이전 app 모듈의 CommentDTO와 동일 가정
import com.wildrew.jobstat.community_read.client.response.GetCommentsByBoardIdAfterResponse
import com.wildrew.jobstat.community_read.client.response.GetCommentsByBoardIdResponse
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(name = "jobstat-community", contextId = "communityRead-commentFeignClient")
interface CommentFeignClient {
    @GetMapping("/api/v1/comments/{commentId}")
    fun fetchCommentById(
        @PathVariable("commentId") commentId: Long,
    ): ApiResponse<CommentDTO>?

    @PostMapping("/api/v1/comments/bulk")
    fun fetchCommentsByIds(
        @RequestBody request: Map<String, List<Long>>,
    ): ApiResponse<List<CommentDTO>>?

    @GetMapping("/api/v1/boards/{boardId}/comments")
    fun getCommentsByBoardId(
        @PathVariable("boardId") boardId: Long,
        @RequestParam("page") page: Int,
    ): ApiResponse<GetCommentsByBoardIdResponse>?

    @GetMapping("/api/v1/boards/{boardId}/comments/after")
    fun getCommentsByBoardIdAfter(
        @PathVariable("boardId") boardId: Long,
        @RequestParam("lastCommentId", required = false) lastCommentId: Long?,
        @RequestParam("limit") limit: Int,
    ): ApiResponse<GetCommentsByBoardIdAfterResponse>?
}
