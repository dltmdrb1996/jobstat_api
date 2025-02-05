package com.example.jobstat.core.usecase.impl

import jakarta.validation.Validator
import org.springframework.cache.annotation.Cacheable

abstract class CacheableValidUseCase<in Request : Any, out Response : Any>(
    validator: Validator,
) : ValidUseCase<Request, Response>(validator) {
    override fun execute(request: Request): Response = executeWithCache(request)

    @Cacheable(
        cacheNames = ["statsWithRanking"],
        key = "#request.hashCode()",
        unless = "#result == null",
    )
    protected open fun executeWithCache(request: Request): Response = executeInternal(request)

    protected abstract fun executeInternal(request: Request): Response
}
