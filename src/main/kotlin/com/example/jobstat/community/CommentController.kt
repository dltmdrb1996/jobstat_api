package com.example.jobstat.community

import com.example.jobstat.community.usecase.*
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.security.annotation.PublicWithTokenCheck
import com.example.jobstat.core.wrapper.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}")
internal class CommentController(
    private val addComment: AddComment,
    private val deleteComment: DeleteComment,
    private val updateComment: UpdateComment,
    private val getRecentComments: GetRecentComments,
    private val getAuthorActivities: GetAuthorActivities,
) {
    @PublicWithTokenCheck
    @PostMapping("/boards/{boardId}/comments")
    fun addComment(
        @PathVariable boardId: Long,
        @RequestBody request: AddComment.Request,
    ): ResponseEntity<ApiResponse<AddComment.Response>> = ApiResponse.ok(addComment.invoke(request.of(boardId)))

    @PublicWithTokenCheck
    @DeleteMapping("/boards/{boardId}/comments/{commentId}")
    fun deleteComment(
        @PathVariable boardId: Long,
        @PathVariable commentId: Long,
        @RequestBody request: DeleteComment.Request,
    ): ResponseEntity<ApiResponse<DeleteComment.Response>> = ApiResponse.ok(deleteComment(request.of(boardId, commentId)))

    @PublicWithTokenCheck
    @PutMapping("/boards/{boardId}/comments/{commentId}")
    fun updateComment(
        @PathVariable boardId: Long,
        @PathVariable commentId: Long,
        @RequestBody request: UpdateComment.Request,
    ): ResponseEntity<ApiResponse<UpdateComment.Response>> = ApiResponse.ok(updateComment(request.of(commentId)))

    @Public
    @GetMapping("/boards/{boardId}/comments/recent")
    fun getRecentComments(
        @PathVariable boardId: Long,
    ): ResponseEntity<ApiResponse<GetRecentComments.Response>> = ApiResponse.ok(getRecentComments(GetRecentComments.Request(boardId)))

    @Public
    @GetMapping("/authors/{author}/activities")
    fun getAuthorActivities(
        @PathVariable author: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetAuthorActivities.Response>> = ApiResponse.ok(getAuthorActivities(GetAuthorActivities.Request(author, page)))
}
