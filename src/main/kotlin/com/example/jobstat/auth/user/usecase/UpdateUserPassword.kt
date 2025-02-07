package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.stereotype.Service

@Service
internal class UpdateUserPassword(
    private val userService: UserService,
    private val bcryptPasswordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<UpdateUserPassword.Request, Unit>(validator) {
    @Transactional
    override fun execute(request: Request) {
        val user = userService.getUserById(request.userId)

        // 현재 패스워드 확인
        if (!bcryptPasswordUtil.matches(request.currentPassword, user.password)) {
            throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "현재 비밀번호가 일치하지 않습니다.",
            )
        }

        userService.updateUser(
            mapOf(
                "id" to request.userId,
                "password" to bcryptPasswordUtil.encode(request.newPassword),
            ),
        )
    }

    data class Request(
        val userId: Long,
        @field:NotBlank
        val currentPassword: String,
        @field:NotBlank
        @field:Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 최소 8자 이상이며, 문자, 숫자, 특수문자를 포함해야 합니다.",
        )
        val newPassword: String,
    )
}
