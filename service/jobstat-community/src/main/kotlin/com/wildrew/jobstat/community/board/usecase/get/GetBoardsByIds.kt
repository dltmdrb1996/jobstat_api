package com.wildrew.jobstat.community.board.usecase.get

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.common.toEpochMilli
import com.wildrew.jobstat.community.counting.CounterService
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 여러 게시글을 ID 목록으로 한번에 조회하는 유스케이스
 * - 캐싱된 ID 목록으로 게시글 데이터를 조회할 때 사용
 * - 카운터 서비스를 통해 최신 조회수, 좋아요 수 반영
 */
@Service
class GetBoardsByIds(
    private val theadContextUtils: TheadContextUtils,
    private val boardService: BoardService,
    private val counterService: CounterService,
    validator: Validator,
) : ValidUseCase<GetBoardsByIds.Request, GetBoardsByIds.Response>(validator) {
    override fun execute(request: Request): Response {
        // 현재 사용자 ID 확인
        val userId = theadContextUtils.getCurrentUserIdOrNull()?.toString()

        // ID 목록으로 게시글 조회
        val boards = boardService.getBoardsByIds(request.boardIds)

        if (boards.isEmpty()) {
            return Response(emptyList())
        }

        val boardIdsWithCounts =
            boards.map { board ->
                Triple(board.id, board.viewCount, board.likeCount)
            }

        val countersMap =
            counterService
                .getBulkBoardCounters(boardIdsWithCounts, userId)
                .associateBy { it.boardId }

        val boardItems =
            boards.map { board ->
                val counters = countersMap[board.id] ?: throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED)
                mapToBoardItem(
                    board,
                    viewCount = counters.viewCount,
                    likeCount = counters.likeCount,
                    userLiked = counters.userLiked,
                )
            }

        return Response(boardItems)
    }

    private fun mapToBoardItem(
        board: Board,
        viewCount: Int,
        likeCount: Int,
        userLiked: Boolean,
    ): BoardItem =
        with(board) {
            BoardItem(
                id = id.toString(),
                userId = userId,
                title = title,
                content = content,
                author = author,
                viewCount = viewCount,
                likeCount = likeCount,
                commentCount = commentCount,
                categoryId = category.id,
                createdAt = createdAt,
                userLiked = userLiked,
                eventTs = updatedAt.toEpochMilli(),
            )
        }

    @Schema(
        name = "GetBoardsByIdsRequest",
        description = "다중 게시글 조회 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "조회할 게시글 ID 목록",
            example = "[1, 2, 3, 4, 5]",
            required = true,
        )
        @field:NotEmpty(message = "게시판 ID 목록은 비어있을 수 없습니다")
        val boardIds: List<Long>,
    )

    @Schema(
        name = "GetBoardsByIdsResponse",
        description = "다중 게시글 조회 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "게시글 목록",
        )
        val boards: List<BoardItem>,
    )

    @Schema(
        name = "BoardItemByIds",
        description = "다중 조회 게시글 정보 모델",
    )
    data class BoardItem(
        @field:Schema(
            description = "게시글 ID",
            example = "1",
        )
        val id: String,
        @field:Schema(
            description = "작성자 ID",
            example = "100",
            nullable = true,
        )
        val userId: Long?,
        @field:Schema(
            description = "카테고리 ID",
            example = "2",
        )
        val categoryId: Long,
        @field:Schema(
            description = "게시글 제목",
            example = "안녕하세요, 첫 게시글입니다",
        )
        val title: String,
        @field:Schema(
            description = "게시글 내용",
            example = "게시글 내용입니다. 여기에 자세한 내용을 작성합니다.",
        )
        val content: String,
        @field:Schema(
            description = "작성자",
            example = "홍길동",
        )
        val author: String,
        @field:Schema(
            description = "조회수",
            example = "42",
            minimum = "0",
        )
        val viewCount: Int,
        @field:Schema(
            description = "좋아요 수",
            example = "15",
            minimum = "0",
        )
        val likeCount: Int,
        @field:Schema(
            description = "댓글 수",
            example = "7",
            minimum = "0",
        )
        val commentCount: Int,
        @field:Schema(
            description = "현재 사용자의 좋아요 여부",
            example = "false",
        )
        val userLiked: Boolean,
        @field:Schema(
            description = "생성 일시",
            example = "2023-05-10T14:30:15.123456",
        )
        val createdAt: LocalDateTime,
        @field:Schema(
            description = "이벤트 타임스탬프 (밀리초)",
            example = "1683727815123",
        )
        val eventTs: Long,
    )
}
