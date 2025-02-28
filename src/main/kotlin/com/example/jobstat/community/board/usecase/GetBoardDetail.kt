package com.example.jobstat.community.board.usecase

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
internal class GetBoardDetail(
    private val boardService: BoardService,
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetBoardDetail.Request, GetBoardDetail.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        // 증가된 조회수와 함께 게시글을 반환 (incrementViewCount 내부에서 persist)
        val board = boardService.incrementViewCount(request.boardId)
        val comments: Page<DetailCommentResponse> =
            commentService
                .getCommentsByBoardId(
                    boardId = request.boardId,
                    pageable =
                        request.commentPage?.let {
                            PageRequest.of(it, BoardConstants.DEFAULT_PAGE_SIZE)
                        } ?: PageRequest.of(0, BoardConstants.DEFAULT_PAGE_SIZE),
                ).map { comment ->
                    DetailCommentResponse(
                        id = comment.id,
                        content = comment.content,
                        author = comment.author,
                        createdAt = comment.createdAt.toString(),
                    )
                }
        return Response(
            id = board.id,
            title = board.title,
            content = board.content,
            author = board.author,
            viewCount = board.viewCount,
            likeCount = board.likeCount,
            categoryId = board.category.id,
            createdAt = board.createdAt.toString(),
            comments = comments,
            commentTotalCount = comments.totalElements,
            commentHasNext = comments.hasNext(),
        )
    }

    data class Request(
        @field:Positive val boardId: Long,
        val commentPage: Int?,
    )

    data class Response(
        val id: Long,
        val title: String,
        val content: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val categoryId: Long,
        val createdAt: String,
        val comments: Page<DetailCommentResponse>,
        val commentTotalCount: Long,
        val commentHasNext: Boolean,
    )

    data class DetailCommentResponse(
        val id: Long,
        val content: String,
        val author: String,
        val createdAt: String,
    )
}
