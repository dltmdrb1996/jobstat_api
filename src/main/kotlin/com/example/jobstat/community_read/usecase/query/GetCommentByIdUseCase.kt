package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.community_read.client.response.CommentReadResponse
import com.example.jobstat.community_read.client.mapper.ResponseMapper
import com.example.jobstat.core.usecase.impl.ValidUseCase
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

    /**
     * 유스케이스 실행
     */
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        try {
            val comment = communityReadService.getCommentById(commentId)
                ?: throw NoSuchElementException("댓글을 찾을 수 없습니다: commentId=$commentId")
            
            Response(comment = ResponseMapper.toResponse(comment))
        } catch (e: Exception) {
            log.error("[GetCommentByIdUseCase] 댓글 상세 조회 실패: commentId={}, error={}", commentId, e.message)
            throw e
        }
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        @field:Positive val commentId: Long
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val comment: CommentReadResponse
    )
}