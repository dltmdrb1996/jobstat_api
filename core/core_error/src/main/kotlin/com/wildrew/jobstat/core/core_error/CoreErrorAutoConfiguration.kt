package com.wildrew.jobstat.core.core_error

import com.wildrew.jobstat.core.core_error.handler.GlobalExceptionHandlerLogic
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.web.servlet.DispatcherServlet

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet::class, GlobalExceptionHandlerLogic::class)
class CoreErrorAutoConfiguration {
    private val log = LoggerFactory.getLogger(CoreErrorAutoConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandlerLogic::class)
    fun coreGlobalExceptionHandlerLogic(
        environment: Environment,
        @Value("\${jobstat.core.error.sentry.enabled:true}") sentryEnabledProperty: Boolean,
        @Value("\${jobstat.core.error.show-detailed-error-on-dev:true}") showDetailedErrorOnDev: Boolean,
        @Value("\${jobstat.core.error.default-message:에러가 발생하였습니다.}") defaultErrorMessage: String,
    ): GlobalExceptionHandlerLogic {
        val effectiveSentryEnabled = sentryEnabledProperty && isSentryAvailable()
        log.info(
            "Creating GlobalExceptionHandlerLogic Bean. Sentry Enabled: {}, Show Detailed Error on Dev: {}, Default Error Message: '{}'",
            effectiveSentryEnabled,
            showDetailedErrorOnDev,
            defaultErrorMessage,
        )
        return GlobalExceptionHandlerLogic(
            environment,
            effectiveSentryEnabled,
            showDetailedErrorOnDev,
            defaultErrorMessage,
        )
    }

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandlerAdviceActivator::class)
    @ConditionalOnBean(GlobalExceptionHandlerLogic::class)
    @ConditionalOnProperty(name = ["jobstat.core.error.global-handler.enabled"], havingValue = "true", matchIfMissing = true)
    fun globalExceptionHandlerAdviceActivator(
        globalExceptionHandlerLogic: GlobalExceptionHandlerLogic,
    ): GlobalExceptionHandlerAdviceActivator {
        log.info("Creating GlobalExceptionHandlerAdviceActivator Bean, delegating to: {}", globalExceptionHandlerLogic)
        return GlobalExceptionHandlerAdviceActivator(globalExceptionHandlerLogic)
    }

    private fun isSentryAvailable(): Boolean =
        try {
            Class.forName("io.sentry.Sentry")
            true
        } catch (ex: ClassNotFoundException) {
            false
        }
}
