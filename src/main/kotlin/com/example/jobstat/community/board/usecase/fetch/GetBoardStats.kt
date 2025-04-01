package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetBoardStats(
    private val boardService: BoardService,
    private val commentService: CommentService,
    validator: Validator,
) : ValidUseCase<GetBoardStats.Request, GetBoardStats.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response = with(request) {
        // 작성자의 게시글 수와 댓글 여부 동시에 조회
        val boardCount = boardService.countBoardsByAuthor(author)
        val hasComment = commentService.hasCommentedOnBoard(boardId, author)
        
        // 결과 응답 생성
        Response(
            totalBoardCount = boardCount,
            hasCommentedOnBoard = hasComment
        )
    }

    data class Request(
        @field:NotBlank val author: String,
        @field:Positive val boardId: Long,
    )

    data class Response(
        val totalBoardCount: Long,
        val hasCommentedOnBoard: Boolean,
    )
}
