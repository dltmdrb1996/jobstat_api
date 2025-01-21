package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.CoroutineUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

abstract class AsyncUseCase<in Request : Any, out Response : Any> : CoroutineUseCase<Request, Response> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    final override suspend operator fun invoke(request: Request): Response =
        withContext(Dispatchers.Default) {
            try {
                logger.info("Executing async use case: ${this@AsyncUseCase.javaClass.simpleName} with request: $request")
                val (response, duration) =
                    measureTimedValue {
                        execute(request)
                    }
                logger.info(
                    "Async use case: ${this@AsyncUseCase.javaClass.simpleName} completed in ${duration.inWholeMilliseconds}ms",
                )
                response
            } catch (e: Exception) {
                logger.error("Error executing async use case: ${this@AsyncUseCase.javaClass.simpleName}", e)
                throw e
            }
        }

    protected abstract suspend fun execute(request: Request): Response
}
