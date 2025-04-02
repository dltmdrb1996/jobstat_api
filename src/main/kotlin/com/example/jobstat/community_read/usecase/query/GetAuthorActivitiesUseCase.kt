package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.CommunityReadConstants
import com.example.jobstat.community_read.client.mapper.ResponseMapper
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 작성자 활동 조회 유스케이스
 */
@Service
class GetAuthorActivitiesUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<GetAuthorActivitiesUseCase.Request, GetAuthorActivitiesUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 유스케이스 실행
     */
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        // 작성자의 게시글 조회
        val pageable = PageRequest.of(page ?: 0, CommunityReadConstants.DEFAULT_PAGE_SIZE)
        val boardsPage = communityReadService.getBoardsByAuthor(author, pageable)

        // 작성자의 댓글 조회
        val commentsPage = communityReadService.getCommentsByAuthorFromClient(author, pageable)

        Response(
            boards = boardsPage.content.map { ResponseMapper.toResponse(it) },
            comments = commentsPage.content.map { ResponseMapper.toResponse(it) },
            boardsTotalCount = boardsPage.totalElements,
            commentsTotalCount = commentsPage.totalElements,
            boardsHasNext = boardsPage.hasNext(),
            commentsHasNext = commentsPage.hasNext()
        )
    }

    /**
     * 요청 데이터 클래스
     */
    data class Request(
        @field:NotBlank val author: String,
        val page: Int? = null
    )

    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val boards: List<BoardReadResponse>,
        val comments: List<CommentReadResponse>,
        val boardsTotalCount: Long,
        val commentsTotalCount: Long,
        val boardsHasNext: Boolean,
        val commentsHasNext: Boolean
    )
} 