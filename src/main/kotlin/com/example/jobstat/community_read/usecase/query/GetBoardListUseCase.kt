package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.client.response.BoardReadResponse
import com.example.jobstat.community_read.response.ResponseMapper
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.constants.CommunityReadConstants
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 게시판 목록 조회 유스케이스
 */
@Component
class GetBoardListUseCase(
    private val communityReadService: CommunityReadService
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional(readOnly = true)
    operator fun invoke(request: Request): Response {
        try {
            val page = request.page ?: 0
            val size = request.size ?: CommunityReadConstants.DEFAULT_PAGE_SIZE
            val pageable = PageRequest.of(page, size)
            
            // 조회 타입에 따른 분기 처리
            val boardsPage = when {
                // 카테고리별 게시글 조회
                request.categoryId != null -> {
                    log.info("카테고리별 게시글 조회: categoryId={}", request.categoryId)
                    communityReadService.getBoardsByCategory(request.categoryId, pageable)
                }
                // 작성자별 게시글 조회
                request.author != null -> {
                    log.info("작성자별 게시글 조회: author={}", request.author)
                    communityReadService.getBoardsByAuthor(request.author, pageable)
                }
                // 검색 조회
                request.keyword != null -> {
                    log.info("키워드 검색 게시글 조회: keyword={}", request.keyword)
                    communityReadService.searchBoards(request.keyword, pageable)
                }
                // 최신 게시글 조회 (기본)
                else -> {
                    log.info("최신 게시글 조회: page={}, size={}", page, size)
                    communityReadService.getAllBoards(pageable)
                }
            }
            
            return Response(
                items = boardsPage.content.map { ResponseMapper.toResponse(it) },
                totalCount = boardsPage.totalElements,
                hasNext = boardsPage.hasNext()
            )
        } catch (e: Exception) {
            log.error("게시글 목록 조회 실패: error={}", e.message, e)
            throw AppException.fromErrorCode(
                ErrorCode.INTERNAL_SERVER_ERROR,
                message = "게시글 목록 조회 중 오류가 발생했습니다"
            )
        }
    }

    data class Request(
        val page: Int? = null,
        val size: Int? = null,
        val categoryId: Long? = null,
        val author: String? = null,
        val keyword: String? = null
    )

    data class Response(
        val items: List<BoardReadResponse>,
        val totalCount: Long,
        val hasNext: Boolean
    )
}