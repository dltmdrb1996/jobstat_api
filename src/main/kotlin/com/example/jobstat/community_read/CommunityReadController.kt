package com.example.jobstat.community_read

import com.example.jobstat.community_read.usecase.query.*
import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.global.wrapper.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/community-read")
@Tag(name = "커뮤니티 Read 모델", description = "커뮤니티 읽기 모델 관련 API")
class CommunityReadController(
    private val getBoardDetailUseCase: GetBoardDetailUseCase,
    private val getBoardListUseCase: GetBoardListUseCase,
    private val getCommentByIdUseCase: GetCommentByIdUseCase,
    private val getCommentsByBoardIdUseCase: GetCommentsByBoardIdUseCase,
    private val getCommentsByAuthorUseCase: GetCommentsByAuthorUseCase,
) {

    /**
     * 게시글 ID로 게시글 조회
     */
    @Public
    @GetMapping("/boards/{boardId}")
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 내용을 조회합니다.")
    fun getBoardById(
        @PathVariable boardId: Long,
        @RequestParam(required = false) commentPage: Int?,
        @RequestParam(required = false) commentSize: Int?
    ): ResponseEntity<ApiResponse<GetBoardDetailUseCase.Response>> =
        GetBoardDetailUseCase.Request(
            boardId = boardId,
            commentPage = commentPage
        ).let { ApiResponse.ok(getBoardDetailUseCase(it)) }

    /**
     * 모든 게시글 조회
     */
    @Public
    @GetMapping("/boards")
    @Operation(summary = "게시글 목록 조회", description = "게시글 목록을 조회합니다.")
    fun getBoardList(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) keyword: String?
    ): ResponseEntity<ApiResponse<GetBoardListUseCase.Response>> =
        GetBoardListUseCase.Request(
            page = page,
            size = size,
            categoryId = categoryId,
            author = author,
            keyword = keyword
        ).let { ApiResponse.ok(getBoardListUseCase(it)) }


    /**
     * 작성자별 게시글 조회
     */
    @Public
    @GetMapping("/boards/author/{author}")
    @Operation(summary = "작성자별 게시글 조회", description = "특정 작성자가 작성한 게시글 목록을 조회합니다.")
    fun getBoardsByAuthor(
        @PathVariable author: String,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<GetBoardListUseCase.Response>> =
        GetBoardListUseCase.Request(
            page = page,
            size = size,
            author = author
        ).let { ApiResponse.ok(getBoardListUseCase(it)) }

    /**
     * 카테고리별 게시글 조회
     */
    @Public
    @GetMapping("/boards/category/{categoryId}")
    @Operation(summary = "카테고리별 게시글 조회", description = "특정 카테고리의 게시글 목록을 조회합니다.")
    fun getBoardsByCategory(
        @PathVariable categoryId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<GetBoardListUseCase.Response>> =
        GetBoardListUseCase.Request(
            page = page,
            size = size,
            categoryId = categoryId
        ).let { ApiResponse.ok(getBoardListUseCase(it)) }
//
//    /**
//     * 댓글 ID로 댓글 조회
//     */
//    @Public
//    @GetMapping("/comments/{commentId}")
//    @Operation(summary = "댓글 조회", description = "특정 댓글을 조회합니다.")
//    fun getCommentById(
//        @PathVariable commentId: Long
//    ): ResponseEntity<ApiResponse<CommentReadResponse>> =
//        GetCommentByIdUseCase.Request(commentId = commentId)
//            .let { getCommentByIdUseCase(it).comment }
//            ?.let { ResponseEntity.ok(ApiResponse.ok(it)) }
//            ?: ResponseEntity.notFound().build()
//
//    /**
//     * 게시글에 달린 모든 댓글 조회
//     */
//    @Public
//    @GetMapping("/boards/{boardId}/comments")
//    @Operation(summary = "게시글별 댓글 조회", description = "특정 게시글에 달린 댓글 목록을 조회합니다.")
//    fun getCommentsByBoardId(
//        @PathVariable boardId: Long,
//        @RequestParam(required = false) page: Int?,
//        @RequestParam(required = false) size: Int?
//    ): ResponseEntity<ApiResponse<GetCommentsByBoardIdUseCase.Response>> =
//        GetCommentsByBoardIdUseCase.Request(
//            boardId = boardId,
//            page = page,
//            size = size
//        ).let { ApiResponse.ok(getCommentsByBoardIdUseCase(it)) }
//
//    /**
//     * 작성자별 댓글 조회
//     */
//    @Public
//    @GetMapping("/comments/author/{author}")
//    @Operation(summary = "작성자별 댓글 조회", description = "특정 작성자가 작성한 댓글 목록을 조회합니다.")
//    fun getCommentsByAuthor(
//        @PathVariable author: String,
//        @RequestParam(required = false) page: Int?,
//        @RequestParam(required = false) size: Int?
//    ): ResponseEntity<ApiResponse<GetCommentsByAuthorUseCase.Response>> =
//        GetCommentsByAuthorUseCase.Request(
//            author = author,
//            page = page,
//            size = size
//        ).let { ApiResponse.ok(getCommentsByAuthorUseCase(it)) }
//
//    /**
//     * 게시글 좋아요 상태 업데이트
//     */
//    @PublicWithTokenCheck
//    @PostMapping("/boards/{boardId}/likes")
//    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 표시합니다.")
//    fun likeBoard(
//        @PathVariable boardId: Long
//    ): ResponseEntity<ApiResponse<LikeBoardUseCase.Response>> =
//        LikeBoardUseCase.Request(boardId = boardId)
//            .let { ApiResponse.ok(likeBoardUseCase(it)) }
//}
//
//@RestController
//@RequestMapping("/api/${RestConstants.Versions.V1}/community-read")
//@Tag(name = "커뮤니티 Read 모델", description = "커뮤니티 읽기 모델 관련 API")
//class CommunityReadController(
//    private val getBoardDetailUseCase: GetBoardDetailUseCase,
//    private val getBoardListUseCase: GetBoardListUseCase,
//    private val getTopBoardsUseCase: GetTopBoardsUseCase,
//    private val getBoardStatsUseCase: GetBoardStatsUseCase,
//    private val getCommentByIdUseCase: GetCommentByIdUseCase,
//    private val getCommentsByBoardIdUseCase: GetCommentsByBoardIdUseCase,
//    private val getCommentsByAuthorUseCase: GetCommentsByAuthorUseCase,
//    private val likeBoardUseCase: LikeBoardUseCase,
//    private val getRecentComments: GetRecentComments,
//    private val getAuthorActivitiesUseCase: GetAuthorActivitiesUseCase
//) {
//
//    /**
//     * 게시글 ID로 게시글 조회
//     */
//    @Public
//    @GetMapping("/boards/{boardId}")
//    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 내용을 조회합니다.")
//    fun getBoardById(
//        @PathVariable boardId: Long,
//        @RequestParam(required = false) commentPage: Int?,
//        @RequestParam(required = false) commentSize: Int?
//    ): ResponseEntity<ApiResponse<GetBoardDetailUseCase.Response>> =
//        GetBoardDetailUseCase.Request(
//            boardId = boardId,
//            commentPage = commentPage
//        ).let { ApiResponse.ok(getBoardDetailUseCase(it)) }
//
//    /**
//     * 모든 게시글 조회
//     */
//    @Public
//    @GetMapping("/boards")
//    @Operation(summary = "게시글 목록 조회", description = "게시글 목록을 조회합니다.")
//    fun getBoardList(
//        @RequestParam(required = false) page: Int?,
//        @RequestParam(required = false) size: Int?,
//        @RequestParam(required = false) categoryId: Long?,
//        @RequestParam(required = false) author: String?,
//        @RequestParam(required = false) keyword: String?
//    ): ResponseEntity<ApiResponse<GetBoardListUseCase.Response>> =
//        GetBoardListUseCase.Request(
//            page = page,
//            size = size,
//            categoryId = categoryId,
//            author = author,
//            keyword = keyword
//        ).let { ApiResponse.ok(getBoardListUseCase(it)) }
//
//    /**
//     * 인기 게시글 조회
//     */
//    @Public
//    @GetMapping("/boards/top")
//    @Operation(summary = "인기 게시글 조회", description = "조회수 기준 인기 게시글 목록을 조회합니다.")
//    fun getTopBoards(
//        @RequestParam(required = false, defaultValue = "10") limit: Int,
//        @RequestParam(required = false, defaultValue = "views") type: String
//    ): ResponseEntity<ApiResponse<GetTopBoardsUseCase.Response>> =
//        GetTopBoardsUseCase.Request(type = type, limit = limit)
//            .let { ApiResponse.ok(getTopBoardsUseCase(it)) }
//
//    /**
//     * 작성자별 게시글 조회
//     */
//    @Public
//    @GetMapping("/boards/author/{author}")
//    @Operation(summary = "작성자별 게시글 조회", description = "특정 작성자가 작성한 게시글 목록을 조회합니다.")
//    fun getBoardsByAuthor(
//        @PathVariable author: String,
//        @RequestParam(required = false) page: Int?,
//        @RequestParam(required = false) size: Int?
//    ): ResponseEntity<ApiResponse<GetBoardListUseCase.Response>> =
//        GetBoardListUseCase.Request(
//            page = page,
//            size = size,
//            author = author
//        ).let { ApiResponse.ok(getBoardListUseCase(it)) }
//
//    /**
//     * 카테고리별 게시글 조회
//     */
//    @Public
//    @GetMapping("/boards/category/{categoryId}")
//    @Operation(summary = "카테고리별 게시글 조회", description = "특정 카테고리의 게시글 목록을 조회합니다.")
//    fun getBoardsByCategory(
//        @PathVariable categoryId: Long,
//        @RequestParam(required = false) page: Int?,
//        @RequestParam(required = false) size: Int?
//    ): ResponseEntity<ApiResponse<GetBoardListUseCase.Response>> =
//        GetBoardListUseCase.Request(
//            page = page,
//            size = size,
//            categoryId = categoryId
//        ).let { ApiResponse.ok(getBoardListUseCase(it)) }
//
//    /**
//     * 게시글 통계 조회
//     */
//    @Public
//    @GetMapping("/authors/{author}/boards/stats")
//    @Operation(summary = "게시글 통계 조회", description = "작성자의 게시글 통계 정보를 조회합니다.")
//    fun getBoardStats(
//        @PathVariable author: String,
//        @RequestParam boardId: Long
//    ): ResponseEntity<ApiResponse<GetBoardStatsUseCase.Response>> =
//        GetBoardStatsUseCase.Request(author = author, boardId = boardId)
//            .let { ApiResponse.ok(getBoardStatsUseCase(it)) }
//
//    /**
//     * 댓글 ID로 댓글 조회
//     */
//    @Public
//    @GetMapping("/comments/{commentId}")
//    @Operation(summary = "댓글 조회", description = "특정 댓글을 조회합니다.")
//    fun getCommentById(
//        @PathVariable commentId: Long
//    ): ResponseEntity<ApiResponse<CommentReadResponse>> =
//        GetCommentByIdUseCase.Request(commentId = commentId)
//            .let { getCommentByIdUseCase(it).comment }
//            ?.let { ResponseEntity.ok(ApiResponse.ok(it)) }
//            ?: ResponseEntity.notFound().build()
//
//    /**
//     * 게시글에 달린 모든 댓글 조회
//     */
//    @Public
//    @GetMapping("/boards/{boardId}/comments")
//    @Operation(summary = "게시글별 댓글 조회", description = "특정 게시글에 달린 댓글 목록을 조회합니다.")
//    fun getCommentsByBoardId(
//        @PathVariable boardId: Long,
//        @RequestParam(required = false) page: Int?,
//        @RequestParam(required = false) size: Int?
//    ): ResponseEntity<ApiResponse<GetCommentsByBoardIdUseCase.Response>> =
//        GetCommentsByBoardIdUseCase.Request(
//            boardId = boardId,
//            page = page,
//            size = size
//        ).let { ApiResponse.ok(getCommentsByBoardIdUseCase(it)) }
//
//    /**
//     * 작성자별 댓글 조회
//     */
//    @Public
//    @GetMapping("/comments/author/{author}")
//    @Operation(summary = "작성자별 댓글 조회", description = "특정 작성자가 작성한 댓글 목록을 조회합니다.")
//    fun getCommentsByAuthor(
//        @PathVariable author: String,
//        @RequestParam(required = false) page: Int?,
//        @RequestParam(required = false) size: Int?
//    ): ResponseEntity<ApiResponse<GetCommentsByAuthorUseCase.Response>> =
//        GetCommentsByAuthorUseCase.Request(
//            author = author,
//            page = page,
//            size = size
//        ).let { ApiResponse.ok(getCommentsByAuthorUseCase(it)) }
//
//
//    /**
//     * 최근 댓글 조회
//     */
//    @Public
//    @GetMapping("/boards/{boardId}/comments/recent")
//    @Operation(summary = "최근 댓글 조회", description = "게시글의 최근 댓글 목록을 조회합니다.")
//    fun getRecentCommentsByBoardId(
//        @PathVariable boardId: Long
//    ): ResponseEntity<ApiResponse<GetRecentComments.Response>> =
//        GetRecentComments.Request(boardId = boardId)
//            .let { ApiResponse.ok(getRecentComments(it)) }
//
//    /**
//     * 작성자 활동 조회
//     */
//    @Public
//    @GetMapping("/authors/{author}/activities")
//    @Operation(summary = "작성자 활동 조회", description = "작성자의 활동 내역을 조회합니다.")
//    fun getAuthorActivities(
//        @PathVariable author: String,
//        @RequestParam(required = false) page: Int?
//    ): ResponseEntity<ApiResponse<GetAuthorActivitiesUseCase.Response>> =
//        GetAuthorActivitiesUseCase.Request(author = author, page = page)
//            .let { ApiResponse.ok(getAuthorActivitiesUseCase(it)) }
}