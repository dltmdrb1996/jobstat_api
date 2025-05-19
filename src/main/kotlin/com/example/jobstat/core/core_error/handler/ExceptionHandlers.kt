package com.example.jobstat.core.core_error.handler

import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.transaction.TransactionSystemException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.client.RestClientException
import org.springframework.web.servlet.NoHandlerFoundException
import java.sql.SQLSyntaxErrorException

object ExceptionHandlers {
    fun handle(ex: Exception): AppException =
        when (ex) {
            is NoHandlerFoundException -> handleNoHandlerFoundException(ex)
            is HttpRequestMethodNotSupportedException -> handleHttpRequestMethodNotSupportedException(ex)
            is MissingServletRequestParameterException -> handleMissingServletRequestParameterException(ex)
            is HttpMessageNotReadableException -> handleHttpMessageNotReadableException(ex)
            is HttpMediaTypeNotSupportedException -> handleHttpMediaTypeNotSupportedException(ex)
            is DataIntegrityViolationException -> handleDataIntegrityViolationException(ex)
            is TransactionSystemException -> handleTransactionSystemException(ex)
            is SQLSyntaxErrorException -> handleSQLSyntaxErrorException(ex)
            is RestClientException -> handleRestClientException(ex)
            is EntityNotFoundException, is JpaObjectRetrievalFailureException -> handleEntityNotFoundException(ex)
            is MethodArgumentNotValidException -> handleMethodArgumentNotValidException(ex)
            is ConstraintViolationException -> handleConstraintViolationException(ex)
            is IllegalArgumentException -> handleIllegalArgumentException(ex)
            else -> handleUnknownException(ex)
        }

    private fun handleNoHandlerFoundException(ex: NoHandlerFoundException): AppException =
        AppException
            .Builder(ErrorCode.RESOURCE_NOT_FOUND)
            .message("요청한 리소스를 찾을 수 없습니다")
            .detailInfo("URL: ${ex.httpMethod} ${ex.requestURL}")
            .build()

