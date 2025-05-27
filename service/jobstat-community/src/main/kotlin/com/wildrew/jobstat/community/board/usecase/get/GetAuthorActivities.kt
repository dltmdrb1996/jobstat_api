package com.wildrew.jobstat.community.board.usecase.get

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 특정 작성자의 활동(게시글, 댓글) 내역을 조회하는 유스케이스
 * - 작성자가 작성한 게시글 및 댓글 목록을 페이징하여 제공
 * - 작성자 프로필이나 활동 내역 페이지에 적합한 구현
 */
@Service
class GetAuthorActivities(
    private val boardService: BoardService,
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetAuthorActivities.Request, GetAuthorActivities.Response>(validator) {
    @Transactional(readOnly = true)
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response =
        with(request) {
            val pageRequest = PageRequest.of(page ?: 0, 10)

            val boards =
                boardService
                    .getBoardsByAuthor(request.author, pageRequest)
                    .map(BoardActivity.Companion::from)

            val comments =
                commentService
                    .getCommentsByAuthor(request.author, pageRequest)
                    .map(CommentActivity.Companion::from)

            Response.from(boards, comments)
        }

    @Schema(
        name = "GetAuthorActivitiesRequest",
        description = "작성자 활동 조회 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "작성자명",
            example = "홍길동",
            required = true,
        )
        @field:NotBlank(message = "작성자명은 필수입니다")
        val author: String,
        @field:Schema(
            description = "페이지 번호",
            example = "0",
            nullable = true,
            defaultValue = "0",
        )
        val page: Int?,
    )

    @Schema(
        name = "GetAuthorActivitiesResponse",
        description = "작성자 활동 조회 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "작성 게시글 목록",
        )
        val boards: Page<BoardActivity>,
        @field:Schema(
            description = "작성 댓글 목록",
        )
        val comments: Page<CommentActivity>,
        @field:Schema(
            description = "전체 게시글 수",
            example = "25",
            minimum = "0",
        )
        val boardsTotalCount: Long,
        @field:Schema(
            description = "전체 댓글 수",
            example = "42",
            minimum = "0",
        )
        val commentsTotalCount: Long,
        @field:Schema(
            description = "게시글 다음 페이지 존재 여부",
            example = "true",
        )
        val boardsHasNext: Boolean,
        @field:Schema(
            description = "댓글 다음 페이지 존재 여부",
            example = "false",
        )
        val commentsHasNext: Boolean,
    ) {
        companion object {
            fun from(
                boards: Page<BoardActivity>,
                comments: Page<CommentActivity>,
            ): Response =
                Response(
                    boards = boards,
                    comments = comments,
                    boardsTotalCount = boards.totalElements,
                    commentsTotalCount = comments.totalElements,
                    boardsHasNext = boards.hasNext(),
                    commentsHasNext = comments.hasNext(),
                )
        }
    }

    @Schema(
        name = "AuthorBoardActivity",
        description = "작성자 게시글 활동 정보 모델",
    )
    data class BoardActivity(
        @field:Schema(
            description = "게시글 ID",
            example = "1",
        )
        val id: String,
        @field:Schema(
            description = "게시글 제목",
            example = "안녕하세요, 첫 게시글입니다",
        )
        val title: String,
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
            description = "생성 일시",
            example = "2023-05-10T14:30:15.123456",
        )
        val createdAt: String,
    ) {
        companion object {
            fun from(board: Board): BoardActivity =
                with(board) {
                    BoardActivity(
                        id = id.toString(),
                        title = title,
                        viewCount = viewCount,
                        likeCount = likeCount,
                        commentCount = comments.size,
                        createdAt = createdAt.toString(),
                    )
                }
        }
    }

    @Schema(
        name = "AuthorCommentActivity",
        description = "작성자 댓글 활동 정보 모델",
    )
    data class CommentActivity(
        @field:Schema(
            description = "댓글 ID",
            example = "5",
        )
        val id: String,
        @field:Schema(
            description = "댓글 내용",
            example = "좋은 정보 감사합니다!",
        )
        val content: String,
        @field:Schema(
            description = "게시글 ID",
            example = "15",
        )
        val boardId: Long,
        @field:Schema(
            description = "게시글 제목",
            example = "취업 정보 공유합니다",
        )
        val boardTitle: String,
        @field:Schema(
            description = "생성 일시",
            example = "2023-05-11T09:45:22.654321",
        )
        val createdAt: String,
    ) {
        companion object {
            fun from(comment: Comment): CommentActivity =
                with(comment) {
                    CommentActivity(
                        id = id.toString(),
                        content = content,
                        boardId = board.id,
                        boardTitle = board.title,
                        createdAt = createdAt.toString(),
                    )
                }
        }
    }
}
