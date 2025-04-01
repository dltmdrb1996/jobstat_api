package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetBoardsByIds(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<GetBoardsByIds.Request, GetBoardsByIds.Response>(validator) {

    @Transactional(readOnly = true)
    override fun execute(request: Request): Response =
        boardService.getBoardsByIds(request.boardIds)
            .map(::mapToBoardItem)
            .let { Response(it) }

    private fun mapToBoardItem(board: Board): BoardItem = with(board) {
        BoardItem(
            id = id,
            title = title,
            content = content,
            author = author,
            viewCount = viewCount,
            likeCount = likeCount,
            commentCount = commentCount,
            categoryId = category.id,
            createdAt = createdAt.toString()
        )
    }

    data class Request(
        @field:NotEmpty(message = "게시판 ID 목록은 비어있을 수 없습니다") 
        val boardIds: List<Long>
    )

    data class Response(
        val boards: List<BoardItem>
    )

    data class BoardItem(
        val id: Long,
        val title: String,
        val content: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val categoryId: Long,
        val createdAt: String,
    )
} 