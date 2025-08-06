package com.wildrew.jobstat.core.core_security.config

import com.wildrew.jobstat.core.core_security.util.context_util.SecurityContextUtils
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class CoreTheadContextUtilsAutoConfiguration {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Bean
    @ConditionalOnMissingBean(TheadContextUtils::class)
    fun theadContextUtils(): TheadContextUtils = SecurityContextUtils()

//    @Configuration
//    @ConditionalOnProperty(
//        name = ["spring.threads.virtual.enabled"],
//        havingValue = "true",
//        matchIfMissing = false,
//    )
//    @ConditionalOnClass(ScopedSecurityContextHolder::class)
//    class ScopedValueConfig {
//        private val log by lazy { LoggerFactory.getLogger(this::class.java) }
//        @Bean
//        fun scopedValueTheadContextUtils(): TheadContextUtils {
//            return ScopedValueTheadContextUtils()
//        }
//    }
//
//    @Configuration
//    @ConditionalOnProperty(
//        name = ["spring.threads.virtual.enabled"],
//        havingValue = "false",
//        matchIfMissing = true,
//    )
//    class ThreadLocalConfig {
//        private val log by lazy { LoggerFactory.getLogger(this::class.java) }
//        @Bean
//        fun threadLocalTheadContextUtils(): TheadContextUtils {
//            return ThreadLocalTheadContextUtils()
//        }
//    }
}
