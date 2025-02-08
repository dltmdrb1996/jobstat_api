package com.example.jobstat.board

import com.example.jobstat.board.usecase.*
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
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
    @PostMapping("/boards/{boardId}/comments")
    fun addComment(
        @PathVariable boardId: Long,
        @RequestBody request: AddComment.Request,
    ): ResponseEntity<ApiResponse<AddComment.Response>> = ApiResponse.ok(addComment.invoke(request))

    @DeleteMapping("/boards/{boardId}/comments/{commentId}")
    fun deleteComment(
        @PathVariable boardId: Long,
        @PathVariable commentId: Long,
        @RequestParam password: String,
    ): ResponseEntity<ApiResponse<DeleteComment.Response>> = ApiResponse.ok(deleteComment(DeleteComment.Request(commentId, password)))

    @PutMapping("/boards/{boardId}/comments/{commentId}")
    fun updateComment(
        @PathVariable boardId: Long,
        @PathVariable commentId: Long,
        @RequestBody request: UpdateComment.Request,
    ): ResponseEntity<ApiResponse<UpdateComment.Response>> = ApiResponse.ok(updateComment(request))

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
