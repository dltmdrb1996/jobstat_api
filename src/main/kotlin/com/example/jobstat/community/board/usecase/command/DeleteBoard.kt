package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.SecurityUtils
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 게시글 삭제 유스케이스
 * - 로그인 사용자: 자신의 글만 삭제 가능 (관리자는 모든 글 삭제 가능)
 * - 비로그인 사용자: 비밀번호 검증 후 삭제 가능
 */
@Service
internal class DeleteBoard(
    private val boardService: BoardService,
    private val counterService: CounterService,
    private val passwordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<DeleteBoard.ExecuteRequest, DeleteBoard.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: ExecuteRequest): Response = super.invoke(request)

    override fun execute(request: ExecuteRequest): Response {
        boardService.getBoard(request.boardId).apply {
            // 접근 권한 검증
            validatePermission(this, request.password)

            boardService.deleteBoard(id)

            // 관련 카운터 정보 정리
            counterService.cleanupBoardCounters(id)

            // 삭제 이벤트 발행
            communityCommandEventPublisher.publishBoardDeleted(
                boardId = id,
                categoryId = category.id,
            )
        }

        return Response(success = true)
    }

    private fun validatePermission(
        board: Board,
        password: String?,
    ) {
        board.password?.let { storedPassword ->
            validatePasswordAccess(board, password)
        } ?: run {
            validateMemberAccess(board)
        }
    }

    private fun validatePasswordAccess(
        board: Board,
        password: String?,
    ) {
        val pwd =
            password ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 필요합니다",
            )

        if (!passwordUtil.matches(pwd, board.password!!)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 일치하지 않습니다",
            )
        }

        log.info("비회원 게시글 {} 비밀번호 확인 후 삭제", board.id)
    }

    private fun validateMemberAccess(board: Board) {
        val currentUserId =
            securityUtils.getCurrentUserId()
                ?: throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    "로그인이 필요합니다",
                )

        when {
            securityUtils.isAdmin() -> {
                log.info("관리자 {} 사용자가 게시글 {} 삭제", currentUserId, board.id)
            }
            board.userId != currentUserId -> {
                throw AppException.fromErrorCode(
                    ErrorCode.INSUFFICIENT_PERMISSION,
                    "본인의 게시글만 삭제할 수 있습니다",
                )
            }
        }
    }

    @Schema(
        name = "DeleteBoardRequest",
        description = "게시글 삭제 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "비밀번호 (비회원 게시글 삭제 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = BoardConstants.MIN_PASSWORD_LENGTH,
            maxLength = BoardConstants.MAX_PASSWORD_LENGTH,
        )
        @field:Size(
            min = BoardConstants.MIN_PASSWORD_LENGTH,
            max = BoardConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${BoardConstants.MIN_PASSWORD_LENGTH}~${BoardConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다",
        )
        val password: String?,
    ) {
        /**
         * 실행 요청 객체로 변환
         */
        fun of(boardId: Long): ExecuteRequest =
            ExecuteRequest(
                boardId = boardId,
                password = password,
            )
    }

    @Schema(
        name = "DeleteBoardExecuteRequest",
        description = "게시글 삭제 실행 요청 모델",
    )
    data class ExecuteRequest(
        @field:Schema(
            description = "삭제할 게시글 ID",
            example = "1",
            required = true,
        )
        val boardId: Long,
        @field:Schema(
            description = "비밀번호 (비회원 게시글 삭제 시 필수)",
            example = "password1234",
            nullable = true,
            minLength = BoardConstants.MIN_PASSWORD_LENGTH,
            maxLength = BoardConstants.MAX_PASSWORD_LENGTH,
        )
        @field:Size(
            min = BoardConstants.MIN_PASSWORD_LENGTH,
            max = BoardConstants.MAX_PASSWORD_LENGTH,
            message = "비밀번호는 ${BoardConstants.MIN_PASSWORD_LENGTH}~${BoardConstants.MAX_PASSWORD_LENGTH}자 사이여야 합니다",
        )
        val password: String?,
    )

    @Schema(
        name = "DeleteBoardResponse",
        description = "게시글 삭제 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "삭제 성공 여부",
            example = "true",
        )
        val success: Boolean,
    )
}
