package com.example.jobstat.core.error

import com.example.jobstat.core.wrapper.ApiResponse
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(
    private val environment: Environment,
) {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    private val isProd = environment.activeProfiles.contains("prod")

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Unit>> {
        val appException =
            when (ex) {
                is AppException -> ex
                else -> {
                    val appException = ExceptionHandlers.handle(ex)
                    log.error(appException.message, appException, appException.detailInfo())
                    appException
                }
            }

        ex.printStackTrace()
        if (appException.isServerError()) {
            log.error("Capture event ${appException.detailInfo()}")
            if (isProd) captureEvent(appException)
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
