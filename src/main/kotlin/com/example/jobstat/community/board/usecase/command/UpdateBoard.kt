package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.CommunityEventPublisher
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
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
    private val securityUtils: SecurityUtils,
    private val eventPublisher: CommunityEventPublisher,
    validator: Validator,
) : ValidUseCase<UpdateBoard.ExecuteRequest, UpdateBoard.Response>(validator) {

    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        val board = boardService.getBoard(request.boardId)

        // 수정 권한 검증
        validateUpdatePermission(board, request.password)

        // 게시글 업데이트
        val updated = boardService.updateBoard(
            id = request.boardId,
            title = request.title,
            content = request.content
        )

        eventPublisher.publishBoardUpdated(
            board = updated,
        )

        return Response(
            id = updated.id,
            title = updated.title,
            content = updated.content,
            createdAt = updated.createdAt.toString(),
            updatedAt = updated.updatedAt.toString(),
        )
    }

    private fun validateUpdatePermission(board: Board, password: String?) {
        if (board.password != null) {
            validateGuestPermission(board, password)
        } else {
            validateMemberPermission(board)
        }
    }

    private fun validateGuestPermission(board: Board, password: String?) {
        if (password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 필요합니다"
            )
        }

        if (!passwordUtil.matches(password, board.password!!)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 일치하지 않습니다"
            )
        }
    }

    private fun validateMemberPermission(board: Board) {
        val currentUserId = securityUtils.getCurrentUserId()
            ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "로그인이 필요합니다"
            )

        if (board.userId != currentUserId && !securityUtils.isAdmin()) {
            throw AppException.fromErrorCode(
                ErrorCode.INSUFFICIENT_PERMISSION,
                "본인의 게시글만 수정할 수 있습니다"
            )
        }
    }

    data class Request(
        @field:NotBlank
        @field:Size(max = 100)
        val title: String,

        @field:NotBlank
        @field:Size(max = 5000)
        val content: String,

        @field:Size(min = 4, max = 15)
        val password: String?,
    ) {
        fun of(boardId: Long) = ExecuteRequest(
            boardId = boardId,
            title = this.title,
            content = this.content,
            password = this.password,
        )
    }

    data class ExecuteRequest(
        @field:Positive
        val boardId: Long,

        @field:NotBlank
        @field:Size(max = 100)
        val title: String,

        @field:NotBlank
        @field:Size(max = 5000)
        val content: String,

        @field:Size(min = 4, max = 15)
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