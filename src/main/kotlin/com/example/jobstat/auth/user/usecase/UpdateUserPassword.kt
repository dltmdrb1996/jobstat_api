package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.user.UserConstants
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
    private val passwordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<UpdateUserPassword.Request, Unit>(validator) {
    @Transactional
    override fun execute(request: Request): Unit = with(request) {
        // 사용자 조회 및 현재 비밀번호 검증
        userService.getUserById(userId).let { user ->
            // 현재 비밀번호 확인
            if (!passwordUtil.matches(currentPassword, user.password)) {
                throw AppException.fromErrorCode(
                    ErrorCode.AUTHENTICATION_FAILURE,
                    UserConstants.ErrorMessages.CURRENT_PASSWORD_MISMATCH,
                )
            }
            
            // 새 비밀번호로 업데이트
            userService.updateUser(
                mapOf(
                    "id" to userId,
                    "password" to passwordUtil.encode(newPassword),
                )
            )
        }
    }

    data class Request(
        val userId: Long,
        @field:NotBlank(message = UserConstants.ErrorMessages.PASSWORD_REQUIRED)
        val currentPassword: String,
        @field:NotBlank(message = UserConstants.ErrorMessages.PASSWORD_REQUIRED)
        @field:Pattern(
            regexp = UserConstants.Patterns.PASSWORD_PATTERN,
            message = UserConstants.ErrorMessages.INVALID_PASSWORD,
        )
        val newPassword: String,
    )
}
