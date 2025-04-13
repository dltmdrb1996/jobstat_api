package com.example.jobstat.core.usecase

import jakarta.transaction.Transactional

interface UseCase<in Request : Any, out Response : Any> {
    fun invoke(request: Request): Response
}

interface CoroutineUseCase<in Request, out Response> {
    suspend operator fun invoke(request: Request): Response
}

interface TransactionalUseCase<in Request : Any, out Response : Any> {
    @Transactional
    fun invoke(request: Request): Response
}