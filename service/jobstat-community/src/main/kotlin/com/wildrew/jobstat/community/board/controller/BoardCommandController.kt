package com.wildrew.jobstat.community.board.controller

import com.wildrew.jobstat.community.board.usecase.command.*
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
@Tag(name = "게시판 명령", description = "게시글 생성/수정/삭제 관련 API")
class BoardCommandController(
    private val createBoard: CreateBoard,
    private val updateBoard: UpdateBoard,
    private val deleteBoard: DeleteBoard,
    private val likeBoard: LikeBoard,
    private val unlikeBoard: UnlikeBoard,
) {
    @PublicWithTokenCheck
    @PostMapping("/boards")
    @Operation(
        summary = "게시글 작성",
        description = "새로운 게시글을 작성합니다. 회원은 로그인 정보로, 비회원은 비밀번호를 통해 게시글을 작성할 수 있습니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 작성 성공",
        content = [Content(schema = Schema(implementation = CreateBoard.Response::class))],
    )
    fun createBoard(
        @Parameter(description = "게시글 작성 정보", required = true)
        @RequestBody request: CreateBoard.Request,
    ): ResponseEntity<ApiResponse<CreateBoard.Response>> = ApiResponse.ok(createBoard.invoke(request))

    @PublicWithTokenCheck
    @PutMapping("/boards/{boardId}")
    @Operation(
        summary = "게시글 수정",
        description = "작성된 게시글을 수정합니다. 본인의 게시글만 수정 가능하며, 비회원은 비밀번호 인증이 필요합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 수정 성공",
        content = [Content(schema = Schema(implementation = UpdateBoard.Response::class))],
    )
    fun updateBoard(
        @Parameter(description = "수정할 게시글 ID", required = true, example = "1")
        @PathVariable boardId: Long,
        @Parameter(description = "게시글 수정 정보", required = true)
        @RequestBody request: UpdateBoard.Request,
    ): ResponseEntity<ApiResponse<UpdateBoard.Response>> = ApiResponse.ok(updateBoard(request.of(boardId)))

    @PublicWithTokenCheck
    @DeleteMapping("/boards/{boardId}")
    @Operation(
        summary = "게시글 삭제",
        description = "게시글을 삭제합니다. 로그인 사용자는 자신의 글만, 비로그인 사용자는 비밀번호 검증 후 삭제 가능합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 삭제 성공",
        content = [Content(schema = Schema(implementation = DeleteBoard.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "403",
        description = "권한 없음 또는 비밀번호 불일치",
        content = [Content(mediaType = "application/json")],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "게시글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun deleteBoardById(
        @Parameter(description = "삭제할 게시글 ID", required = true, example = "1")
        @PathVariable boardId: Long,
        @Parameter(description = "게시글 삭제 요청 정보 (비회원인 경우 비밀번호 필요)", required = false)
        @RequestBody(required = false) request: DeleteBoard.Request?,
    ): ResponseEntity<ApiResponse<DeleteBoard.Response>> {
        val executeRequest = request?.of(boardId) ?: DeleteBoard.ExecuteRequest(boardId, null)
        return ApiResponse.ok(deleteBoard.invoke(executeRequest))
    }

    @PostMapping("/boards/{boardId}/likes")
    @Operation(
        summary = "게시글 좋아요",
        description = "게시글에 좋아요를 표시합니다. 로그인 사용자만 이용 가능합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 좋아요 성공",
        content = [Content(schema = Schema(implementation = LikeBoard.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "401",
        description = "로그인이 필요합니다",
        content = [Content(mediaType = "application/json")],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "게시글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun likeBoard(
        @Parameter(description = "좋아요할 게시글 ID", required = true, example = "1")
        @PathVariable boardId: Long,
    ): ResponseEntity<ApiResponse<LikeBoard.Response>> = ApiResponse.ok(likeBoard(LikeBoard.Request(boardId)))

    @DeleteMapping("/boards/{boardId}/likes")
    @Operation(
        summary = "게시글 좋아요 취소",
        description = "게시글의 좋아요를 취소합니다. 로그인 사용자만 이용 가능합니다.",
    )
    @SwaggerResponse(
        responseCode = "200",
        description = "게시글 좋아요 취소 성공",
        content = [Content(schema = Schema(implementation = UnlikeBoard.Response::class))],
    )
    @SwaggerResponse(
        responseCode = "401",
        description = "로그인이 필요합니다",
        content = [Content(mediaType = "application/json")],
    )
    @SwaggerResponse(
        responseCode = "404",
        description = "게시글을 찾을 수 없음",
        content = [Content(mediaType = "application/json")],
    )
    fun unlikeBoard(
        @Parameter(description = "좋아요 취소할 게시글 ID", required = true, example = "1")
        @PathVariable boardId: Long,
    ): ResponseEntity<ApiResponse<UnlikeBoard.Response>> = ApiResponse.ok(unlikeBoard(UnlikeBoard.Request(boardId)))
}
