package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.UseCase
import org.springframework.cache.annotation.Cacheable

abstract class CachingUseCase<in Request : Any, out Response : Any> : UseCase<Request, Response> {
    @Cacheable(cacheNames = ["useCaseCache"], key = "#request")
    override fun invoke(request: Request): Response {
        validateRequest(request)
        return execute(request)
    }

    protected abstract fun execute(request: Request): Response

    protected open fun validateRequest(request: Request) {}
}
