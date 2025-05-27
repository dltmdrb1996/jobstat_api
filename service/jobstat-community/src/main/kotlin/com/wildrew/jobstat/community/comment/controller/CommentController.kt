package com.wildrew.jobstat.community.comment.controller

import com.wildrew.jobstat.community.board.usecase.get.GetAuthorActivities
import com.wildrew.jobstat.community.comment.usecase.command.CreateComment
import com.wildrew.jobstat.community.comment.usecase.command.DeleteComment
import com.wildrew.jobstat.community.comment.usecase.command.UpdateComment
import com.wildrew.jobstat.community.comment.usecase.get.*
import com.wildrew.jobstat.core.core_security.annotation.Public
import com.wildrew.jobstat.core.core_security.annotation.PublicWithTokenCheck
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import com.wildrew.jobstat.core.core_web_util.RestConstants
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}")
@Tag(name = "댓글", description = "게시글 댓글 관리 관련 API")
class CommentController(
    private val createComment: CreateComment,
    private val deleteComment: DeleteComment,
    private val updateComment: UpdateComment,
    private val getAuthorActivities: GetAuthorActivities,
    private val getCommentDetail: GetCommentDetail,
    private val getCommentsByAuthor: GetCommentsByAuthor,
    private val getCommentsByBoardId: GetCommentsByBoardId,
    private val getCommentsByBoardIdAfter: GetCommentsByBoardIdAfter,
    private val getCommentsByIds: GetCommentsByIds,
) {
    // ===================================================
    // 댓글 생성/수정/삭제 관련 API (POST, PUT, DELETE)
    // ===================================================

    @PublicWithTokenCheck
    @PostMapping("/boards/{boardId}/comments")
    @Operation(
        summary = "댓글 작성",
        description = "게시글에 새로운 댓글을 작성합니다. 회원은 로그인 정보로, 비회원은 비밀번호를 통해 댓글을 작성할 수 있습니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "댓글 작성 성공",
        content = [Content(schema = Schema(implementation = CreateComment.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "잘못된 요청 (유효성 검증 실패)",
        content = [Content(mediaType = "application/json")],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "게시글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun addComment(
        @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable boardId: Long,
        @Parameter(description = "댓글 작성 정보", required = true) @RequestBody request: CreateComment.Request,
    ): ResponseEntity<ApiResponse<CreateComment.Response>> = ApiResponse.ok(createComment(request.of(boardId)))

    @PublicWithTokenCheck
    @PutMapping("/comments/{commentId}")
    @Operation(
        summary = "댓글 수정",
        description = "작성된 댓글을 수정합니다. 본인의 댓글만 수정 가능하며, 비회원은 비밀번호 인증이 필요합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "댓글 수정 성공",
        content = [Content(schema = Schema(implementation = UpdateComment.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "잘못된 요청 (유효성 검증 실패)",
        content = [Content(mediaType = "application/json")],
    )
    @SwaggerResponse(
        responseCode = "403",
        description = "권한 없음 또는 비밀번호 불일치",
        content = [Content(mediaType = "application/json")],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "댓글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun updateComment(
        @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable commentId: Long,
        @Parameter(description = "댓글 수정 정보", required = true) @RequestBody request: UpdateComment.Request,
    ): ResponseEntity<ApiResponse<UpdateComment.Response>> = ApiResponse.ok(updateComment(request.of(commentId)))

    @PublicWithTokenCheck
    @DeleteMapping("/comments/{commentId}")
    @Operation(
        summary = "댓글 삭제",
        description = "작성된 댓글을 삭제합니다. 로그인 사용자는 자신의 댓글만, 비로그인 사용자는 비밀번호 검증 후 삭제 가능합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "댓글 삭제 성공",
        content = [Content(schema = Schema(implementation = DeleteComment.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "403",
        description = "권한 없음 또는 비밀번호 불일치",
        content = [Content(mediaType = "application/json")],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "댓글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun deleteComment(
        @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable commentId: Long,
        @Parameter(description = "댓글 삭제 요청 정보 (비회원인 경우 비밀번호 필요)", required = true) @RequestBody request: DeleteComment.Request,
    ): ResponseEntity<ApiResponse<DeleteComment.Response>> = ApiResponse.ok(deleteComment(request.of(commentId)))

    // ===================================================
    // 댓글 조회 관련 API (GET)
    // ===================================================

    @Public
    @GetMapping("/comments/{commentId}")
    @Operation(
        summary = "댓글 단일 조회",
        description = "특정 댓글을 ID로 조회합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "댓글 조회 성공",
        content = [Content(schema = Schema(implementation = GetCommentDetail.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "댓글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun getComment(
        @Parameter(description = "댓글 ID", required = true, example = "1") @PathVariable commentId: Long,
    ): ResponseEntity<ApiResponse<GetCommentDetail.Response>> = ApiResponse.ok(getCommentDetail(GetCommentDetail.Request(commentId)))

    @Public
    @GetMapping("/boards/{boardId}/comments")
    @Operation(
        summary = "게시글별 댓글 목록 조회",
        description = "특정 게시글의 댓글 목록을 조회합니다. 페이징을 지원합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글별 댓글 조회 성공",
        content = [Content(schema = Schema(implementation = GetCommentsByBoardId.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "게시글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun getCommentsByBoardId(
        @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable boardId: Long,
        @Parameter(description = "페이지 번호 (0부터 시작)", required = false, example = "0") @RequestParam(required = false) page: Int?,
    ): ResponseEntity<ApiResponse<GetCommentsByBoardId.Response>> = ApiResponse.ok(getCommentsByBoardId(GetCommentsByBoardId.Request(boardId, page)))

    @Public
    @GetMapping("/boards/{boardId}/comments/after")
    @Operation(
        summary = "게시글별 댓글 무한 스크롤 조회",
        description = "특정 게시글의 댓글 목록을 커서 기반으로 조회합니다. 무한 스크롤 방식의 페이징을 구현할 때 사용합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글별 커서 기반 댓글 조회 성공",
        content = [Content(schema = Schema(implementation = GetCommentsByBoardIdAfter.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "게시글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun getCommentsByBoardIdAfter(
        @Parameter(description = "게시글 ID", required = true, example = "1") @PathVariable boardId: Long,
        @Parameter(description = "마지막으로 조회한 댓글 ID", required = false, example = "15") @RequestParam(required = false) lastCommentId: Long?,
        @Parameter(description = "조회할 댓글 수", required = false, example = "20") @RequestParam(required = false, defaultValue = "20") limit: Int,
    ): ResponseEntity<ApiResponse<GetCommentsByBoardIdAfter.Response>> = ApiResponse.ok(getCommentsByBoardIdAfter(GetCommentsByBoardIdAfter.Request(boardId, lastCommentId, limit)))

    // ===================================================
    // 댓글 일괄 조회 관련 API (POST)
    // ===================================================

    @Public
    @PostMapping("/comments/bulk")
    @Operation(
        summary = "여러 댓글 일괄 조회",
        description = "여러 댓글을 한 번에 조회합니다. 댓글 ID 목록을 전달하면 해당하는 모든 댓글 정보를 반환합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "여러 댓글 조회 성공",
        content = [Content(schema = Schema(implementation = GetCommentsByIds.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "400",
        description = "잘못된 요청 (유효성 검증 실패)",
        content = [Content(mediaType = "application/json")],
    )
    fun getCommentsByIds(
        @Parameter(description = "조회할 댓글 ID 목록", required = true, example = "[1, 2, 3]") @RequestBody request: GetCommentsByIds.Request,
    ): ResponseEntity<ApiResponse<GetCommentsByIds.Response>> = ApiResponse.ok(getCommentsByIds.invoke(request))

    // ===================================================
    // 비활성화된 API (주석 처리)
    // ===================================================

//    @Public
//    @GetMapping("/authors/{author}/activities")
//    @Operation(
//        summary = "작성자 활동 조회",
//        description = "특정 작성자의 댓글 활동 내역을 조회합니다."
//    )
//    @SwaggerResponse(
//        responseCode = "200",
//        description = "작성자 활동 조회 성공",
//        content = [Content(schema = Schema(implementation = GetAuthorActivities.Response::class))]
//    )
//    @SwaggerResponse(
//        responseCode = "404",
//        description = "작성자를 찾을 수 없음",
//        content = [Content(mediaType = "application/json")]
//    )
//    fun getAuthorActivities(
//        @Parameter(description = "작성자 이름", required = true, example = "홍길동") @PathVariable author: String,
//        @Parameter(description = "페이지 번호", required = false, example = "0") @RequestParam(required = false) page: Int?,
//    ): ResponseEntity<ApiResponse<GetAuthorActivities.Response>> = ApiResponse.ok(getAuthorActivities(GetAuthorActivities.Request(author, page)))

//    @Public
//    @GetMapping("/authors/{author}/comments")
//    @Operation(
//        summary = "작성자별 댓글 조회",
//        description = "특정 작성자가 작성한 댓글 목록을 조회합니다. 페이징을 지원합니다."
//    )
//    @SwaggerResponse(
//        responseCode = "200",
//        description = "작성자별 댓글 조회 성공",
//        content = [Content(schema = Schema(implementation = GetCommentsByAuthor.Response::class))]
//    )
//    fun getCommentsByAuthor(
//        @Parameter(description = "작성자 이름", required = true, example = "홍길동") @PathVariable author: String,
//        @Parameter(description = "페이지 번호 (0부터 시작)", required = false, example = "0") @RequestParam(required = false) page: Int?,
//    ): ResponseEntity<ApiResponse<GetCommentsByAuthor.Response>> = ApiResponse.ok(getCommentsByAuthor(GetCommentsByAuthor.Request(author, page)))
}
