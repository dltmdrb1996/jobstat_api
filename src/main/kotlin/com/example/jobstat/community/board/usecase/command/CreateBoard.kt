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
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service

@Service
internal class CreateBoard(
    private val boardService: BoardService,
    private val securityUtils: SecurityUtils,
    private val passwordUtil: PasswordUtil,
    private val communityEventPublisher: CommunityEventPublisher,
    validator: Validator,
) : ValidUseCase<CreateBoard.Request, CreateBoard.Response>(validator) {

    @Transactional
    override fun execute(request: Request): Response = run {
        // 사용자 정보 및 비밀번호 검증
        val userId = securityUtils.getCurrentUserId()
        
        // 로그인 상태가 아니면서 비밀번호가 없는 경우 예외 처리
        validatePasswordIfNotLoggedIn(userId, request.password)
        
        // 비밀번호 처리 (로그인 시 null, 비로그인 시 암호화)
        val encodedPassword = processPassword(userId, request.password)

        // 게시글 생성 및 응답 변환
        val createdBoard = boardService.createBoard(
            title = request.title,
            content = request.content,
            author = request.author,
            categoryId = request.categoryId,
            password = encodedPassword,
            userId = userId,
        )

        communityEventPublisher.publishBoardCreated(
            board = createdBoard,
            categoryId = request.categoryId,
            userId = userId
        )

        return Response.from(createdBoard)
    }
    
    private fun validatePasswordIfNotLoggedIn(userId: Long?, password: String?) {
        if (userId == null && password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비로그인 상태에서는 비밀번호 설정이 필수입니다",
            )
        }
    }
    
    private fun processPassword(userId: Long?, password: String?): String? =
        userId?.let { null } ?: password?.let { passwordUtil.encode(it) }

    data class Request(
        @field:Size(
            min = BoardConstants.MIN_TITLE_LENGTH,
            max = BoardConstants.MAX_TITLE_LENGTH,
        )
        val title: String,

        @field:Size(
            min = BoardConstants.MIN_CONTENT_LENGTH,
            max = BoardConstants.MAX_CONTENT_LENGTH,
        )
        val content: String,

        @field:NotBlank
        val author: String,

        @field:NotNull
        val categoryId: Long,

        @field:Size(
            min = BoardConstants.MIN_PASSWORD_LENGTH,
            max = BoardConstants.MAX_PASSWORD_LENGTH,
        )
        val password: String?,
    )

    data class Response(
        val id: Long,
        val title: String,
        val createdAt: String,
    ) {
        companion object {
            fun from(board: Board): Response = with(board) {
                Response(
                    id = id,
                    title = title,
                    createdAt = createdAt.toString(),
                )
            }
        }
    }
}