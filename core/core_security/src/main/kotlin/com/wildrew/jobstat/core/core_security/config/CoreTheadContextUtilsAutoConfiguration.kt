package com.wildrew.jobstat.core.core_security.config

import com.wildrew.jobstat.core.core_security.util.ScopedSecurityContextHolder // ScopedValueTheadContextUtils가 의존
import com.wildrew.jobstat.core.core_security.util.context_util.ScopedValueTheadContextUtils
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils // 인터페이스
import com.wildrew.jobstat.core.core_security.util.context_util.ThreadLocalTheadContextUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@AutoConfiguration
class CoreTheadContextUtilsAutoConfiguration {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 가상 스레드가 활성화된 경우 ScopedValue 기반 SecurityUtils를 제공합니다.
     */
    @Bean("scopedValueTheadContextUtils")
    @ConditionalOnMissingBean(TheadContextUtils::class)
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "true",
        matchIfMissing = false,
    )
    @ConditionalOnClass(ScopedSecurityContextHolder::class)
    fun scopedValuePrimarySecurityUtils(): TheadContextUtils {
        log.info("ScopedValueTheadContextUtils is created.")
        return ScopedValueTheadContextUtils()
    }

    /**
     * 가상 스레드가 비활성화되었거나, 관련 프로퍼티가 없거나,
     * 또는 ScopedValueTheadContextUtils 조건이 맞지 않을 경우 (예: JDK 버전)
     * ThreadLocal 기반 SecurityUtils를 기본으로 제공합니다.
     */
    @Bean("threadLocalTheadContextUtils")
    @Primary
    @ConditionalOnMissingBean(TheadContextUtils::class)
    @ConditionalOnProperty(
        name = ["spring.threads.virtual.enabled"],
        havingValue = "false",
        matchIfMissing = true,
    )
    fun threadLocalPrimarySecurityUtils(): TheadContextUtils {
        log.info("ThreadLocalTheadContextUtils is created.")
        return ThreadLocalTheadContextUtils()
    }
}