    private fun handleHttpRequestMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException): AppException =
        AppException
            .Builder(ErrorCode.INVALID_ARGUMENT)
            .message("지원하지 않는 HTTP 메서드입니다")
            .detailInfo("지원하지 않는 메서드: ${ex.method}")
            .build()

    private fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException): AppException =
        AppException
            .Builder(ErrorCode.MISSING_PARAMETER)
            .message("필수 요청 파라미터가 누락되었습니다")
            .detailInfo("누락된 파라미터: ${ex.parameterName}")
            .build()

    private fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): AppException =
        AppException
            .Builder(ErrorCode.INVALID_REQUEST_BODY)
            .message("잘못된 요청 본문입니다")
            .detailInfo(ex.message)
            .build()

    private fun handleHttpMediaTypeNotSupportedException(ex: HttpMediaTypeNotSupportedException): AppException =
        AppException
            .Builder(ErrorCode.INVALID_ARGUMENT)
            .message("지원하지 않는 미디어 타입입니다")
            .detailInfo("지원하지 않는 미디어 타입: ${ex.contentType}")
            .build()

    private fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): AppException =
        when {
            ex.cause is org.hibernate.exception.ConstraintViolationException -> {
                val cause = ex.cause as org.hibernate.exception.ConstraintViolationException
                when {
                    cause.constraintName?.contains("UK_", ignoreCase = true) == true ->
                        AppException
                            .Builder(ErrorCode.DUPLICATE_RESOURCE)
                            .message("중복된 데이터가 존재합니다")
                            .detailInfo("제약조건: ${cause.constraintName}")
                            .build()
                    else ->
                        AppException
                            .Builder(ErrorCode.INVALID_ARGUMENT)
                            .message("데이터 무결성 제약조건을 위반했습니다")
                            .detailInfo(cause.message)
                            .build()
                }
            }
            else ->
                AppException
                    .Builder(ErrorCode.DATABASE_ERROR)
                    .message("데이터베이스 오류가 발생했습니다")
                    .detailInfo(ex.message)
                    .build()
        }

    private fun handleTransactionSystemException(ex: TransactionSystemException): AppException =
        when (val rootCause = ex.rootCause) {
            is ConstraintViolationException ->
                AppException
                    .Builder(ErrorCode.CONSTRAINT_VIOLATION)
                    .message("데이터 유효성 검사에 실패했습니다")
                    .detailInfo(
                        rootCause.constraintViolations
                            .associate {
                                it.propertyPath.toString() to it.message
                            }.toString(),
                    ).build()
            is org.hibernate.exception.ConstraintViolationException ->
                AppException
                    .Builder(ErrorCode.INVALID_ARGUMENT)
                    .message("데이터 제약조건을 위반했습니다")
                    .detailInfo(rootCause.message)
                    .build()
            else ->
                AppException
                    .Builder(ErrorCode.TRANSACTION_ERROR)
                    .message("트랜잭션 처리 중 오류가 발생했습니다")
                    .detailInfo(ex.message)
                    .build()
        }

    private fun handleSQLSyntaxErrorException(ex: SQLSyntaxErrorException): AppException =
        AppException
            .Builder(ErrorCode.SQL_SYNTAX_ERROR)
            .message("SQL 구문 오류가 발생했습니다")
            .detailInfo(ex.message)
            .build()

    private fun handleRestClientException(ex: RestClientException): AppException =
        AppException
            .Builder(ErrorCode.EXTERNAL_SERVICE_ERROR)
            .message("외부 서비스 호출 중 오류가 발생했습니다")
            .detailInfo(ex.message)
            .build()

    private fun handleEntityNotFoundException(ex: Exception): AppException {
        val (resourceType, resourceId) =
            when (ex) {
                is JpaObjectRetrievalFailureException -> {
                    extractEntityInfo(ex.cause?.message)
                        ?: (ex.persistentClass?.simpleName to ex.identifier?.toString())
                }
                is EntityNotFoundException -> extractEntityInfo(ex.message) ?: ("알 수 없음" to "알 수 없음")
                else -> "알 수 없음" to "알 수 없음"
            }

        return AppException
            .Builder(ErrorCode.RESOURCE_NOT_FOUND)
            .message("리소스를 찾을 수 없습니다")
            .detailInfo("리소스 유형: $resourceType, ID: $resourceId")
            .build()
    }

    private fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): AppException {
        val errors = ex.bindingResult.fieldErrors
        return handleValidationErrors(errors.map { ValidationError(it.field, it.defaultMessage, it.code) })
    }

    private fun handleConstraintViolationException(ex: ConstraintViolationException): AppException {
        val errors =
            ex.constraintViolations.map {
                ValidationError(
                    it.propertyPath.toString(),
                    it.message,
                    it.constraintDescriptor.annotation.annotationClass.simpleName,
                )
            }
        return handleValidationErrors(errors)
    }

    private data class ValidationError(
        val field: String,
        val message: String?,
        val code: String?,
    )

    private fun handleValidationErrors(errors: List<ValidationError>): AppException {
        val invalidFields = errors.associate { it.field to (it.message ?: "잘못된 입력") }
        val missingParameters = errors.filter { it.code in listOf("NotEmpty", "NotNull", "NotBlank") }.map { it.field }

        return when {
            missingParameters.isNotEmpty() ->
                AppException
                    .Builder(ErrorCode.MISSING_PARAMETER)
                    .message("필수 매개변수가 누락되었습니다")
                    .detailInfo("누락된 파라미터: ${missingParameters.joinToString(", ")}")
                    .build()
            invalidFields.isNotEmpty() ->
                AppException
                    .Builder(ErrorCode.INVALID_ARGUMENT)
                    .message("유효하지 않은 인자가 제공되었습니다")
                    .detailInfo(invalidFields.toString())
                    .build()
            else ->
                AppException
                    .Builder(ErrorCode.INVALID_ARGUMENT)
                    .message("유효성 검사 오류가 발생했습니다")
                    .detailInfo("알 수 없는 유효성 검사 오류")
                    .build()
        }
    }

    private fun handleIllegalArgumentException(ex: IllegalArgumentException): AppException =
        AppException
            .Builder(ErrorCode.INVALID_ARGUMENT)
            .message("잘못된 인자가 제공되었습니다")
            .detailInfo(ex.message)
            .build()

    private fun handleUnknownException(ex: Exception): AppException =
        AppException
            .Builder(ErrorCode.INTERNAL_ERROR)
            .message("오류가 발생했습니다")
            .detailInfo("${ex.javaClass.simpleName}: ${ex.message}")
            .build()

    private fun extractEntityInfo(message: String?): Pair<String, String>? =
        message?.split(":")?.let {
            if (it.size == 2) it[0] to it[1] else null
        }
}
