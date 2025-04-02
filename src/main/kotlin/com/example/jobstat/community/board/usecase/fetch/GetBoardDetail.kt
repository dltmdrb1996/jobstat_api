package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.CommunityEventPublisher
import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.core.global.extension.toEpochMilli
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 게시글 상세 조회 유스케이스 (최적화 버전)
 * - DB 중복 호출 최적화 적용
 * - 영속성 컨텍스트 활용하여 성능 향상
 */
@Service
internal class GetBoardDetail(
    private val boardService: BoardService,
    private val commentService: CommentService,
    private val counterService: CounterService,
    private val securityUtils: SecurityUtils,
    private val communityEventPublisher: CommunityEventPublisher,
    validator: Validator,
) : ValidUseCase<GetBoardDetail.Request, GetBoardDetail.Response>(validator) {

    @Transactional
    override fun execute(request: Request): Response {
        val board = boardService.getBoard(request.boardId)
        val userId = securityUtils.getCurrentUserId()?.toString()

        val likeCount = counterService.getLikeCount(board.id, board.likeCount)
        val viewCount = counterService.getViewCount(boardId = board.id, dbViewCount = board.viewCount)
        val userLiked = userId?.let { counterService.hasUserLiked(board.id, it) } ?: false
        return Response.from(board, viewCount, likeCount, userLiked)
    }


    data class Request(
        @field:Positive val boardId: Long,
        val commentPage: Int?,
        val incrementViewCount: Boolean = true, // 조회수 증가 여부 (기본값: true)
    )

    data class Response(
        val id: Long,
        val userId: Long?,
        val categoryId: Long,
        val title: String,
        val content: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val userLiked: Boolean,
        val createdAt: LocalDateTime,
        val eventTs : Long,
    ) {
        companion object {
            fun from(
                board: Board,
                viewCount: Int,
                likeCount: Int,
                userLiked: Boolean,
            ): Response = with(board) {
                Response(
                    id = id,
                    title = title,
                    userId = userId,
                    content = content,
                    author = author,
                    viewCount = viewCount,
                    likeCount = likeCount,
                    commentCount = commentCount,
                    userLiked = userLiked,
                    categoryId = category.id,
                    createdAt = createdAt,
                    eventTs = updatedAt.toEpochMilli(),
                )
            }
        }
    }

    data class DetailCommentResponse(
        val id: Long,
        val content: String,
        val author: String,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            fun from(comment: Comment): DetailCommentResponse = with(comment) {
                DetailCommentResponse(
                    id = id,
                    content = content,
                    author = author,
                    createdAt = createdAt,
                )
            }
        }
    }
}