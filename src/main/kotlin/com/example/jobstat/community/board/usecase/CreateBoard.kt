package com.example.jobstat.community.board.usecase

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.utils.SecurityUtils
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
    validator: Validator,
) : ValidUseCase<CreateBoard.Request, CreateBoard.Response>(validator) {

    @Transactional
    override fun execute(request: Request): Response {
        val userId = securityUtils.getCurrentUserId()

        // 로그인 상태가 아닐 때만 비밀번호 체크
        if (userId == null && request.password == null) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "비로그인 상태에서는 비밀번호 설정이 필수입니다",
            )
        }

        // 로그인 상태면 비밀번호는 무시하고 null로 설정
        val password =
            if (userId == null) {
                request.password?.let { passwordUtil.encode(it) }
            } else {
                null
            }

        val board =
            boardService.createBoard(
                title = request.title,
                content = request.content,
                author = request.author,
                categoryId = request.categoryId,
                password = password,
                userId = userId,
            )

        return Response(
            id = board.id,
            title = board.title,
            createdAt = board.createdAt.toString(),
        )
    }

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
    )
}
