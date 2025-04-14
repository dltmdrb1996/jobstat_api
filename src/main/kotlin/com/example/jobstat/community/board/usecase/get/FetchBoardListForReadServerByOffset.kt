// package com.example.jobstat.community.board.usecase.get
//
// import com.example.jobstat.community.board.entity.Board
// import com.example.jobstat.community.board.service.BoardService
// import com.example.jobstat.community.board.utils.BoardConstants
// import com.example.jobstat.community.counting.CounterService
// import com.example.jobstat.core.global.extension.toEpochMilli
// import com.example.jobstat.core.global.utils.SecurityUtils
// import com.example.jobstat.core.usecase.impl.ValidUseCase
// import io.swagger.v3.oas.annotations.media.Schema
// import jakarta.validation.Validator
// import jakarta.validation.constraints.Max
// import jakarta.validation.constraints.Positive
// import org.springframework.data.domain.PageRequest
// import org.springframework.stereotype.Service
// import java.time.LocalDateTime
//
// @Service
// internal class FetchBoardListForReadServerByOffset(
//    private val boardService: BoardService,
//    private val securityUtils: SecurityUtils,
//    private val counterService: CounterService,
//    validator: Validator,
// ) : ValidUseCase<FetchBoardListForReadServerByOffset.Request, FetchBoardListForReadServerByOffset.Response>(validator) {
//    override fun execute(request: Request): Response {
//        // 현재 사용자 ID 확인
//        val userId = securityUtils.getCurrentUserId()?.toString()
//
//        // 페이지 요청 객체 생성
//        val pageable = PageRequest.of(request.page ?: 0, request.size ?: BoardConstants.DEFAULT_PAGE_SIZE)
//
//        // 조건에 따른 게시글 목록 조회
//        val boardsPage =
//            when {
//                // 카테고리별 조회
//                request.categoryId != null -> boardService.getBoardsByCategory(request.categoryId, pageable)
//                // 작성자별 조회
//                request.author != null -> boardService.getBoardsByAuthor(request.author, pageable)
//                // 검색 조회
//                request.keyword != null -> boardService.searchBoards(request.keyword, pageable)
//                // 전체 조회 (기본)
//                else -> boardService.getAllBoards(pageable)
//            }
//
//        // 조회 결과가 없는 경우 빈 목록 반환
//        if (boardsPage.isEmpty) {
//            return Response(emptyList(), 0, false)
//        }
//
//        // 카운터 정보를 업데이트하고 게시글 항목으로 변환
//        val boardItems = updateCountersAndMapBoards(boardsPage.content, userId)
//
//        // 응답 객체 생성 및 반환
//        return Response(
//            items = boardItems,
//            totalCount = boardsPage.totalElements,
//            hasNext = boardsPage.hasNext(),
//        )
//    }
//
//    /**
//     * 게시글 목록의 카운터 정보(조회수, 좋아요)를 업데이트하고 응답 모델로 변환
//     */
//    private fun updateCountersAndMapBoards(
//        boards: List<Board>,
//        userId: String?,
//    ): List<BoardItem> {
//        // 게시글 ID와 카운터 정보 준비
//        val boardIdsWithCounts =
//            boards.map { board ->
//                Triple(board.id, board.viewCount, board.likeCount)
//            }
//
//        // 대량 카운터 정보 조회
//        val countersMap =
//            counterService
//                .getBulkBoardCounters(boardIdsWithCounts, userId)
//                .associateBy { it.boardId }
//
//        // 게시글 목록과 카운터 정보 병합하여 응답 객체 생성
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
//    /**
//     * 게시글 엔티티와 카운터 정보를 응답 모델로 변환
//     */
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
//    /**
//     * 오프셋 기반 게시글 목록 조회 요청 모델
//     */
//    @Schema(
//        name = "FetchBoardListByOffsetRequest",
//        description = "오프셋 기반 게시글 목록 조회 요청 모델",
//    )
//    data class Request(
//        @field:Schema(
//            description = "페이지 번호",
//            example = "0",
//            nullable = true,
//            defaultValue = "0",
//        )
//        val page: Int? = null,
//        @field:Schema(
//            description = "페이지 크기",
//            example = "20",
//            nullable = true,
//            defaultValue = "20",
//        )
//        val size: Int? = null,
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
//    /**
//     * 오프셋 기반 게시글 목록 조회 응답 모델
//     */
//    @Schema(
//        name = "FetchBoardListByOffsetResponse",
//        description = "오프셋 기반 게시글 목록 조회 응답 모델",
//    )
//    data class Response(
//        @field:Schema(
//            description = "게시글 목록",
//        )
//        val items: List<BoardItem>,
//        @field:Schema(
//            description = "전체 게시글 수",
//            example = "120",
//            minimum = "0",
//        )
//        val totalCount: Long,
//        @field:Schema(
//            description = "다음 페이지 존재 여부",
//            example = "true",
//        )
//        val hasNext: Boolean,
//    )
//
//    /**
//     * 오프셋 기반 게시글 정보 모델
//     */
//    @Schema(
//        name = "BoardItemByOffset",
//        description = "오프셋 기반 게시글 정보 모델",
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
