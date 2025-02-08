package com.example.jobstat.board.usecase

import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service

// 회원 비회원 로직 분리해야함
@Service
internal class DeleteBoard(
    private val boardService: BoardService,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<DeleteBoard.Request, DeleteBoard.Response>(validator) {
    @Transactional
    override fun execute(request: Request): Response {
        val board = boardService.getBoardById(request.boardId)
        if (board.password != null) {
            require(request.password != null) { "비밀번호는 필수입니다" }
            require(bcryptPasswordUtil.matches(request.password, board.password!!)) { "비밀번호가 일치하지 않습니다" }
        }
        boardService.deleteBoard(request.boardId)
        return Response(success = true)
    }

    data class Request(
        @field:Positive val boardId: Long,
        val password: String?,
    )

    data class Response(
        val success: Boolean,
    )
}
