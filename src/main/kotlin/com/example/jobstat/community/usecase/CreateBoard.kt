package com.example.jobstat.community.usecase

import com.example.jobstat.community.internal.service.BoardService
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
    private val bcryptPasswordUtil: PasswordUtil,
    private val securityUtils: SecurityUtils,
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
                request.password?.let { bcryptPasswordUtil.encode(it) }
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
        @field:Size(min = 2, max = 100)
        val title: String,
        @field:Size(min = 10, max = 5000)
        val content: String,
        @field:NotBlank
        val author: String,
        @field:NotNull
        val categoryId: Long,
        @field:Size(min = 4, max = 10)
        val password: String?,
    )

    data class Response(
        val id: Long,
        val title: String,
        val createdAt: String,
    )
}
