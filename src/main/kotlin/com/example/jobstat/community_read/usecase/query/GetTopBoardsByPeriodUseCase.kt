package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.response.ResponseMapper
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 기간별(일간/주간/월간) 인기 게시글 조회 유스케이스
 */
@Component
class GetTopBoardsByPeriodUseCase(
    private val communityReadService: CommunityReadService
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional(readOnly = true)
    operator fun invoke(request: Request): Response {
        try {
            log.info("기간별 인기 게시글 조회: type={}, period={}, limit={}", 
                request.type, request.period, request.limit)
            
            val boards = when (request.type.lowercase()) {
                "likes" -> communityReadService.getTopBoardsByLikes(request.period, request.limit)
                "views" -> communityReadService.getTopBoardsByViews(request.period, request.limit)
                else -> throw AppException.fromErrorCode(
                    ErrorCode.INVALID_REQUEST, 
                    message = "유효하지 않은 정렬 기준입니다. 'likes' 또는 'views'만 가능합니다.",
                    detailInfo = "type: ${request.type}"
                )
            }
            
            return Response(
                items = boards.map { ResponseMapper.toResponse(it) },
                totalCount = boards.size.toLong(),
                period = request.period
            )
        } catch (e: Exception) {
            log.error("기간별 인기 게시글 조회 실패: type={}, period={}, error={}", 
                request.type, request.period, e.message, e)
            
            throw when (e) {
                is AppException -> e
                else -> AppException.fromErrorCode(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    message = "인기 게시글 조회 중 오류가 발생했습니다",
                    detailInfo = "type: ${request.type}, period: ${request.period}"
                )
            }
        }
    }

    data class Request(
        val type: String = "likes",  // 'likes' 또는 'views'
        val period: String = "week", // 'day', 'week', 'month'
        val limit: Int = 10
    )

    data class Response(
        val items: List<BoardReadResponse>,
        val totalCount: Long,
        val period: String
    )
} 