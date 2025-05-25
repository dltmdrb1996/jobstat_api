package com.wildrew.app.auth.email.usecase

import com.wildrew.app.auth.UserEventPublisher
import com.wildrew.app.auth.email.service.EmailVerificationService
import com.wildrew.app.auth.user.service.UserService
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RequestEmailVerification(
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val userEventPublisher: UserEventPublisher,
    validator: Validator,
) : ValidUseCase<RequestEmailVerification.Request, Unit>(validator) {
    @Transactional
    override fun invoke(request: Request) {
        super.invoke(request)
    }

    override fun execute(request: Request): Unit =
        with(request) {
            validateEmailAvailability(email)

            checkPreviousVerification(email)
            val emailVerification = emailVerificationService.create(email)
            val code = emailVerification.code
            val subject = "[JobStat] 이메일 인증"
            val body = "인증 코드: $code\n30분 안에 인증을 완료해주세요."

            userEventPublisher.publishEmailNotification(
                to = email,
                subject = subject,
                body = body,
            )
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

    @Schema(
        name = "RequestEmailVerificationRequest",
        description = "이메일 인증 요청 모델",
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
    )
}
