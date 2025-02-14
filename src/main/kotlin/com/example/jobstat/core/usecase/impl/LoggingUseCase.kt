package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.UseCase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

abstract class LoggingUseCase<in Request : Any, out Response : Any> : UseCase<Request, Response> {
    protected val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    final override operator fun invoke(request: Request): Response =
        try {
            log.debug("유스케이스 실행 중: ${this.javaClass.simpleName}, 요청 내용: $request")
//            val startTime = Instant.now()

            validateRequest(request)

            val (response, duration) =
                measureTimedValue {
                    execute(request)
                }

            log.debug("유스케이스: ${this.javaClass.simpleName} 완료, 소요 시간: ${duration.inWholeMilliseconds}ms")
            response
        } catch (e: Exception) {
            log.error("유스케이스 실행 중 오류 발생: ${this.javaClass.simpleName}", e)
            throw e
        }

    protected abstract fun execute(request: Request): Response

    protected open fun validateRequest(request: Request) {
    }

    protected fun logInfo(message: String) {
        log.debug(message)
    }

    protected fun logError(
        message: String,
        e: Throwable? = null,
    ) {
        log.error(message, e)
    }
}
