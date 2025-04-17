package com.example.jobstat.core.error

import com.example.jobstat.core.global.wrapper.ApiResponse
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler(
    private val environment: Environment,
) {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    private val isProd = environment.activeProfiles.contains("prod")
    private val isDev = environment.activeProfiles.contains("dev") || environment.activeProfiles.isEmpty()

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Unit>> {
        // 요청 추적을 위한 고유 ID 생성
        val errorId = UUID.randomUUID().toString()
        MDC.put("errorId", errorId)

        val appException =
            when (ex) {
                is AppException -> ex
                else -> {
                    ExceptionHandlers.handle(ex)
                }
            }

        // 에러 타입에 따라 로깅 레벨 조정
        if (isDev) ex.printStackTrace()
        when {
            appException.isServerError() -> {
                log.error(
                    "Server Error [{}]: {} - {}",
                    errorId,
                    appException.message,
                    appException.detailInfo(),
                )
                if (isProd) captureEvent(appException, errorId)
            }
            else -> {
                log.warn(
                    "Client Error [{}]: {} - {}",
                    errorId,
                    appException.message,
                    appException.detailInfo(),
                )
            }
        }

        try {
            // 프로덕션 환경에서는 기본 오류 메시지만 표시
            val errorMessage =
                if (isProd) {
                    "${appException.message} [ErrorID: $errorId]"
                } else {
                    // 개발 환경에서는 상세 정보 표시
                    val details = appException.detailInfo().toMutableMap()
                    details["errorId"] = errorId
                    details["exceptionClass"] = ex.javaClass.name
                    details["stackTrace"] =
                        ex
                            .stackTraceToString()
                            .lines()
                            .take(10)
                            .joinToString("\n")
                    details.toString()
                }

            return ApiResponse.fail(appException.httpStatus, errorMessage)
        } finally {
            // ThreadLocal 자원 정리
            MDC.remove("errorId")
        }
    }

    private fun captureEvent(
        ex: AppException,
        errorId: String,
    ) {
        Sentry.withScope { scope ->
            scope.setLevel(SentryLevel.ERROR)
            scope.setExtra("detailInfo", ex.detailInfo().toString())
            scope.setExtra("errorId", errorId)

            // 오류 타입에 따라 태그 추가
            scope.setTag("errorCode", ex.errorCode.code)
            scope.setTag("errorType", ex.errorCode.type.name)

            val event =
                SentryEvent(ex).apply {
                    message =
                        Message().apply {
                            message = ex.message
                        }
                    // 에러ID 포함하여 식별 용이하게
                    fingerprints = listOf(errorId, ex.errorCode.code)
                }

            Sentry.captureEvent(event)
        }
    }
}
