package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.CommunityEventPublisher
import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 게시글 삭제 유스케이스
 * 로그인 사용자는 자신의 글만 삭제 가능
 * 비로그인 사용자는 비밀번호 검증 후 삭제 가능
 */
@Service
internal class DeleteBoard(
    private val boardService: BoardService,
    private val passwordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
    private val communityEventPublisher: CommunityEventPublisher,
    validator: Validator,
) : ValidUseCase<DeleteBoard.ExecuteRequest, DeleteBoard.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun execute(request: ExecuteRequest): Response {
        // 게시글 조회 및 권한 검증
        boardService.getBoard(request.boardId).apply {
            // 접근 권한 검증
            validatePermission(this, request.password)
            
            // 게시글 삭제
            boardService.deleteBoard(id)
            
            // 이벤트 발행
            communityEventPublisher.publishBoardDeleted(
                boardId = id,
                userId = userId?.toString()
            )
        }

        return Response(success = true)
    }

    private fun validatePermission(board: Board, password: String?) {
        board.password?.let { storedPassword -> 
            // 비회원 게시글인 경우
            validatePasswordAccess(board, password)
        } ?: run {
            // 회원 게시글인 경우
            validateMemberAccess(board)
        }
    }

    private fun validatePasswordAccess(board: Board, password: String?) {
        // 비밀번호 필수 체크
        val pwd = password ?: throw AppException.fromErrorCode(
            ErrorCode.AUTHENTICATION_FAILURE,
            "비밀번호가 필요합니다"
        )

        // 비밀번호 일치 검증
        if (!passwordUtil.matches(pwd, board.password!!)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비밀번호가 일치하지 않습니다"
            )
        }

        log.info("[DeleteBoard] Anonymous board ${board.id} deleted with password")
    }

    private fun validateMemberAccess(board: Board) {
        // 현재 사용자 ID 확인
        val currentUserId = securityUtils.getCurrentUserId()
            ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "로그인이 필요합니다"
            )

        when {
            // 관리자는 모든 게시글 삭제 가능
            securityUtils.isAdmin() -> {
                log.info("[DeleteBoard] Admin user $currentUserId deleting board ${board.id}")
            }
            // 일반 사용자는 자신의 게시글만 삭제 가능
            board.userId != currentUserId -> {
                throw AppException.fromErrorCode(
                    ErrorCode.INSUFFICIENT_PERMISSION,
                    "본인의 게시글만 삭제할 수 있습니다"
                )
            }
        }
    }

    data class Request(
        @field:Size(
            min = BoardConstants.MIN_PASSWORD_LENGTH,
            max = BoardConstants.MAX_PASSWORD_LENGTH
        )
        val password: String?,
    ) {
        fun of(boardId: Long): ExecuteRequest = ExecuteRequest(
            boardId = boardId,
            password = password,
        )
    }

    data class ExecuteRequest(
        @field:Positive
        val boardId: Long,

        @field:Size(
            min = BoardConstants.MIN_PASSWORD_LENGTH,
            max = BoardConstants.MAX_PASSWORD_LENGTH
        )
        val password: String?,
    )

    data class Response(
        val success: Boolean,
    )
}