// package com.example.jobstat.core.wrapper
//
// import com.example.jobstat.core.error.AppException
//
//
// sealed class ApiResult<out R> {
//    data class Success<out T>(val response: T) : ApiResult<T>()
//    data class Error(val error: AppException) : ApiResult<Nothing>()
//    override fun toString(): String {
//        return when (this) {
//            is Success<*> -> "Success[data=$response]"
//            is Error -> "error = $error code = ${error.httpStatus.value()} message = ${error.message}"
//        }
//    }
// }
//
// inline fun <T> ApiResult<T>.onSuccess(action: (value: T) -> Unit): ApiResult<T> {
//    if (this is ApiResult.Success) action(response)
//    return this
// }
//
// inline fun <T> ApiResult<T>.onFailure(action: (exception: AppException) -> Unit) {
//    if (this is ApiResult.Error) action(error)
// }
//
// //map
// inline fun <T, R> ApiResult<T>.map(transform: (value: T) -> R): ApiResult<R> {
//    return when (this) {
//        is ApiResult.Success -> ApiResult.Success(transform(response))
//        is ApiResult.Error -> ApiResult.Error(error)
//    }
// }
//
// val ApiResult<*>.succeeded
//    get() = this is ApiResult.Success && response != null
//
// fun <T> ApiResult<T>.successOr(fallback: T): T {
//    return (this as? ApiResult.Success<T>)?.response ?: fallback
// }
//
// fun <T> ApiResult<T>.failBack(fallback: () -> Unit) {
//    if (!this.succeeded) fallback()
// }
//
// val <T> ApiResult<T>.response: T?
//    get() = (this as? ApiResult.Success)?.response
//
// val <T> ApiResult<T>.error: AppException
//    get() = (this as? ApiResult.Error)?.error ?:AppException.ServerError.INTERNAL_ERROR
