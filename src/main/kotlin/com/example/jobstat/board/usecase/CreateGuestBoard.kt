package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
class CreateGuestBoard(
    private val boardService: BoardService,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<CreateGuestBoard.Request, CreateGuestBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val encodedPassword = bcryptPasswordUtil.encode(request.password)
        val board =
            boardService.createBoard(
                title = request.title,
                content = request.content,
                author = request.author,
                categoryId = request.categoryId,
                password = encodedPassword,
            )
        return Response(
            id = board.id,
            title = board.title,
        )
    }

    data class Request(
        @field:NotBlank @field:Size(max = 100) val title: String,
        @field:NotBlank @field:Size(max = 5000) val content: String,
        @field:NotBlank val author: String,
        @field:NotBlank
        @field:Pattern(regexp = "^[0-9]{4,12}$", message = "비밀번호는 4-12자리 숫자여야 합니다")
        val password: String,
        @field:NotNull val categoryId: Long,
    )

    data class Response(
        val id: Long,
        val title: String,
    )
}
