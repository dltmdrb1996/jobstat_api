package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

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
    validator: Validator,
) : ValidUseCase<GetBoardDetail.Request, GetBoardDetail.Response>(validator) {

    @Transactional
    override fun execute(request: Request): Response {
        // 게시글 정보 조회
        val board = boardService.getBoard(request.boardId)
        val userId = securityUtils.getCurrentUserId()?.toString()

        // 조회수 처리 - 엔티티에서 DB 값 직접 전달
        val viewCount = if (request.incrementViewCount) {
            counterService.incrementViewCount(request.boardId, userId, board.viewCount)
        } else {
            counterService.getViewCount(request.boardId, board.viewCount)
        }

        // 좋아요 정보 조회 - 엔티티에서 DB 값 직접 전달
        val likeCount = counterService.getLikeCount(request.boardId, board.likeCount)
        val userLiked = userId?.let {
            counterService.hasUserLiked(request.boardId, it)
        } ?: false

        // 댓글 페이지 조회
        val commentsPage = getCommentsPage(request.boardId, request.commentPage)

        // 응답 생성
        return Response.from(board, viewCount, likeCount, userLiked, commentsPage)
    }

    private fun getCommentsPage(boardId: Long, commentPage: Int?): Page<DetailCommentResponse> {
        val pageable = commentPage?.let {
            PageRequest.of(it, BoardConstants.DEFAULT_PAGE_SIZE)
        } ?: PageRequest.of(0, BoardConstants.DEFAULT_PAGE_SIZE)

        return commentService
            .getCommentsByBoardId(boardId, pageable)
            .map(DetailCommentResponse.Companion::from)
    }

    data class Request(
        @field:Positive val boardId: Long,
        val commentPage: Int?,
        val incrementViewCount: Boolean = true, // 조회수 증가 여부 (기본값: true)
    )

    data class Response(
        val id: Long,
        val title: String,
        val content: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val userLiked: Boolean,
        val categoryId: Long,
        val createdAt: String,
        val comments: Page<DetailCommentResponse>,
        val commentTotalCount: Long,
        val commentHasNext: Boolean,
    ) {
        companion object {
            fun from(
                board: Board,
                viewCount: Int,
                likeCount: Int,
                userLiked: Boolean,
                comments: Page<DetailCommentResponse>
            ): Response = with(board) {
                Response(
                    id = id,
                    title = title,
                    content = content,
                    author = author,
                    viewCount = viewCount,
                    likeCount = likeCount,
                    userLiked = userLiked,
                    categoryId = category.id,
                    createdAt = createdAt.toString(),
                    comments = comments,
                    commentTotalCount = comments.totalElements,
                    commentHasNext = comments.hasNext(),
                )
            }
        }
    }

    data class DetailCommentResponse(
        val id: Long,
        val content: String,
        val author: String,
        val createdAt: String,
    ) {
        companion object {
            fun from(comment: Comment): DetailCommentResponse = with(comment) {
                DetailCommentResponse(
                    id = id,
                    content = content,
                    author = author,
                    createdAt = createdAt.toString(),
                )
            }
        }
    }
}