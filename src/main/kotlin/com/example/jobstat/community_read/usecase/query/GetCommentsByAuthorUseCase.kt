package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.CommunityReadConstants
import com.example.jobstat.community_read.client.response.CommentReadResponse
import com.example.jobstat.community_read.client.mapper.ResponseMapper
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 작성자별 댓글 조회 유스케이스 (원본 소스에서 직접 조회)
 */
@Service
class GetCommentsByAuthorUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<GetCommentsByAuthorUseCase.Request, GetCommentsByAuthorUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 유스케이스 실행
     */
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        val pageable = PageRequest.of(page, CommunityReadConstants.DEFAULT_COMMENT_PAGE_SIZE)

        // 원본 소스(commentClient)에서 작성자별 댓글을 직접 조회
        val comments = communityReadService.getCommentsByAuthorFromClient(authorId, pageable)

        Response(
            items = comments.content.map { ResponseMapper.toResponse(it) },
            totalCount = comments.totalElements,
            hasNext = comments.hasNext()
        )
    }

    /**
     * 요청 데이터 클래스
     */
    data class Request(
        @field:NotBlank val authorId: String,
        @field:Positive val page: Int = 0
    )

    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val items: List<CommentReadResponse>,
        val totalCount: Long,
        val hasNext: Boolean
    )
}