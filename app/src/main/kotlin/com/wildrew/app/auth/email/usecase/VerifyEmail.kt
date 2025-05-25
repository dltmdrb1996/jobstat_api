package com.wildrew.app.auth.email.usecase

import com.wildrew.app.auth.email.service.EmailVerificationService
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VerifyEmail(
    private val emailVerificationService: EmailVerificationService,
    validator: Validator,
) : ValidUseCase<VerifyEmail.Request, Unit>(validator) {
    @Transactional
    override fun invoke(request: Request) {
        super.invoke(request)
    }

    override fun execute(request: Request) {
        emailVerificationService
            .findLatestByEmail(request.email)
            ?.let { verification ->
                if (!verification.isValid()) {
                    throw AppException.fromErrorCode(ErrorCode.VERIFICATION_EXPIRED, "만료된 인증 코드입니다.")
                }

                if (!emailVerificationService.matchesCode(verification, request.code)) {
                    throw AppException.fromErrorCode(ErrorCode.INVALID_VERIFICATION_CODE, "잘못된 인증 코드입니다.")
                }
            } ?: throw AppException.fromErrorCode(ErrorCode.VERIFICATION_NOT_FOUND, "인증 정보를 찾을 수 없습니다.")
    }

    @Schema(
        name = "VerifyEmailRequest",
        description = "이메일 인증 코드 검증 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "인증할 이메일 주소",
            example = "user@example.com",
            required = true,
        )
        @field:NotBlank(message = "이메일은 필수 입력값입니다")
        @field:Email(message = "유효한 이메일 형식이 아닙니다")
        val email: String,
        @field:Schema(
            description = "이메일로 전송된 6자리 인증 코드",
            example = "123456",
            pattern = "^[0-9]{6}$",
            required = true,
        )
        @field:NotBlank(message = "인증 코드는 필수 입력값입니다")
        @field:Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다")
        val code: String,
    )
}
