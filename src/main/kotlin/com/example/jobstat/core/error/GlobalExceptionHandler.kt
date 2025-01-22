package com.example.jobstat.core.error

import ApiResponse
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(
    private val environment: Environment,
) {
    private val logger = StructuredLogger(this::class.java)
    private val isProd = environment.activeProfiles.contains("dev")

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Unit>> {
        val appException =
            when (ex) {
                is AppException -> ex
                else -> {
                    val appException = ExceptionHandlers.handle(ex)
                    logger.error(appException.message, appException, appException.detailInfo())
                    appException
                }
            }

        if (appException.isServerError()) {
            logger.error("Capture event", appException, appException.detailInfo())
            if(isProd) captureEvent(appException)
        }

        return if (isProd) {
            ApiResponse.fail(appException.httpStatus, appException.message)
        } else {
            ApiResponse.fail(appException.httpStatus, appException.detailInfo().toString())
        }
    }

    private fun captureEvent(ex: AppException) {
        Sentry.withScope { scope ->
            scope.setLevel(SentryLevel.ERROR)
            scope.setExtra("detailInfo", ex.detailInfo().toString())

            val event =
                SentryEvent(ex).apply {
                    message =
                        Message().apply {
                            message = ex.message
                        }
                }

            Sentry.captureEvent(event)
        }
    }
}
