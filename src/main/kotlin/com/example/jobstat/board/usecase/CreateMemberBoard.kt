package com.example.jobstat.board.usecase

import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class CreateMemberBoard(
    private val userService: UserService,
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<CreateMemberBoard.Request, CreateMemberBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val user = userService.getUserById(request.userId)
        val board =
            boardService.createBoard(
                title = request.title,
                content = request.content,
                categoryId = request.categoryId,
                author = user.email,
                password = null,
                userId = request.userId,
            )
        return Response(
            id = board.id,
            title = board.title,
            createdAt = board.createdAt.toString(),
        )
    }

    data class Request(
        @field:NotBlank @field:Size(max = 100) val title: String,
        @field:NotBlank @field:Size(max = 5000) val content: String,
        @field:NotNull @field:Positive val categoryId: Long,
        @field:NotNull val userId: Long,
    )

    data class Response(
        val id: Long,
        val title: String,
        val createdAt: String,
    )
}
