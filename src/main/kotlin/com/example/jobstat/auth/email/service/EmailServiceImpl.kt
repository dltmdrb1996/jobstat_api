package com.example.jobstat.auth.email.service

import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EmailServiceImpl(
    private val emailSender: JavaMailSender,
    @Value("\${spring.mail.username}") private val fromEmail: String,
) : EmailService {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @Async
    override fun sendVerificationEmail(
        to: String,
        code: String,
    ) {
        try {
            SimpleMailMessage()
                .apply {
                    setFrom(fromEmail)
                    setTo(to)
                    setSubject("[JobStat] 이메일 인증")
                    setText("인증 코드: $code\n30분 안에 인증을 완료해주세요.")
                }.also { message ->
                    emailSender.send(message)
                    log.debug("인증 이메일 발송 완료: $to")
                }
        } catch (e: Exception) {
            log.error("인증 이메일 발송 실패: $to", e)
            throw AppException.fromErrorCode(
                ErrorCode.EMAIL_SENDING_FAILURE,
                "이메일 발송에 실패했습니다.",
            )
        }
    }
}
