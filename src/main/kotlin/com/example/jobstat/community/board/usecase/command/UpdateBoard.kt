package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.example.jobstat.core.core_security.util.PasswordUtil
import com.example.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 게시글 수정 유스케이스
 * - 로그인 사용자: 자신의 글만 수정 가능 (관리자는 모든 글 수정 가능)
 * - 비로그인 사용자: 비밀번호 검증 후 수정 가능
 */
@Service
internal class UpdateBoard(
    private val boardService: BoardService,
    private val passwordUtil: PasswordUtil,
    private val theadContextUtils: TheadContextUtils,
    private val eventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<UpdateBoard.ExecuteRequest, UpdateBoard.Response>(validator) {
    @Transactional
    override fun invoke(request: ExecuteRequest): Response = super.invoke(request)

    override fun execute(request: ExecuteRequest): Response {
        val board = boardService.getBoard(request.boardId)

        // 수정 권한 검증
        validateUpdatePermission(board, request.password)

        val updated =
            boardService.updateBoard(
                id = request.boardId,
                title = request.title,
                content = request.content,
            )

        // 수정 이벤트 발행
        eventPublisher.publishBoardUpdated(
            board = updated,
        )

        return Response(
            id = updated.id.toString(),
            title = updated.title,
            content = updated.content,
            createdAt = updated.createdAt.toString(),
            updatedAt = updated.updatedAt.toString(),
        )
    }

    private fun validateUpdatePermission(
        board: Board,
        password: String?,
    ) {
        if (board.password != null) {
            validateGuestPermission(board, password)
        } else {
            validateMemberPermission(board)
        }
    }

    private fun validateGuestPermission(
        board: Board,
        password: String?,
    ) {
        if (password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 필요합니다",
            )
        }

        if (!passwordUtil.matches(password, board.password!!)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 일치하지 않습니다",
            )
        }
    }

    private fun validateMemberPermission(board: Board) {
        val currentUserId =
            theadContextUtils.getCurrentUserId()
                ?: throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    "로그인이 필요합니다",
                )

        if (board.userId != currentUserId && !theadContextUtils.isAdmin()) {
            throw AppException.fromErrorCode(
                ErrorCode.INSUFFICIENT_PERMISSION,
                "본인의 게시글만 수정할 수 있습니다",
            )
        }
    }

    @Schema(
        name = "UpdateBoardRequest",
        description = "게시글 수정 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "게시글 제목",
            example = "수정된 게시글 제목입니다",
            required = true,
            minLength = 1,
            maxLength = 100,
        )
        @field:NotBlank(message = "제목은 필수입니다")
        @field:Size(
            max = 100,
            message = "제목은 최대 100자까지 입력 가능합니다",
        )
        val title: String,
        @field:Schema(
            description = "게시글 내용",
            example = "수정된 게시글 내용입니다. 자세한 내용을 작성합니다.",
            required = true,
            minLength = 1,
            maxLength = 5000,
        )
        @field:NotBlank(message = "내용은 필수입니다")
        @field:Size(
            max = 5000,
            message = "내용은 최대 5000자까지 입력 가능합니다",
        )
        val content: String,
        @field:Schema(
            description = "비밀번호 (비회원 게시글 수정 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = 4,
            maxLength = 15,
        )
        @field:Size(
            min = 4,
            max = 15,
            message = "비밀번호는 4~15자 사이여야 합니다",
        )
        val password: String?,
    ) {
        fun of(boardId: Long) =
            ExecuteRequest(
                boardId = boardId,
                title = this.title,
                content = this.content,
                password = this.password,
            )
    }

    @Schema(
        name = "UpdateBoardExecuteRequest",
        description = "게시글 수정 실행 요청 모델",
    )
    data class ExecuteRequest(
        @field:Schema(
            description = "수정할 게시글 ID",
            example = "1",
            required = true,
        )
        val boardId: Long,
        @field:Schema(
            description = "게시글 제목",
            example = "수정된 게시글 제목입니다",
            required = true,
            minLength = 1,
            maxLength = 100,
        )
        @field:NotBlank(message = "제목은 필수입니다")
        @field:Size(
            max = 100,
            message = "제목은 최대 100자까지 입력 가능합니다",
        )
        val title: String,
        @field:Schema(
            description = "게시글 내용",
            example = "수정된 게시글 내용입니다. 자세한 내용을 작성합니다.",
            required = true,
            minLength = 1,
            maxLength = 5000,
        )
        @field:NotBlank(message = "내용은 필수입니다")
        @field:Size(
            max = 5000,
            message = "내용은 최대 5000자까지 입력 가능합니다",
        )
        val content: String,
        @field:Schema(
            description = "비밀번호 (비회원 게시글 수정 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = 4,
            maxLength = 15,
        )
        @field:Size(
            min = 4,
            max = 15,
            message = "비밀번호는 4~15자 사이여야 합니다",
        )
        val password: String?,
    )

    /**
     * 게시글 수정 응답 모델
     */
    @Schema(
        name = "UpdateBoardResponse",
        description = "게시글 수정 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "게시글 ID",
            example = "1",
        )
        val id: String,
        @field:Schema(
            description = "게시글 제목",
            example = "수정된 게시글 제목입니다",
        )
        val title: String,
        @field:Schema(
            description = "게시글 내용",
            example = "수정된 게시글 내용입니다. 자세한 내용을 작성합니다.",
        )
        val content: String,
        @field:Schema(
            description = "생성 일시",
            example = "2023-05-10T14:30:15.123456",
        )
        val createdAt: String,
        @field:Schema(
            description = "수정 일시",
            example = "2023-05-11T09:45:22.654321",
        )
        val updatedAt: String,
    )
}
