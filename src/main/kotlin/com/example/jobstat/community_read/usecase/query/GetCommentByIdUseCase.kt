package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.model.CommentResponseDto
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 댓글 상세 조회 유스케이스
 */
@Service
class GetCommentByIdUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<GetCommentByIdUseCase.Request, GetCommentByIdUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun execute(request: Request): Response {
        return Response(
            comment = CommentResponseDto.from(communityReadService.getCommentById(request.commentId))
        )
    }
    
    /**
     * 요청 데이터 클래스
     */
    @Schema(
        name = "GetCommentByIdRequest",
        description = "댓글 상세 조회 요청 모델"
    )
    data class Request(
        @field:Schema(
            description = "조회할 댓글 ID", 
            example = "1",
            required = true
        )
        @field:Positive(message = "댓글 ID는 양수여야 합니다") 
        val commentId: Long
    )
    
    /**
     * 응답 데이터 클래스
     */
    @Schema(
        name = "GetCommentByIdResponse",
        description = "댓글 상세 조회 응답 모델"
    )
    data class Response(
        @field:Schema(
            description = "댓글 상세 정보"
        )
        val comment: CommentResponseDto
    )
}