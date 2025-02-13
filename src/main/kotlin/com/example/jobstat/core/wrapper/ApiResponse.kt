package com.example.jobstat.core.wrapper

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonUnwrapped
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val code: Int,
    val status: HttpStatus,
    val message: String,
    val data: T? = null,
) {
    companion object {
        private const val SUCCESS_MESSAGE = "Success"
        private const val CREATED_MESSAGE = "Created"

        fun <T> ok(
            data: T,
            message: String = SUCCESS_MESSAGE,
        ): ResponseEntity<ApiResponse<T>> = createResponse(HttpStatus.OK, data, message)

        fun <T> ok(
            page: Page<T>,
            message: String = SUCCESS_MESSAGE,
        ): ResponseEntity<ApiResponse<Page<T>>> = createResponse(HttpStatus.OK, page, message)

        fun ok(message: String = SUCCESS_MESSAGE): ResponseEntity<ApiResponse<Unit>> =
            createResponse(
                HttpStatus.OK,
                message = message,
            )

        fun <T> create(
            data: T,
            message: String = CREATED_MESSAGE,
        ): ResponseEntity<ApiResponse<T>> = createResponse(HttpStatus.CREATED, data, message)

        fun create(message: String = CREATED_MESSAGE): ResponseEntity<ApiResponse<Unit>> = createResponse(HttpStatus.CREATED, message = message)

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
