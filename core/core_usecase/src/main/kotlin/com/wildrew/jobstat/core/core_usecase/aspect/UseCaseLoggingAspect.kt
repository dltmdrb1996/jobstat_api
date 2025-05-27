package com.wildrew.jobstat.core.core_usecase.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoggedUseCase

@Aspect
@Component
class UseCaseLoggingAspect {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @Around("@within(com.wildrew.jobstat.core.core_usecase.aspect.LoggedUseCase)")
    fun logAround(joinPoint: ProceedingJoinPoint): Any {
        val useCaseName = joinPoint.signature.declaringType.simpleName
        val request = joinPoint.args.firstOrNull()

        log.debug("유스케이스 실행 중: $useCaseName, 요청 내용: $request")

        return try {
            val (response, duration) =
                measureTimedValue {
                    joinPoint.proceed()
                }
            log.debug("유스케이스: $useCaseName 완료, 소요 시간: ${duration.inWholeMilliseconds}ms")
            response
        } catch (e: Exception) {
            log.error("유스케이스 실행 중 오류 발생: $useCaseName", e)
            throw e
        }
    }
}
