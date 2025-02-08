package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class UpdateBoard(
    private val boardService: BoardService,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<UpdateBoard.Request, UpdateBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val board = boardService.getBoardById(request.boardId)
        if (board.password != null) {
            require(request.password != null) { "비밀번호는 필수입니다" }
            require(bcryptPasswordUtil.matches(request.password, board.password!!)) { "비밀번호가 일치하지 않습니다" }
        }
        val updated = boardService.updateBoard(request.boardId, request.title, request.content)
        return Response(
            id = updated.id,
            title = updated.title,
            content = updated.content,
            createdAt = updated.createdAt.toString(),
            updatedAt = updated.updatedAt.toString(),
        )
    }

    data class Request(
        @field:Positive val boardId: Long,
        @field:NotBlank @field:Size(max = 100) val title: String,
        @field:NotBlank @field:Size(max = 5000) val content: String,
        val password: String?,
    )

    data class Response(
        val id: Long,
        val title: String,
        val content: String,
        val createdAt: String,
        val updatedAt: String,
    )
}
