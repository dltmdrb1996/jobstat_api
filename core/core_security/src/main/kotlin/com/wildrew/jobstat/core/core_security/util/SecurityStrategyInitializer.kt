// 경로: com/wildrew/jobstat/core/core_security/config/SecurityStrategyInitializer.kt
package com.wildrew.jobstat.core.core_security.util

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.security.core.context.SecurityContextHolder

/**
 * ApplicationListener를 사용하여 Spring의 Environment가 완전히 준비된 후 (Config 서버 포함),
 * 하지만 빈 생성이 시작되기 전에 SecurityContextHolder의 전략을 설정합니다.
 * 이 방법은 Config 서버를 사용하는 MSA 환경의 공통 라이브러리에 가장 적합합니다.
 */
class SecurityStrategyInitializer : ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent) {
        val environment: ConfigurableEnvironment = event.environment

        // [핵심] System.getProperty 대신, Config 서버의 값이 포함된 environment에서 프로퍼티를 읽습니다.
        val isVirtualThreadEnabled = environment.getProperty("spring.threads.virtual.enabled", "false").toBoolean()

        if (isVirtualThreadEnabled) {
            log.info("✅ Virtual threads enabled (detected from environment). Setting SecurityContextHolder strategy to MODE_INHERITABLETHREADLOCAL.")
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
        } else {
            log.info("✅ Virtual threads disabled (detected from environment). Using default SecurityContextHolder strategy (MODE_THREADLOCAL).")
        }
    }
}
