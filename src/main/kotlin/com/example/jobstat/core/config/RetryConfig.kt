package com.example.jobstat.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
class RetryConfig {
    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()
        val backOffPolicy =
            ExponentialBackOffPolicy().apply {
                initialInterval = 1000L // 첫 재시도 1초 후
                multiplier = 2.0 // 매번 2배씩 증가
                maxInterval = 5000L // 최대 5초 대기
            }
        retryTemplate.setBackOffPolicy(backOffPolicy)

        val retryPolicy =
            SimpleRetryPolicy().apply {
                maxAttempts = 3 // 최대 3회 시도 (원본 호출 포함)
            }
        retryTemplate.setRetryPolicy(retryPolicy)

        return retryTemplate
    }
}
