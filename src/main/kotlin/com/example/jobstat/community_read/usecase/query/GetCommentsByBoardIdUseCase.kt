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
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode

/**
 * 게시글별 댓글 조회 유스케이스
 */
@Service
class GetCommentsByBoardIdUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<GetCommentsByBoardIdUseCase.Request, GetCommentsByBoardIdUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 유스케이스 실행
     */
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        try {
            val pageable = PageRequest.of(page, CommunityReadConstants.DEFAULT_COMMENT_PAGE_SIZE)
            val commentsPage = communityReadService.getCommentsByBoardId(boardId, pageable)

            Response(
                items = commentsPage.content.map { ResponseMapper.toResponse(it) },
                totalCount = commentsPage.totalElements,
                hasNext = commentsPage.hasNext()
            )
        } catch (e: Exception) {
            throw AppException.fromErrorCode(
                ErrorCode.INTERNAL_SERVER_ERROR,
                message = "댓글 조회 중 오류가 발생했습니다",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 요청 데이터 클래스
     */
    data class Request(
        @field:Positive val boardId: Long,
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