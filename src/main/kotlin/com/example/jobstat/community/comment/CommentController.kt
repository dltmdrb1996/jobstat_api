package com.example.jobstat.community.comment

import com.example.jobstat.community.board.usecase.fetch.GetAuthorActivities
import com.example.jobstat.community.comment.usecase.CreateComment
import com.example.jobstat.community.comment.usecase.DeleteComment
import com.example.jobstat.community.comment.usecase.GetCommentById
import com.example.jobstat.community.comment.usecase.GetCommentsByAuthor
import com.example.jobstat.community.comment.usecase.GetCommentsByBoardId
import com.example.jobstat.community.comment.usecase.GetCommentsByIds
import com.example.jobstat.community.comment.usecase.NotifyCommentCreated
import com.example.jobstat.community.comment.usecase.UpdateComment
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.security.annotation.PublicWithTokenCheck
import com.example.jobstat.core.global.wrapper.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}")
@Tag(name = "댓글", description = "게시글 댓글 관리 관련 API")
internal class CommentController(
    private val createComment: CreateComment,
    private val deleteComment: DeleteComment,
    private val updateComment: UpdateComment,
    private val getAuthorActivities: GetAuthorActivities,
    private val getCommentById: GetCommentById,
    private val getCommentsByAuthor: GetCommentsByAuthor,
    private val getCommentsByBoardId: GetCommentsByBoardId,
    private val getCommentsByIds: GetCommentsByIds,
    private val notifyCommentCreated: NotifyCommentCreated,
) {
    @PublicWithTokenCheck
    @PostMapping("/boards/{boardId}/comments")
    @Operation(summary = "댓글 작성", description = "게시글에 새로운 댓글을 작성합니다.")
    fun addComment(
        @PathVariable boardId: Long,
        @RequestBody request: CreateComment.Request,
    ): ResponseEntity<ApiResponse<CreateComment.Response>> = ApiResponse.ok(createComment.invoke(request.of(boardId)))

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
    @GetMapping("/authors/{author}/activities")
    @Operation(summary = "작성자 활동 조회", description = "특정 작성자의 댓글 활동 내역을 조회합니다.")
    fun getAuthorActivities(
        @PathVariable author: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetAuthorActivities.Response>> = ApiResponse.ok(getAuthorActivities(GetAuthorActivities.Request(author, page)))
    
    @Public
    @GetMapping("/comments/{commentId}")
    @Operation(summary = "댓글 조회", description = "특정 댓글을 ID로 조회합니다.")
    fun getComment(
        @PathVariable commentId: Long,
    ): ResponseEntity<ApiResponse<GetCommentById.Response>> = ApiResponse.ok(getCommentById(GetCommentById.Request(commentId)))
    
    @Public
    @GetMapping("/authors/{author}/comments")
    @Operation(summary = "작성자별 댓글 조회", description = "특정 작성자가 작성한 댓글 목록을 조회합니다.")
    fun getCommentsByAuthor(
        @PathVariable author: String,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetCommentsByAuthor.Response>> = ApiResponse.ok(getCommentsByAuthor(GetCommentsByAuthor.Request(author, page)))
    
    @Public
    @GetMapping("/boards/{boardId}/comments")
    @Operation(summary = "게시글별 댓글 조회", description = "특정 게시글의 댓글 목록을 조회합니다.")
    fun getCommentsByBoardId(
        @PathVariable boardId: Long,
        @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetCommentsByBoardId.Response>> = ApiResponse.ok(getCommentsByBoardId(GetCommentsByBoardId.Request(boardId, page)))
    
    @Public
    @PostMapping("/comments/batch")
    @Operation(summary = "여러 댓글 조회", description = "여러 댓글을 한 번에 조회합니다.")
    fun getCommentsByIds(
        @RequestBody request: GetCommentsByIds.Request,
    ): ResponseEntity<ApiResponse<GetCommentsByIds.Response>> = ApiResponse.ok(getCommentsByIds(request))
    
    @PublicWithTokenCheck
    @PostMapping("/comments/{commentId}/notify")
    @Operation(summary = "댓글 작성 알림", description = "댓글 작성 알림을 발송합니다.")
    fun notifyCommentCreated(
        @PathVariable commentId: Long,
        @RequestBody request: NotifyCommentCreated.Request,
    ): ResponseEntity<ApiResponse<NotifyCommentCreated.Response>> = ApiResponse.ok(notifyCommentCreated(request))
}
