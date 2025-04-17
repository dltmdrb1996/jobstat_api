package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.model.CommentResponseDto
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.community_read.utils.CommunityReadConstants
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class GetCommentsByBoardIdUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator,
) : ValidUseCase<GetCommentsByBoardIdUseCase.Request, GetCommentsByBoardIdUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun execute(request: Request): Response =
        with(request) {
            log.info("게시글별 댓글 조회 요청: boardId=$boardId, page=$page, size=$size")

            val pageable = PageRequest.of(page, size)

            val commentsPage = communityReadService.getCommentsByBoardIdByOffset(boardId, pageable)
            log.info("게시글별 댓글 조회 완료: boardId=$boardId, 댓글 수=${commentsPage.totalElements}개")

            Response(
                items = CommentResponseDto.from(commentsPage.content),
                totalCount = commentsPage.totalElements,
                hasNext = commentsPage.hasNext(),
            )
        }

    @Schema(
        name = "GetCommentsByBoardIdRequest",
        description = "게시글별 댓글 목록 조회 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "게시글 ID",
            example = "1",
            required = true,
        )
        @field:Positive(message = "게시글 ID는 양수여야 합니다")
        val boardId: Long,
        @field:Schema(
            description = "페이지 번호",
            example = "0",
            defaultValue = "0",
            minimum = "0",
        )
        @PositiveOrZero
        val page: Int = 0,
        @field:Schema(
            description = "페이지 크기",
            example = "20",
            defaultValue = "20",
            minimum = "1",
            maximum = "100",
        )
        @field:Positive(message = "페이지 크기는 양수여야 합니다")
        val size: Int = CommunityReadConstants.DEFAULT_COMMENT_PAGE_SIZE,
    )

    @Schema(
        name = "GetCommentsByBoardIdResponse",
        description = "게시글별 댓글 목록 조회 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "댓글 목록",
        )
        val items: List<CommentResponseDto>,
        @field:Schema(
            description = "전체 댓글 수",
            example = "42",
            minimum = "0",
        )
        val totalCount: Long,
        @field:Schema(
            description = "다음 페이지 존재 여부",
            example = "true",
        )
        val hasNext: Boolean,
    )
}
