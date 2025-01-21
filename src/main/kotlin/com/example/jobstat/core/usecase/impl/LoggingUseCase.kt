package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.UseCase
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

abstract class LoggingUseCase<in Request : Any, out Response : Any> : UseCase<Request, Response> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    final override operator fun invoke(request: Request): Response =
        try {
            logger.info("Executing use case: ${this.javaClass.simpleName} with request: $request")
//            val startTime = Instant.now()

            validateRequest(request)

            val (response, duration) =
                measureTimedValue {
                    execute(request)
                }

            logger.info("Use case: ${this.javaClass.simpleName} completed in ${duration.inWholeMilliseconds}ms")
            response
        } catch (e: Exception) {
            logger.error("Error executing use case: ${this.javaClass.simpleName}", e)
            throw e
        }

    protected abstract fun execute(request: Request): Response

    protected open fun validateRequest(request: Request) {
    }

    protected fun logInfo(message: String) {
        logger.info(message)
    }

    protected fun logError(
        message: String,
        e: Throwable? = null,
    ) {
        logger.error(message, e)
    }
}
