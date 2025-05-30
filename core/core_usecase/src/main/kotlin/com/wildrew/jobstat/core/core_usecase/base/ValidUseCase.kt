package com.wildrew.jobstat.core.core_usecase.base

import jakarta.validation.ConstraintViolationException

abstract class ValidUseCase<in Request : Any, out Response : Any>(
    private val validator: jakarta.validation.Validator,
) : UseCase<Request, Response> {
    override operator fun invoke(request: Request): Response {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw ConstraintViolationException(violations)
        }
        validateRequest(request)
        return execute(request)
    }

    protected open fun validateRequest(request: Request) {}

    protected abstract fun execute(request: Request): Response
}
