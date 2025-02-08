// BoardController.kt
package com.example.jobstat.board

import com.example.jobstat.board.usecase.*
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.wrapper.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}")
internal class BoardController(
    private val createGuestBoard: CreateGuestBoard,
    private val createMemberBoard: CreateMemberBoard,
    private val getBoardDetail: GetBoardDetail,
    private val getBoardList: GetBoardList,
    private val getBoardStats: GetBoardStats,
    private val getTopBoards: GetTopBoards,
    private val updateBoard: UpdateBoard,
    private val deleteBoard: DeleteBoard,
    private val likeBoard: LikeBoard,
) {
    @PostMapping("/members/{memberId}/boards")
    fun createMemberBoard(
        @PathVariable memberId: Long,
        @RequestBody request: CreateMemberBoard.Request,
    ): ResponseEntity<ApiResponse<CreateMemberBoard.Response>> = ApiResponse.ok(createMemberBoard.invoke(request))

    @Public
    @PostMapping("/boards/guest")
    fun createGuestBoard(
        @RequestBody request: CreateGuestBoard.Request,
    ): ResponseEntity<ApiResponse<CreateGuestBoard.Response>> = ApiResponse.ok(createGuestBoard.invoke(request))

    @Public
    @GetMapping("/boards/{boardId}")
    fun getBoardDetail(
        @PathVariable boardId: Long,
        @RequestParam(required = false) commentPage: Int?,
    ): ResponseEntity<ApiResponse<GetBoardDetail.Response>> = ApiResponse.ok(getBoardDetail(GetBoardDetail.Request(boardId, commentPage)))

    @Public
    @GetMapping("/boards")
    fun getBoardList(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<ApiResponse<GetBoardList.Response>> = ApiResponse.ok(getBoardList(GetBoardList.Request(page, categoryId, author, keyword)))

    @Public
    @GetMapping("/authors/{author}/boards/stats")
    fun getBoardStats(
        @PathVariable author: String,
        @RequestParam boardId: Long,
    ): ResponseEntity<ApiResponse<GetBoardStats.Response>> = ApiResponse.ok(getBoardStats(GetBoardStats.Request(author, boardId)))

    @Public
    @GetMapping("/boards/top")
    fun getTopBoards(
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<ApiResponse<GetTopBoards.Response>> = ApiResponse.ok(getTopBoards(GetTopBoards.Request(limit)))

    @PutMapping("/boards/{boardId}")
    fun updateBoard(
        @PathVariable boardId: Long,
        @RequestBody request: UpdateBoard.Request,
    ): ResponseEntity<ApiResponse<UpdateBoard.Response>> = ApiResponse.ok(updateBoard(request))

    @DeleteMapping("/boards/{boardId}")
    fun deleteBoard(
        @PathVariable boardId: Long,
        @RequestParam(required = false) password: String?,
    ): ResponseEntity<ApiResponse<DeleteBoard.Response>> = ApiResponse.ok(deleteBoard(DeleteBoard.Request(boardId, password)))

    @PostMapping("/boards/{boardId}/like")
    fun likeBoard(
        @PathVariable boardId: Long,
    ): ResponseEntity<ApiResponse<LikeBoard.Response>> = ApiResponse.ok(likeBoard(LikeBoard.Request(boardId)))
}
