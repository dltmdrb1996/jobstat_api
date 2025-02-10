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

// 이메일 인증 요청 유스케이스
@Service
internal class RequestEmailVerification(
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val emailService: EmailService,
    validator: Validator,
) : ValidUseCase<RequestEmailVerification.Request, Unit>(validator) {
    @Transactional
    override fun execute(request: Request) {
        // 1. 이메일 중복 체크
//        if (!userService.isEmailAvailable(request.email)) {
//            throw AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 이메일입니다.")
//        }

        // 2. 이전 인증 코드 체크
        emailVerificationService.findLatestByEmail(request.email)?.let { verification ->
            if (verification.isValid()) {
                throw AppException.fromErrorCode(
                    ErrorCode.VERIFICATION_CODE_ALREADY_SENT,
                    "이미 발송된 인증 코드가 있습니다. 잠시 후 다시 시도해주세요.",
                )
            }
        }

        // 3. 새 인증 코드 생성
        val verification = emailVerificationService.create(request.email)

        // 4. 이메일 발송
        emailService.sendVerificationEmail(request.email, verification.code)
    }

    data class Request(
        @field:NotBlank
        @field:Email
        val email: String,
    )
}
