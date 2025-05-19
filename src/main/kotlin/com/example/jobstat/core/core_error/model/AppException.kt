package com.example.jobstat.core.core_error.model

import org.springframework.http.HttpStatus
import java.time.LocalDateTime

open class AppException private constructor(
    val errorCode: ErrorCode,
    override val message: String,
    val httpStatus: HttpStatus,
    private val detailInfo: String? = null,
    private val type: AppExceptionType,
) : RuntimeException(message) {
    companion object {
        private val errorCodeMap: Map<ErrorCode, AppException> by lazy {
            ErrorCode.entries.associateWith { code ->
                when (code.type) {
                    AppExceptionType.CLIENT_ERROR -> ClientError(code)
                    AppExceptionType.SERVER_ERROR -> ServerError(code)
                }
            }
        }

        fun fromErrorCode(
            errorCode: ErrorCode,
            message: String? = null,
            detailInfo: String? = null,
        ): AppException =
            errorCodeMap[errorCode]?.copy(message, detailInfo)
                ?: throw IllegalArgumentException("Unknown error code: ${errorCode.code}")
    }

    private val detailInfoMap by lazy {
        mapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to httpStatus.value(),
            "error" to httpStatus.reasonPhrase,
            "message" to message,
            "errorCode" to errorCode.code,
            "detailInfo" to (detailInfo ?: "No additional information"),
        )
    }

    fun isServerError() = type == AppExceptionType.SERVER_ERROR

    fun detailInfo() = detailInfoMap

    fun copy(
        message: String? = null,
        detailInfo: String? = null,
    ): AppException = AppException(errorCode, message ?: this.message, httpStatus, detailInfo ?: this.detailInfo, type)

    class Builder(
        private val errorCode: ErrorCode,
    ) {
        private var message: String = errorCode.defaultMessage
        private var detailInfo: String? = null

        fun message(message: String) = apply { this.message = message }

        fun detailInfo(detailInfo: String?) = apply { this.detailInfo = detailInfo ?: "추가 정보가 없습니다." }

        fun build() = fromErrorCode(errorCode, message, detailInfo)
    }

    class ClientError internal constructor(
        errorCode: ErrorCode,
    ) : AppException(
            errorCode,
            errorCode.defaultMessage,
            errorCode.defaultHttpStatus,
            null,
        AppExceptionType.CLIENT_ERROR,
        )

    class ServerError internal constructor(
        errorCode: ErrorCode,
    ) : AppException(
            errorCode,
            errorCode.defaultMessage,
            errorCode.defaultHttpStatus,
            null,
        AppExceptionType.SERVER_ERROR,
        )
}

enum class AppExceptionType {
    CLIENT_ERROR,
    SERVER_ERROR,
}
