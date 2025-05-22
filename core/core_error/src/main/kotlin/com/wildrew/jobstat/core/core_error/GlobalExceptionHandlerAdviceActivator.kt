package com.wildrew.jobstat.core.core_error

import com.wildrew.jobstat.core.core_error.handler.GlobalExceptionHandlerLogic
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter // 추가
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean // 추가
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice // @Configuration은 생략 가능 (RestControllerAdvice가 @Component를 메타 어노테이션으로 가짐)
@ConditionalOnProperty(name = ["jobstat.core.error.global-handler.enabled"], havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(GlobalExceptionHandlerLogic::class) // GlobalExceptionHandlerLogic 빈이 존재할 때만 활성화
@AutoConfigureAfter(CoreErrorAutoConfiguration::class) // GlobalExceptionHandlerLogic 빈이 먼저 생성되도록 순서 명시
class GlobalExceptionHandlerAdviceActivator( // 생성자 주입
    private val globalExceptionHandlerDelegate: GlobalExceptionHandlerLogic
) {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandlerAdviceActivator::class.java)

    init { // 생성자 대신 init 블록에서 로그 출력 (선호에 따라)
        log.info("GlobalExceptionHandlerAdviceActivator initialized with delegate: {}", globalExceptionHandlerDelegate)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiResponse<Unit>> {
        return globalExceptionHandlerDelegate.handleExceptionLogic(ex)
    }
}