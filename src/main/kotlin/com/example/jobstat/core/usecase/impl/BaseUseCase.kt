package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.UseCase
import jakarta.validation.Valid

abstract class BaseUseCase<in Request : Any, out Response : Any> : UseCase<Request, Response> {
    final override operator fun invoke(
        @Valid request: Request,
    ): Response {
        validateRequest(request)
        return execute(request)
    }

    protected abstract fun execute(request: Request): Response

    protected open fun validateRequest(request: Request) {}
}
