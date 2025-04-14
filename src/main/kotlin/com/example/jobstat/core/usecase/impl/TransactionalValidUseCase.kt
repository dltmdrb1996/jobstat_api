package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.usecase.UseCase
import jakarta.validation.ConstraintViolationException
import org.springframework.transaction.annotation.Transactional

abstract class TransactionalValidUseCase<in Request : Any, out Response : Any>(
    private val validator: jakarta.validation.Validator,
) : UseCase<Request, Response> {
    @Transactional
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
