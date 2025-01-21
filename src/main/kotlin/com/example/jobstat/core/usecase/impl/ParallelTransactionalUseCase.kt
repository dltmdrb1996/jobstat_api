package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.CoroutineUseCase
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

abstract class ParallelTransactionalUseCase<Request : Any, Response : Any> : CoroutineUseCase<Request, Response> {
    protected abstract fun defineOperations(request: Request): List<ParallelOperation<Request, Any>>

    protected abstract fun createResponse(results: List<Any>): Response

    override suspend fun invoke(request: Request): Response =
        coroutineScope {
            val operations = defineOperations(request)
            val results = mutableListOf<Any?>()
            val hasError = AtomicBoolean(false)

            try {
                val deferredResults =
                    operations.map { operation ->
                        async {
                            try {
                                operation.execute(request)
                            } catch (e: Exception) {
                                hasError.set(true)
                                throw e
                            }
                        }
                    }

                results.addAll(deferredResults.awaitAll())

                if (hasError.get()) {
                    throw RuntimeException("One or more operations failed")
                }

                createResponse(results.filterNotNull())
            } catch (e: Exception) {
                // Parallel compensation
                operations
                    .zip(results)
                    .map { (operation, result) ->
                        async { operation.compensate(request, result) }
                    }.awaitAll()

                throw e
            }
        }

    protected fun <Result> createOperation(
        execute: suspend (Request) -> Result,
        compensate: suspend (Request, Result?) -> Unit,
    ): ParallelOperation<Request, Result> = ParallelOperation(execute, compensate)

    protected data class ParallelOperation<Request, Result>(
        val execute: suspend (Request) -> Result,
        val compensate: suspend (Request, Result?) -> Unit,
    )
}
