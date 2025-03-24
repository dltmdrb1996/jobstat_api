package com.example.jobstat.community.comment.usecase

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

// 회원 비회원 로직 분리해야함
@Service
internal class DeleteBoard(
    private val boardService: BoardService,
    private val passwordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
    validator: Validator,
) : ValidUseCase<DeleteBoard.ExecuteRequest, DeleteBoard.Response>(validator) {
    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        val board = boardService.getBoardById(request.boardId)

        if (board.password != null) {
            if (request.password == null) throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
            if (!passwordUtil.matches(request.password, board.password!!)) {
                throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
            }
        } else {
            val currentUserId =
                securityUtils.getCurrentUserId()
                    ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
            require(board.userId == currentUserId) { "본인의 게시글만 삭제할 수 있습니다" }
        }

        boardService.deleteBoard(request.boardId)
        return Response(success = true)
    }

    data class Request(
        @field:Size(min = 4, max = 10)
        val password: String?,
    ) {
        fun of(boardId: Long) =
            ExecuteRequest(
                boardId = boardId,
                password = password,
            )
    }

    data class ExecuteRequest(
        @field:Positive
        val boardId: Long,
        @field:Size(min = 4, max = 10)
        val password: String?,
    )

    data class Response(
        val success: Boolean,
    )
}
