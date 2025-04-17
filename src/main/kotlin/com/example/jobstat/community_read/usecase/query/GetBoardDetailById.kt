package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.event.CommunityReadEventPublisher
import com.example.jobstat.community_read.model.BoardResponseDto
import com.example.jobstat.community_read.model.CommentResponseDto
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetBoardDetailById(
    private val communityReadService: CommunityReadService,
    private val communityReadEventPublisher: CommunityReadEventPublisher,
    validator: Validator,
) : ValidUseCase<GetBoardDetailById.Request, GetBoardDetailById.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response {
        log.info("게시글 상세 조회 요청 시작: boardId=${request.boardId}")

        val board = communityReadService.getBoardByIdWithFetch(request.boardId)
        log.info("게시글 정보 조회 완료: boardId=${request.boardId}, title=${board.title}")

        val commentsPage =
            if (request.includeComments) {
                log.debug("댓글 포함 조회: boardId=${request.boardId}")
                val pageable = PageRequest.of(0, request.commentPageSize)
                communityReadService.getCommentsByBoardIdByOffset(request.boardId, pageable)
            } else {
                null
            }

        try {
            communityReadEventPublisher.publishIncViewed(
                boardId = board.id,
                delta = 1,
            )
        } catch (e: Exception) {
            log.error("게시글 조회수 증가 이벤트 발행 중 오류 발생: boardId=${request.boardId}. 조회 응답은 계속 진행.", e)
        }

        log.info("게시글 상세 조회 처리 완료: boardId=${request.boardId}")
        return Response(
            board = BoardResponseDto.from(board),
            comments = CommentResponseDto.from(commentsPage?.content),
            commentsTotalCount = commentsPage?.totalElements ?: 0,
            commentsCurrentPage = commentsPage?.number ?: 0,
            commentsTotalPages = commentsPage?.totalPages ?: 0,
            commentsHasNext = commentsPage?.hasNext() ?: false,
        )
    }

    @Schema(
        name = "GetBoardDetailByIdRequest",
        description = "게시글 상세 조회 요청 모델",
    )
    data class Request private constructor(
        @field:Schema(
            description = "조회할 게시글 ID",
            example = "1",
            required = true,
        )
        @field:Positive(message = "게시글 ID는 양수여야 합니다")
        val boardId: Long,
        @field:Schema(
            description = "댓글 포함 여부",
            example = "false",
            defaultValue = "false",
        )
        val includeComments: Boolean = false,
        @field:Schema(
            description = "댓글 페이지 크기",
            example = "20",
            defaultValue = "20",
            minimum = "1",
            maximum = "100",
        )
        @field:Positive(message = "댓글 페이지 크기는 양수여야 합니다")
        val commentPageSize: Int = 20,
    ) {
        companion object {
            fun create(
                boardId: Long,
                includeComments: Boolean,
                commentPageSize: Int,
            ): Request =
                Request(
                    boardId = boardId,
                    includeComments = includeComments,
                    commentPageSize = commentPageSize,
                )
        }
    }

    @Schema(
        name = "GetBoardDetailByIdResponse",
        description = "게시글 상세 조회 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "게시글 상세 정보",
        )
        val board: BoardResponseDto,
        @field:Schema(
            description = "댓글 목록",
        )
        val comments: List<CommentResponseDto> = emptyList(),
        @field:Schema(
            description = "전체 댓글 수",
            example = "42",
            minimum = "0",
        )
        val commentsTotalCount: Long = 0,
        @field:Schema(
            description = "현재 댓글 페이지",
            example = "0",
            minimum = "0",
        )
        val commentsCurrentPage: Int = 0,
        @field:Schema(
            description = "전체 댓글 페이지 수",
            example = "5",
            minimum = "0",
        )
        val commentsTotalPages: Int = 0,
        @field:Schema(
            description = "다음 댓글 페이지 존재 여부",
            example = "true",
        )
        val commentsHasNext: Boolean = false,
    )
}
