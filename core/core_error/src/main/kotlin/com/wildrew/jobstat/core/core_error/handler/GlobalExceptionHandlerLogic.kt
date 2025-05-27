package com.wildrew.jobstat.core.core_error.handler

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

open class GlobalExceptionHandlerLogic(
    private val environment: Environment,
    private val sentryEnabled: Boolean,
    private val showDetailedErrorOnDev: Boolean,
    private val defaultErrorMessage: String,
) {
    private val log: Logger by lazy { LoggerFactory.getLogger(GlobalExceptionHandlerLogic::class.java) }
    private val isProd by lazy { environment.activeProfiles.contains("prod") }
    private val isDev by lazy { environment.activeProfiles.contains("dev") || environment.activeProfiles.isEmpty() }

    fun handleExceptionLogic(ex: Exception): ResponseEntity<ApiResponse<Unit>> {
        val errorId = UUID.randomUUID().toString()

        val appException =
            when (ex) {
                is AppException -> ex
                else -> ExceptionHandlers.handle(ex)
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
                    errorId,
                    appException.errorCode.code,
                    appException.httpStatus,
                    appException.message,
                    originalMessageForLog,
                    appException.detailInfo(),
                    exceptionTypeForLog,
                )
                if (sentryEnabled && isProd && Sentry.isEnabled()) {
                    captureSentryEvent(appException, errorId, ex, requestInfoForSentry())
                }
            }
            else -> {
                log.warn(
                    logMessageFormat,
                    errorId,
                    appException.errorCode.code,
                    appException.httpStatus,
                    appException.message,
                    originalMessageForLog,
                    appException.detailInfo(),
                    exceptionTypeForLog,
                )
            }
        }

        val responseMessage =
            when {
                isProd -> "${appException.message} [Error ID: $errorId]"
                showDetailedErrorOnDev -> {
                    val details = appException.detailInfo().toMutableMap()
                    details["errorId"] = errorId
                    details["exceptionClass"] = ex.javaClass.name
                    details["originalMessage"] = originalMessageForLog
                    "Development Mode Error Details: $details"
                }
                else -> "$defaultErrorMessage [Error ID: $errorId]"
            }
        return ApiResponse.fail(appException.httpStatus, responseMessage)
    }

    private fun captureSentryEvent(
        appException: AppException,
        errorId: String,
        originalException: Exception,
        requestDetails: Map<String, String?>,
    ) {
        Sentry.withScope { scope ->
            scope.level = if (appException.isServerError()) SentryLevel.ERROR else SentryLevel.WARNING
            scope.setExtra("error_id", errorId)
            scope.setExtra("error_code_details", appException.errorCode.toString())
            scope.setExtra("additional_info", appException.detailInfo().toString())

            requestDetails["request_uri"]?.let { scope.setExtra("request_uri", it) }
            requestDetails["request_method"]?.let { scope.setExtra("request_method", it) }

            scope.setTag("error_code", appException.errorCode.code)
            scope.setTag("error_type", appException.errorCode.type.name)

            val event =
                SentryEvent(originalException).apply {
                    this.message =
                        Message().apply {
                            this.message = "[${appException.errorCode.code}] ${appException.message} (ID: $errorId)"
                        }
                    this.fingerprints = listOf("{{default}}", appException.errorCode.code, originalException.javaClass.name)
                }
            Sentry.captureEvent(event)
        }
    }

    private fun requestInfoForSentry(): Map<String, String?> = emptyMap()
}
