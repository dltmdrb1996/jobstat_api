package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ValidateBoardTitle(
    private val boardService: BoardService,
    validator: Validator,
) : ValidUseCase<ValidateBoardTitle.Request, ValidateBoardTitle.Response>(validator) {
    @Transactional(readOnly = true)
    override fun execute(request: Request): Response {
        val isDuplicated = boardService.isBoardTitleDuplicated(request.author, request.title)
        return Response(isAvailable = !isDuplicated)
    }

    data class Request(
        @field:NotBlank val author: String,
        @field:NotBlank @field:Size(max = 100) val title: String,
    )

    data class Response(
        val isAvailable: Boolean,
    )
}
