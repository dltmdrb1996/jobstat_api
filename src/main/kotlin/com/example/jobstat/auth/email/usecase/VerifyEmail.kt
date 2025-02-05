package com.example.jobstat.auth.email.usecase

import com.example.jobstat.auth.email.service.EmailVerificationService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.stereotype.Service

@Service
internal class VerifyEmail(
    private val emailVerificationService: EmailVerificationService,
    validator: Validator,
) : ValidUseCase<VerifyEmail.Request, Unit>(validator) {

    @Transactional
    override fun execute(request: Request) {
        val verification = emailVerificationService.findLatestByEmail(request.email)
            ?: throw AppException.fromErrorCode(ErrorCode.VERIFICATION_NOT_FOUND, "인증 정보를 찾을 수 없습니다.")

        if (!verification.isValid()) {
            throw AppException.fromErrorCode(ErrorCode.VERIFICATION_EXPIRED, "만료된 인증 코드입니다.")
        }

        if (!emailVerificationService.matchesCode(verification, request.code)) {
            throw AppException.fromErrorCode(ErrorCode.INVALID_VERIFICATION_CODE, "잘못된 인증 코드입니다.")
        }
    }

    data class Request(
        @field:NotBlank
        @field:Email
        val email: String,

        @field:NotBlank
        @field:Pattern(regexp = "^[0-9]{6}$")
        val code: String,
    )
}