package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.user.UserConstants
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class UpdateUserPassword(
    private val userService: UserService,
    private val passwordUtil: PasswordUtil,
    validator: Validator,
) : ValidUseCase<UpdateUserPassword.Request, Unit>(validator) {
    @Transactional
    override fun invoke(request: Request) {
        super.invoke(request)
    }

    override fun execute(request: Request): Unit =
        with(request) {
            userService.getUserById(userId).let { user ->
                if (!passwordUtil.matches(currentPassword, user.password)) {
                    throw AppException.fromErrorCode(
                        ErrorCode.AUTHENTICATION_FAILURE,
                        UserConstants.ErrorMessages.CURRENT_PASSWORD_MISMATCH,
                    )
                }

                userService.updateUser(
                    mapOf(
                        "id" to userId,
                        "password" to passwordUtil.encode(newPassword),
                    ),
                )
            }
        }

    @Schema(
        name = "UpdateUserPasswordRequest",
        description = "사용자 비밀번호 변경 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "사용자 ID",
            example = "1",
            required = true,
        )
        @field:Positive(message = "사용자 ID는 양수여야 합니다")
        @field:NotNull(message = "사용자 ID는 필수 값입니다")
        val userId: Long,
        @field:Schema(
            description = "현재 비밀번호",
            example = "Current1234!",
            required = true,
        )
        @field:NotBlank(message = UserConstants.ErrorMessages.PASSWORD_REQUIRED)
        val currentPassword: String,
        @field:Schema(
            description = "새 비밀번호 (영문 대/소문자, 숫자, 특수문자 조합 8~20자)",
            example = "NewPassword1234!",
            pattern = UserConstants.Patterns.PASSWORD_PATTERN,
            required = true,
        )
        @field:NotBlank(message = UserConstants.ErrorMessages.PASSWORD_REQUIRED)
        @field:Pattern(
            regexp = UserConstants.Patterns.PASSWORD_PATTERN,
            message = UserConstants.ErrorMessages.INVALID_PASSWORD,
        )
        val newPassword: String,
    )
}
