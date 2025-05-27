package com.wildrew.jobstat.community.board.usecase.get

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.common.toEpochMilli
import com.wildrew.jobstat.community.counting.CounterService
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetBoardById(
    private val boardService: BoardService,
    private val counterService: CounterService,
    private val theadContextUtils: TheadContextUtils,
    validator: Validator,
) : ValidUseCase<GetBoardById.Request, GetBoardById.Response>(validator) {
    @Transactional(readOnly = true)
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response {
        val board = boardService.getBoard(request.boardId)
        val userId = theadContextUtils.getCurrentUserId()?.toString()

        val finalCounters =
            counterService.getSingleBoardCounters(
                boardId = board.id,
                userId = userId,
                dbViewCount = board.viewCount,
                dbLikeCount = board.likeCount,
            )

        return Response.from(
            board = board,
            viewCount = finalCounters.viewCount,
            likeCount = finalCounters.likeCount,
            userLiked = finalCounters.userLiked,
        )
    }

    @Schema(
        name = "GetBoardDetailRequest",
        description = "게시글 상세 조회 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "조회할 게시글 ID",
            example = "1",
            required = true,
        )
        val boardId: Long,
    )

    @Schema(
        name = "GetBoardDetailResponse",
        description = "게시글 상세 조회 응답 모델",
    )
    data class Response(
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
            example = "게시글 내용입니다. 반갑습니다!",
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
    ) {
        companion object {
            fun from(
                board: Board,
                viewCount: Int,
                likeCount: Int,
                userLiked: Boolean,
            ): Response =
                with(board) {
                    Response(
                        id = id.toString(),
                        title = title,
                        userId = userId,
                        content = content,
                        author = author,
                        viewCount = viewCount,
                        likeCount = likeCount,
                        commentCount = commentCount,
                        userLiked = userLiked,
                        categoryId = category.id,
                        createdAt = createdAt,
                        eventTs = updatedAt.toEpochMilli(),
                    )
                }
        }
    }
}
