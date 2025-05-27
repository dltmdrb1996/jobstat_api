// package com.example.jobstat.community.board.usecase.get
//
// import com.wildrew.jobstat.community.board.entity.Board
// import com.wildrew.jobstat.community.board.service.BoardService
// import com.wildrew.jobstat.community.board.utils.BoardConstants
// import com.wildrew.jobstat.community.counting.CounterService
// import com.wildrew.jobstat.core.global.extension.toEpochMilli
// import com.wildrew.jobstat.core.global.utils.SecurityUtils
// import com.wildrew.jobstat.core.usecase.impl.ValidUseCase
// import io.swagger.v3.oas.annotations.media.Schema
// import jakarta.validation.Validator
// import jakarta.validation.constraints.Max
// import jakarta.validation.constraints.Positive
// import org.springframework.stereotype.Service
// import java.time.LocalDateTime
//
// @Service
// class FetchBoardListForReadServerByCursor(
//    private val boardService: BoardService,
//    private val securityUtils: SecurityUtils,
//    private val counterService: CounterService,
//    validator: Validator,
// ) : ValidUseCase<FetchBoardListForReadServerByCursor.Request, FetchBoardListForReadServerByCursor.Response>(validator) {
//    override fun execute(request: Request): Response {
//        val userId = securityUtils.getCurrentUserId()?.toString()
//        val limit = request.limit.coerceAtMost(BoardConstants.DEFAULT_PAGE_SIZE)
//
//        val boards =
//            when {
//                request.categoryId != null ->
//                    boardService.getBoardsByCategoryAfter(request.categoryId, request.lastBoardId, limit)
//                request.author != null ->
//                    boardService.getBoardsByAuthorAfter(request.author, request.lastBoardId, limit)
//                request.keyword != null ->
//                    boardService.searchBoardsAfter(request.keyword, request.lastBoardId, limit)
//                else ->
//                    boardService.getBoardsAfter(request.lastBoardId, limit)
//            }
//
//        if (boards.isEmpty()) {
//            return Response(emptyList(), false)
//        }
//
//        val boardItems = updateCountersAndMapBoards(boards, userId)
//
//        // 다음 페이지 여부 확인
//        val hasNext = boards.size >= limit
//
//        return Response(
//            items = boardItems,
//            hasNext = hasNext,
//        )
//    }
//
//    private fun updateCountersAndMapBoards(
//        boards: List<Board>,
//        userId: String?,
//    ): List<BoardItem> {
//        val boardIdsWithCounts =
//            boards.map { board ->
//                Triple(board.id, board.viewCount, board.likeCount)
//            }
//
//        val countersMap =
//            counterService
//                .getBulkBoardCounters(boardIdsWithCounts, userId)
//                .associateBy { it.boardId }
//
//        return boards.map { board ->
//            val counter = countersMap[board.id]
//            mapToBoardItem(
//                board,
//                viewCount = counter?.viewCount ?: board.viewCount,
//                likeCount = counter?.likeCount ?: board.likeCount,
//                userLiked = counter?.userLiked ?: false,
//            )
//        }
//    }
//
//    private fun mapToBoardItem(
//        board: Board,
//        viewCount: Int,
//        likeCount: Int,
//        userLiked: Boolean,
//    ): BoardItem =
//        with(board) {
//            BoardItem(
//                id = id.toString(),
//                userId = userId,
//                title = title,
//                content = content,
//                author = author,
//                viewCount = viewCount,
//                likeCount = likeCount,
//                commentCount = commentCount,
//                categoryId = category.id,
//                createdAt = createdAt,
//                userLiked = userLiked,
//                eventTs = updatedAt.toEpochMilli(),
//            )
//        }
//
//    @Schema(
//        name = "FetchBoardListByCursorRequest",
//        description = "커서 기반 게시글 목록 조회 요청 모델",
//    )
//    data class Request(
//        @field:Schema(
//            description = "마지막으로 조회한 게시글 ID",
//            example = "100",
//            nullable = true,
//        )
//        val lastBoardId: Long? = null,
//        @field:Schema(
//            description = "카테고리 ID",
//            example = "1",
//            nullable = true,
//        )
//        val categoryId: Long? = null,
//        @field:Schema(
//            description = "작성자",
//            example = "홍길동",
//            nullable = true,
//        )
//        val author: String? = null,
//        @field:Schema(
//            description = "검색 키워드",
//            example = "안녕하세요",
//            nullable = true,
//        )
//        val keyword: String? = null,
//        @field:Schema(
//            description = "조회할 게시글 수",
//            example = "20",
//            minimum = "1",
//            maximum = "100",
//            defaultValue = "20",
//        )
//        @field:Positive(message = "조회할 게시글 수는 양수여야 합니다")
//        @field:Max(value = 100, message = "조회할 게시글 수는 최대 100개까지 가능합니다")
//        val limit: Int = BoardConstants.DEFAULT_PAGE_SIZE,
//    )
//
//    @Schema(
//        name = "FetchBoardListByCursorResponse",
//        description = "커서 기반 게시글 목록 조회 응답 모델",
//    )
//    data class Response(
//        @field:Schema(
//            description = "게시글 목록",
//        )
//        val items: List<BoardItem>,
//        @field:Schema(
//            description = "다음 페이지 존재 여부",
//            example = "true",
//        )
//        val hasNext: Boolean,
//    )
//
//    @Schema(
//        name = "BoardItemByCursor",
//        description = "커서 기반 게시글 정보 모델",
//    )
//    data class BoardItem(
//        @field:Schema(
//            description = "게시글 ID",
//            example = "1",
//        )
//        val id: String,
//        @field:Schema(
//            description = "작성자 ID",
//            example = "100",
//            nullable = true,
//        )
//        val userId: Long?,
//        @field:Schema(
//            description = "게시글 제목",
//            example = "안녕하세요, 첫 게시글입니다",
//        )
//        val title: String,
//        @field:Schema(
//            description = "게시글 내용",
//            example = "게시글 내용입니다. 여기에 자세한 내용을 작성합니다.",
//        )
//        val content: String,
//        @field:Schema(
//            description = "작성자",
//            example = "홍길동",
//        )
//        val author: String,
//        @field:Schema(
//            description = "조회수",
//            example = "42",
//            minimum = "0",
//        )
//        val viewCount: Int,
//        @field:Schema(
//            description = "좋아요 수",
//            example = "15",
//            minimum = "0",
//        )
//        val likeCount: Int,
//        @field:Schema(
//            description = "댓글 수",
//            example = "7",
//            minimum = "0",
//        )
//        val commentCount: Int,
//        @field:Schema(
//            description = "카테고리 ID",
//            example = "2",
//        )
//        val categoryId: Long,
//        @field:Schema(
//            description = "현재 사용자의 좋아요 여부",
//            example = "false",
//        )
//        val userLiked: Boolean,
//        @field:Schema(
//            description = "생성 일시",
//            example = "2023-05-10T14:30:15.123456",
//        )
//        val createdAt: LocalDateTime,
//        @field:Schema(
//            description = "이벤트 타임스탬프 (밀리초)",
//            example = "1683727815123",
//        )
//        val eventTs: Long,
//    )
// }
