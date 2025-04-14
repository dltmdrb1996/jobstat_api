package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.model.CommentResponseDto
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.community_read.utils.CommunityReadConstants
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * 게시글별 댓글 조회 유스케이스
 * 특정 게시글에 달린 댓글 목록을 페이지 단위로 조회
 */
@Service
class GetCommentsByBoardIdUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator,
) : ValidUseCase<GetCommentsByBoardIdUseCase.Request, GetCommentsByBoardIdUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    // ===================================================
    // 유스케이스 실행 메소드
    // ===================================================

    override fun execute(request: Request): Response =
        with(request) {
            log.info("게시글별 댓글 조회 요청: boardId=$boardId, page=$page, size=$size")

            // Pageable 객체 생성
            val pageable = PageRequest.of(page, size)

            // 서비스 계층 호출하여 댓글 목록 조회
            val commentsPage = communityReadService.getCommentsByBoardIdByOffset(boardId, pageable)
            log.info("게시글별 댓글 조회 완료: boardId=$boardId, 댓글 수=${commentsPage.totalElements}개")

            // 응답 생성
            Response(
                items = CommentResponseDto.from(commentsPage.content),
                totalCount = commentsPage.totalElements,
                hasNext = commentsPage.hasNext(),
            )
        }

    // ===================================================
    // 요청 및 응답 모델
    // ===================================================

    /**
     * 요청 데이터 클래스
     */
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
        @field:Positive(message = "페이지 번호는 양수여야 합니다")
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

    /**
     * 응답 데이터 클래스
     */
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
