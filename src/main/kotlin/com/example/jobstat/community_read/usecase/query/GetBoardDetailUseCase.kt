package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.community_read.CommunityReadConstants
import com.example.jobstat.community_read.client.response.BoardReadResponse
import com.example.jobstat.community_read.client.response.CommentReadResponse
import com.example.jobstat.community_read.client.mapper.ResponseMapper
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
 * 게시판 상세 조회 유스케이스
 */
@Service
class GetBoardDetailUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<GetBoardDetailUseCase.Request, GetBoardDetailUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 유스케이스 실행
     */
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        try {
            // 게시글 조회 - 캐시에서 조회 후 없으면 원본에서 fetch
            val board = communityReadService.getBoardById(boardId)
            
            // 댓글 페이징 설정
            val commentPage = commentPage ?: 0
            val commentSize = CommunityReadConstants.DEFAULT_COMMENT_PAGE_SIZE
            val pageable = PageRequest.of(commentPage, commentSize)
            
            // 댓글 조회
            val comments = communityReadService.getCommentsByBoardId(boardId, pageable)
            
            // 조회수 증가 (사용자 ID는 null로 처리)
            communityReadService.incrementBoardViewCount(boardId, null, board.createdAt.toEpochSecond() * 1000)
            
            Response(
                board = ResponseMapper.toResponse(board),
                comments = comments.content.map { ResponseMapper.toResponse(it) },
                commentsTotalCount = comments.totalElements,
                commentsHasNext = comments.hasNext()
            )
        } catch (e: Exception) {
            log.error("[GetBoardDetailUseCase] 게시글 상세 조회 실패: boardId={}, error={}", boardId, e.message)
            throw when (e) {
                is AppException -> e
                else -> AppException.fromErrorCode(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    message = "게시글 조회 중 오류가 발생했습니다",
                    detailInfo = "boardId: $boardId"
                )
            }
        }
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        @field:Positive val boardId: Long,
        val commentPage: Int? = null
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val board: BoardReadResponse,
        val comments: List<CommentReadResponse>,
        val commentsTotalCount: Long,
        val commentsHasNext: Boolean
    )
} 