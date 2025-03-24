package com.example.jobstat.community.board.usecase

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class UpdateBoard(
    private val boardService: BoardService,
    private val passwordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils, // SecurityUtils 추가
    validator: Validator,
) : ValidUseCase<UpdateBoard.ExecuteRequest, UpdateBoard.Response>(validator) {
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
            require(board.userId == currentUserId) { "본인의 게시글만 수정할 수 있습니다" }
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
        @field:NotBlank @field:Size(max = 100) val title: String,
        @field:NotBlank @field:Size(max = 5000) val content: String,
        @field:Size(min = 4, max = 15) val password: String?,
    ) {
        fun of(boardId: Long) =
            ExecuteRequest(
                boardId = boardId,
                title = this.title,
                content = this.content,
                password = this.password,
            )
    }

    // 실제 실행에 사용될 내부 Request 클래스
    data class ExecuteRequest(
        @field:Positive val boardId: Long,
        @field:NotBlank @field:Size(max = 100) val title: String,
        @field:NotBlank @field:Size(max = 5000) val content: String,
        @field:Size(min = 4, max = 15) val password: String?,
    )

    data class Response(
        val id: Long,
        val title: String,
        val content: String,
        val createdAt: String,
        val updatedAt: String,
    )
}
