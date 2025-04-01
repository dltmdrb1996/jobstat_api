package com.example.jobstat.auth.email.usecase

import com.example.jobstat.auth.email.service.EmailService
import com.example.jobstat.auth.email.service.EmailVerificationService
import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Service

@Service
internal class RequestEmailVerification(
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val emailService: EmailService,
    validator: Validator,
) : ValidUseCase<RequestEmailVerification.Request, Unit>(validator) {
    @Transactional
    override fun execute(request: Request): Unit = with(request) {
        // 이메일 중복 확인
        validateEmailAvailability(email)
        
        // 이전 인증 코드 확인
        checkPreviousVerification(email)
        
        // 새 인증 코드 생성 및 발송
        emailVerificationService.create(email)
            .also { verification -> emailService.sendVerificationEmail(email, verification.code) }
    }
    
    private fun validateEmailAvailability(email: String) {
        if (!userService.validateEmail(email)) {
            throw AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 이메일입니다.")
        }
    }
    
    private fun checkPreviousVerification(email: String) {
        emailVerificationService.findLatestByEmail(email)?.let { verification ->
            if (verification.isValid()) {
                throw AppException.fromErrorCode(
                    ErrorCode.VERIFICATION_CODE_ALREADY_SENT,
                    "이미 발송된 인증 코드가 있습니다. 잠시 후 다시 시도해주세요.",
                )
            }
        }
    }

    data class Request(
        @field:NotBlank
        @field:Email
        val email: String,
    )
}
