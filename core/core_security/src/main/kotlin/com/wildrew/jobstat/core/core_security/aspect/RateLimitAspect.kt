package com.wildrew.jobstat.core.core_security.aspect

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.annotation.RateLimit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@Aspect
class RateLimitAspect {
    private val requestCountMap = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()

    @Around("@annotation(rateLimit)")
    fun enforceRateLimit(
        joinPoint: ProceedingJoinPoint,
        rateLimit: RateLimit,
    ): Any {
        val key = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
        val now = Instant.now().epochSecond
        val requests = requestCountMap.computeIfAbsent(key) { ConcurrentLinkedQueue() }

        requests.removeIf { timestamp -> now - timestamp > rateLimit.duration }

        if (requests.size >= rateLimit.limit) {
            throw AppException.fromErrorCode(
                ErrorCode.TOO_MANY_REQUESTS,
                "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
            )
        }

        requests.offer(now)

        return joinPoint.proceed()
    }
}
