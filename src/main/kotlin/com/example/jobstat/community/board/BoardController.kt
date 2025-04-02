// BoardController.kt
package com.example.jobstat.community.board

import com.example.jobstat.community.board.usecase.command.CreateBoard
import com.example.jobstat.community.board.usecase.command.DeleteBoard
import com.example.jobstat.community.board.usecase.fetch.GetBoardDetail
import com.example.jobstat.community.board.usecase.fetch.GetBoardList
import com.example.jobstat.community.board.usecase.fetch.GetBoardStats
import com.example.jobstat.community.board.usecase.fetch.GetBoardsByIds
import com.example.jobstat.community.board.usecase.fetch.GetTopBoards
import com.example.jobstat.community.board.usecase.command.LikeBoard
import com.example.jobstat.community.board.usecase.command.UpdateBoard
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
@Tag(name = "게시판", description = "게시글 관리 관련 API")
internal class BoardController(
    private val createBoard: CreateBoard,
    private val getBoardDetail: GetBoardDetail,
    private val getBoardList: GetBoardList,
    private val getBoardStats: GetBoardStats,
    private val getTopBoards: GetTopBoards,
    private val updateBoard: UpdateBoard,
    private val deleteBoard: DeleteBoard,
    private val likeBoard: LikeBoard,
    private val getBoardsByIds: GetBoardsByIds,
) {
    @PublicWithTokenCheck
    @PostMapping("/boards")
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다.")
    fun createBoard(
        @RequestBody request: CreateBoard.Request,
    ): ResponseEntity<ApiResponse<CreateBoard.Response>> = ApiResponse.ok(createBoard.invoke(request))

    @Public
    @GetMapping("/boards/{boardId}")
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 내용을 조회합니다.")
    fun fetchBoardDetail(
        @PathVariable boardId: Long,
        @RequestParam(required = false) commentPage: Int?,
    ): ResponseEntity<ApiResponse<GetBoardDetail.Response>> = ApiResponse.ok(getBoardDetail(GetBoardDetail.Request(boardId, commentPage)))

    @Public
    @PostMapping("/boards/bulk")
    @Operation(summary = "게시글 벌크 조회", description = "여러 게시글 ID로 게시글 목록을 한번에 조회합니다.")
    fun fetchBoardsByIds(
        @RequestBody request: GetBoardsByIds.Request,
    ): ResponseEntity<ApiResponse<GetBoardsByIds.Response>> = ApiResponse.ok(getBoardsByIds(request))

    @Public
    @GetMapping("/boards")
    @Operation(summary = "게시글 목록 조회", description = "게시글 목록을 조회합니다.")
    fun getBoardList(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<ApiResponse<GetBoardList.Response>> = ApiResponse.ok(getBoardList(GetBoardList.Request(page, categoryId, author, keyword)))

    @Public
    @GetMapping("/authors/{author}/boards/stats")
    @Operation(summary = "게시글 통계 조회", description = "특정 작성자의 게시글 통계를 조회합니다.")
    fun getBoardStats(
        @PathVariable author: String,
        @RequestParam boardId: Long,
    ): ResponseEntity<ApiResponse<GetBoardStats.Response>> = ApiResponse.ok(getBoardStats(GetBoardStats.Request(author, boardId)))

    @Public
    @GetMapping("/boards/top")
    @Operation(summary = "인기 게시글 조회", description = "인기 있는 게시글 목록을 조회합니다.")
    fun getTopBoards(
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<ApiResponse<GetTopBoards.Response>> = ApiResponse.ok(getTopBoards(GetTopBoards.Request(limit)))

    @PublicWithTokenCheck
    @PutMapping("/boards/{boardId}")
    @Operation(summary = "게시글 수정", description = "작성된 게시글을 수정합니다.")
    fun updateBoard(
        @PathVariable boardId: Long,
        @RequestBody request: UpdateBoard.Request,
    ): ResponseEntity<ApiResponse<UpdateBoard.Response>> = ApiResponse.ok(updateBoard(request.of(boardId)))

    @PublicWithTokenCheck
    @DeleteMapping("/{boardId}")
    @Operation(
        summary = "게시글 삭제",
        description = "게시글을 삭제합니다. 로그인 사용자는 자신의 글만, 비로그인 사용자는 비밀번호 검증 후 삭제 가능합니다."
    )
    fun deleteBoardById(
        @PathVariable boardId: Long,
        @RequestBody(required = false) request: DeleteBoard.Request?
    ): ResponseEntity<ApiResponse<DeleteBoard.Response>> {
        val executeRequest = request?.of(boardId) ?: DeleteBoard.ExecuteRequest(boardId, null)
        return ApiResponse.ok(deleteBoard.invoke(executeRequest))
    }

    @PostMapping("/boards/{boardId}/likes")
    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 표시합니다.")
    fun likeBoard(
        @PathVariable boardId: Long,
    ): ResponseEntity<ApiResponse<LikeBoard.Response>> = ApiResponse.ok(likeBoard(LikeBoard.Request(boardId)))

}
