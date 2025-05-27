package com.wildrew.jobstat.core.core_error

import com.wildrew.jobstat.core.core_error.handler.GlobalExceptionHandlerLogic
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandlerAdviceActivator(
    private val globalExceptionHandlerDelegate: GlobalExceptionHandlerLogic,
) {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandlerAdviceActivator::class.java)

    init {
        log.info("GlobalExceptionHandlerAdviceActivator initialized and active with delegate: {}", globalExceptionHandlerDelegate)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Unit>> = globalExceptionHandlerDelegate.handleExceptionLogic(ex)
}
