package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.CoroutineUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

abstract class AsyncUseCase<in Request : Any, out Response : Any> : CoroutineUseCase<Request, Response> {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    final override suspend operator fun invoke(request: Request): Response =
        withContext(Dispatchers.Default) {
            try {
                log.info("비동기 유스케이스 실행 중: ${this@AsyncUseCase.javaClass.simpleName}, 요청 내용: $request")
                val (response, duration) =
                    measureTimedValue {
                        execute(request)
                    }
                log.info(
                    "비동기 유스케이스: ${this@AsyncUseCase.javaClass.simpleName} 완료, 소요 시간: ${duration.inWholeMilliseconds}ms",
                )
                response
            } catch (e: Exception) {
                log.error("비동기 유스케이스 실행 중 오류 발생: ${this@AsyncUseCase.javaClass.simpleName}", e)
                throw e
            }
        }

    protected abstract suspend fun execute(request: Request): Response
}
