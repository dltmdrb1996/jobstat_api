package com.example.jobstat.community

import com.example.jobstat.community.usecase.*
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.security.annotation.PublicWithTokenCheck
import com.example.jobstat.core.wrapper.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}")
@Tag(name = "댓글", description = "게시글 댓글 관리 관련 API")
internal class CommentController(
    private val addComment: AddComment,
    private val deleteComment: DeleteComment,
    private val updateComment: UpdateComment,
    private val getRecentComments: GetRecentComments,
    private val getAuthorActivities: GetAuthorActivities,
) {
    @PublicWithTokenCheck
    @PostMapping("/boards/{boardId}/comments")
    @Operation(summary = "댓글 작성", description = "게시글에 새로운 댓글을 작성합니다.")
    fun addComment(
        @PathVariable boardId: Long,
        @RequestBody request: AddComment.Request,
    ): ResponseEntity<ApiResponse<AddComment.Response>> = ApiResponse.ok(addComment.invoke(request.of(boardId)))

    @PublicWithTokenCheck
    @DeleteMapping("/boards/{boardId}/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "작성된 댓글을 삭제합니다.")
    fun deleteComment(
        @PathVariable boardId: Long,
        @PathVariable commentId: Long,
        @RequestBody request: DeleteComment.Request,
    ): ResponseEntity<ApiResponse<DeleteComment.Response>> = ApiResponse.ok(deleteComment(request.of(boardId, commentId)))

    @PublicWithTokenCheck
    @PutMapping("/boards/{boardId}/comments/{commentId}")
    @Operation(summary = "댓글 수정", description = "작성된 댓글을 수정합니다.")
    fun updateComment(
        @PathVariable boardId: Long,
        @PathVariable commentId: Long,
        @RequestBody request: UpdateComment.Request,
    ): ResponseEntity<ApiResponse<UpdateComment.Response>> = ApiResponse.ok(updateComment(request.of(commentId)))

    @Public
    @GetMapping("/boards/{boardId}/comments/recent")
    @Operation(summary = "최근 댓글 조회", description = "게시글의 최근 댓글 목록을 조회합니다.")
    fun getRecentComments(
        @PathVariable boardId: Long,
    ): ResponseEntity<ApiResponse<GetRecentComments.Response>> = ApiResponse.ok(getRecentComments(GetRecentComments.Request(boardId)))

    @Public
    @GetMapping("/authors/{author}/activities")
    @Operation(summary = "작성자 활동 조회", description = "특정 작성자의 댓글 활동 내역을 조회합니다.")
    fun getAuthorActivities(
        @PathVariable author: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetAuthorActivities.Response>> = ApiResponse.ok(getAuthorActivities(GetAuthorActivities.Request(author, page)))
}
