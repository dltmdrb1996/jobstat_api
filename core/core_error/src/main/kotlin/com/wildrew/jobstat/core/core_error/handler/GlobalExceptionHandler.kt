package com.wildrew.jobstat.core.core_error.handler // 또는 com.wildrew.jobstat.core.error.logic

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_web_util.ApiResponse
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import java.util.UUID


open class GlobalExceptionHandlerLogic( // 다른 곳에서 상속받아 확장할 여지를 위해 open (선택적)
    private val environment: Environment,
    private val sentryEnabled: Boolean,
    private val showDetailedErrorOnDev: Boolean,
    private val defaultErrorMessage: String,
) {
    private val log: Logger by lazy { LoggerFactory.getLogger(GlobalExceptionHandlerLogic::class.java) }
    private val isProd by lazy { environment.activeProfiles.contains("prod") }
    private val isDev by lazy { environment.activeProfiles.contains("dev") || environment.activeProfiles.isEmpty() }

    // @ExceptionHandler(Exception::class) // 제거, Advice 클래스에서 위임받아 호출
    fun handleExceptionLogic(ex: Exception): ResponseEntity<ApiResponse<Unit>> { // 메소드 이름 변경 및 public
        val errorId = UUID.randomUUID().toString()

        val appException =
            when (ex) {
                is AppException -> ex
                else -> ExceptionHandlers.handle(ex) // ExceptionHandlers는 static object
            }

        val originalMessageForLog = ex.message ?: "N/A"
        val exceptionTypeForLog = ex.javaClass.simpleName

        if (isDev && showDetailedErrorOnDev) {
            log.debug("Full stack trace for errorId [{}]:", errorId, ex)
        }

        val logMessageFormat = "[ErrorID: {}] AppCode={}, HttpStatus={}, AppMsg='{}', OriginalMsg='{}', Detail='{}', ExceptionType={}"
        when {
            appException.isServerError() -> {
                log.error(
                    logMessageFormat,
                    errorId, appException.errorCode.code, appException.httpStatus, appException.message,
                    originalMessageForLog, appException.detailInfo(), exceptionTypeForLog,
                    // 서버 에러일 때만 원본 예외를 로깅 (Sentry가 어차피 전체를 가져감)
                    // ex // 너무 많은 정보를 로깅할 수 있으므로 주의
                )
                if (sentryEnabled && isProd && Sentry.isEnabled()) {
                    captureSentryEvent(appException, errorId, ex, requestInfoForSentry())
                }
            }
            else -> { // ClientError
                log.warn(
                    logMessageFormat,
                    errorId, appException.errorCode.code, appException.httpStatus, appException.message,
                    originalMessageForLog, appException.detailInfo(), exceptionTypeForLog
                )
            }
        }

        val responseMessage = when {
            isProd -> "${appException.message} [Error ID: $errorId]"
            showDetailedErrorOnDev -> {
                val details = appException.detailInfo().toMutableMap()
                details["errorId"] = errorId
                details["exceptionClass"] = ex.javaClass.name
                details["originalMessage"] = originalMessageForLog
                // 실제 스택 트레이스는 응답에 포함하지 않는 것이 좋음
                // details["briefStackTrace"] = ex.stackTraceToString().lines().take(5).joinToString("\\n")
                "Development Mode Error Details: $details"
            }
            else -> "$defaultErrorMessage [Error ID: $errorId]" // 기본 메시지에도 errorId 포함
        }
        return ApiResponse.fail(appException.httpStatus, responseMessage)
    }

    private fun captureSentryEvent(
        appException: AppException,
        errorId: String,
        originalException: Exception,
        requestDetails: Map<String, String?>
    ) {
        Sentry.withScope { scope ->
            scope.level = if (appException.isServerError()) SentryLevel.ERROR else SentryLevel.WARNING
            scope.setExtra("error_id", errorId)
            scope.setExtra("error_code_details", appException.errorCode.toString())
            scope.setExtra("additional_info", appException.detailInfo().toString())

            // 요청 정보 추가 (HttpServletRequest 직접 접근 불가하므로, 필요시 파라미터로 받거나 다른 방식 사용)
            requestDetails["request_uri"]?.let { scope.setExtra("request_uri", it) }
            requestDetails["request_method"]?.let { scope.setExtra("request_method", it) }


            scope.setTag("error_code", appException.errorCode.code)
            scope.setTag("error_type", appException.errorCode.type.name)

            val event = SentryEvent(originalException).apply {
                this.message = Message().apply {
                    this.message = "[${appException.errorCode.code}] ${appException.message} (ID: $errorId)"
                }
                // 핑거프린팅: 동일 유형의 에러를 그룹화하는 데 도움
                this.fingerprints = listOf("{{default}}", appException.errorCode.code, originalException.javaClass.name)
            }
            Sentry.captureEvent(event)
        }
    }

    // Sentry에 보낼 요청 정보를 수집하는 헬퍼 (예시, 실제로는 RequestContextHolder 등 활용)
    // GlobalExceptionHandlerLogic은 HTTP 요청에 직접 접근할 수 없으므로,
    // 필요하다면 Advice 클래스에서 이 정보를 추출하여 넘겨줘야 함.
    private fun requestInfoForSentry(): Map<String, String?> {
        // 실제 구현에서는 RequestContextHolder.getRequestAttributes() 등을 사용해야 하나,
        // 이 클래스는 HTTP 요청 컨텍스트에 직접 접근할 수 없음.
        // 여기서는 빈 맵을 반환하거나, Advice 클래스에서 정보를 받아오도록 설계 변경 필요.
        return emptyMap()
    }
}