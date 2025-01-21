package com.example.jobstat.board.usecase

import com.example.jobstat.board.BoardService
import com.example.jobstat.board.internal.entity.Board
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.*
import org.springframework.stereotype.Service

@Service
class CreateBoard(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<CreateBoard.Request, CreateBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response = boardService.createBoard(request.title, request.content, request.author).toResponse()

    data class Request(
        @field:NotBlank
        @field:Size(max = 100)
        val title: String,
        @field:NotBlank
        @field:Size(max = 5000)
        val content: String,
        @field:NotBlank
        val author: String,
    )

    data class Response(
        val id: Long,
        val title: String,
        val content: String,
        val author: String,
        val createdAt: String,
    )

    private fun Board.toResponse(): Response =
        Response(
            id = this.id,
            title = this.title,
            content = this.content,
            author = this.author,
            createdAt = this.createdAt.toString(),
        )
}
