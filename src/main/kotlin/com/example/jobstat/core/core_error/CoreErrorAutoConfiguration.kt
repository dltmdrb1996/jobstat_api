package com.example.jobstat.core.core_error

import com.example.jobstat.core.core_error.handler.GlobalExceptionHandlerLogic
import com.example.jobstat.core.core_web_util.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.DispatcherServlet

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet::class, GlobalExceptionHandlerLogic::class)
class CoreErrorAutoConfiguration { // public으로 변경

    private val log = LoggerFactory.getLogger(CoreErrorAutoConfiguration::class.java)

    // GlobalExceptionHandlerLogic Bean 정의 (실제 예외 처리 로직 담당)
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
            effectiveSentryEnabled, showDetailedErrorOnDev, defaultErrorMessage
        )
        return GlobalExceptionHandlerLogic(
            environment,
            effectiveSentryEnabled,
            showDetailedErrorOnDev,
            defaultErrorMessage,
        )
    }

    // @RestControllerAdvice를 가진 내부 Configuration 클래스
    // 이 클래스 자체가 조건부로 활성화됨
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = ["jobstat.core.error.global-handler.enabled"], havingValue = "true", matchIfMissing = true)
    @RestControllerAdvice // 여기에 @RestControllerAdvice
    // @ConditionalOnBean(GlobalExceptionHandlerLogic::class) // 위에서 정의한 Logic Bean이 있을 때만 활성화 (선택적이지만 안전)
    class GlobalExceptionHandlerAdviceActivator { // static inner class
        private val log = LoggerFactory.getLogger(GlobalExceptionHandlerAdviceActivator::class.java)

        // @Autowired // 생성자 주입 권장
        private final val globalExceptionHandlerDelegate: GlobalExceptionHandlerLogic

        // 생성자 주입
        constructor(globalExceptionHandlerDelegate: GlobalExceptionHandlerLogic) {
            this.globalExceptionHandlerDelegate = globalExceptionHandlerDelegate
            log.info("GlobalExceptionHandlerAdviceActivator initialized with delegate: {}", globalExceptionHandlerDelegate)
        }

        @ExceptionHandler(Exception::class)
        fun handleGlobalException(ex: Exception, request: jakarta.servlet.http.HttpServletRequest): ResponseEntity<ApiResponse<Unit>> {
            // GlobalExceptionHandlerLogic에는 HttpServletRequest를 직접 전달할 수 없으므로,
            // Sentry에 필요한 요청 정보를 여기서 추출하여 전달하는 것을 고려할 수 있음.
            // 하지만 Sentry Spring Boot Starter는 자동으로 요청 정보를 수집하므로,
            // 명시적으로 전달할 필요는 없을 수 있음. (Sentry 설정 및 버전 확인 필요)
            // 여기서는 handleExceptionLogic을 직접 호출
            return globalExceptionHandlerDelegate.handleExceptionLogic(ex)
        }
    }

    private fun isSentryAvailable(): Boolean {
        return try {
            Class.forName("io.sentry.Sentry")
            true
        } catch (ex: ClassNotFoundException) {
            false
        }
    }
}