package com.example.jobstat.core.core_web_util

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@CommonApiResponseWrapper
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val code: Int,
    val status: HttpStatus,
    val message: String,
    val data: T? = null,
) {
    companion object {
        enum class ResponseMessage(
            val message: String,
        ) {
            SUCCESS("Success"),
            CREATED("Created"),
        }

        fun <T> ok(
            data: T,
            message: String = ResponseMessage.SUCCESS.message,
        ): ResponseEntity<ApiResponse<T>> = createResponse(HttpStatus.OK, data, message)

        fun <T> ok(
            page: Page<T>,
            message: String = ResponseMessage.SUCCESS.message,
        ): ResponseEntity<ApiResponse<Page<T>>> = createResponse(HttpStatus.OK, page, message)

        fun ok(message: String = ResponseMessage.SUCCESS.message): ResponseEntity<ApiResponse<Unit>> =
            createResponse(
                HttpStatus.OK,
                message = message,
            )

        fun <T> create(
            data: T,
            message: String = ResponseMessage.CREATED.message,
        ): ResponseEntity<ApiResponse<T>> = createResponse(HttpStatus.CREATED, data, message)

        fun create(message: String = ResponseMessage.CREATED.message): ResponseEntity<ApiResponse<Unit>> = createResponse(HttpStatus.CREATED, message = message)

        fun fail(
            httpStatus: HttpStatus,
            message: String = httpStatus.reasonPhrase,
        ): ResponseEntity<ApiResponse<Unit>> = createResponse(httpStatus, message = message)

        private fun <T> createResponse(
            status: HttpStatus,
            data: T? = null,
            message: String,
        ): ResponseEntity<ApiResponse<T>> {
            val response = ApiResponse(status.value(), status, message, data)
            return ResponseEntity.status(status).body(response)
        }
    }
}
