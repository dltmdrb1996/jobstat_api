package com.example.jobstat.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync  // @Async 어노테이션 활성화
class MailConfig(
    private val environment: Environment,
) {

    private val isProd = environment.activeProfiles.contains("prod")

    @Bean
    fun javaMailSender(
        @Value("\${spring.mail.host}") host: String,
        @Value("\${spring.mail.port}") port: Int,
        @Value("\${spring.mail.username}") username: String,
        @Value("\${spring.mail.password}") password: String,
    ): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port
        mailSender.username = username
        mailSender.password = password

        val props = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        if(!isProd) props["mail.debug"] = "true"  // 디버그 모드 활성화

        return mailSender
    }
}