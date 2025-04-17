package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.model.CommentResponseDto
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetCommentByIdUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator,
) : ValidUseCase<GetCommentByIdUseCase.Request, GetCommentByIdUseCase.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun execute(request: Request): Response {
        log.info("댓글 상세 조회 요청: commentId=${request.commentId}")

        val comment = communityReadService.getCommentById(request.commentId)
        log.info("댓글 조회 완료: commentId=${request.commentId}")

        return Response(
            comment = CommentResponseDto.from(comment),
        )
    }

    @Schema(
        name = "GetCommentByIdRequest",
        description = "댓글 상세 조회 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "조회할 댓글 ID",
            example = "1",
            required = true,
        )
        @field:Positive(message = "댓글 ID는 양수여야 합니다")
        val commentId: Long,
    )

    @Schema(
        name = "GetCommentByIdResponse",
        description = "댓글 상세 조회 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "댓글 상세 정보",
        )
        val comment: CommentResponseDto,
    )
}
