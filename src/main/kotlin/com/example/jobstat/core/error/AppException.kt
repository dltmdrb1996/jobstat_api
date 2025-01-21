package com.example.jobstat.core.error

import org.springframework.http.HttpStatus
import java.time.LocalDateTime

enum class AppExceptionType {
    CLIENT_ERROR,
    SERVER_ERROR,
}

enum class ErrorCode(
    val code: String,
    val defaultMessage: String,
    val defaultHttpStatus: HttpStatus,
    val type: AppExceptionType,
) {
    // 클라이언트 오류
    MISSING_PARAMETER("C001", "필수 매개변수가 누락되었습니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    INVALID_ARGUMENT("C002", "유효하지 않은 인자가 제공되었습니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    RESOURCE_NOT_FOUND("C003", "리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND, AppExceptionType.CLIENT_ERROR),
    AUTHENTICATION_FAILURE("C004", "인증에 실패했습니다", HttpStatus.UNAUTHORIZED, AppExceptionType.CLIENT_ERROR),
    FORBIDDEN_ACCESS("C005", "접근이 금지되었습니다", HttpStatus.FORBIDDEN, AppExceptionType.CLIENT_ERROR),
    DUPLICATE_RESOURCE("C006", "중복된 리소스입니다", HttpStatus.CONFLICT, AppExceptionType.CLIENT_ERROR),
    INVALID_REQUEST_BODY("C007", "유효하지 않은 요청 본문입니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),
    CONSTRAINT_VIOLATION("C008", "제약 조건 위반입니다", HttpStatus.BAD_REQUEST, AppExceptionType.CLIENT_ERROR),

    // 서버 오류
    INTERNAL_ERROR("S001", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    DATABASE_ERROR("S002", "데이터베이스 작업에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("S003", "외부 서비스 호출에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE, AppExceptionType.SERVER_ERROR),
    SQL_SYNTAX_ERROR("S004", "SQL 구문 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
    TRANSACTION_ERROR("S005", "트랜잭션 처리 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionType.SERVER_ERROR),
}

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
