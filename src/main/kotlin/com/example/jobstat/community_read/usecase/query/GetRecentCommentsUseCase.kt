package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.CommunityReadConstants
import com.example.jobstat.community_read.client.response.CommentReadResponse
import com.example.jobstat.community_read.client.mapper.ResponseMapper
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 최근 댓글 조회 유스케이스
 */
@Service
class GetRecentCommentsUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<GetRecentCommentsUseCase.Request, GetRecentCommentsUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        val pageable = PageRequest.of(page, CommunityReadConstants.DEFAULT_COMMENT_PAGE_SIZE)
        val commentsPage = communityReadService.getRecentComments(pageable)

        Response(
            comments = commentsPage.content.map { ResponseMapper.toResponse(it) },
            totalCount = commentsPage.totalElements,
            hasNext = commentsPage.hasNext()
        )
    }

    /**
     * 요청 데이터 클래스
     */
    data class Request(
        @field:Positive val page: Int = 0
    )

    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val comments: List<CommentReadResponse>,
        val totalCount: Long,
        val hasNext: Boolean
    )
}