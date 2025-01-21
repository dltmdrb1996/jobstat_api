package com.example.jobstat.core.usecase

interface UseCase<in Request : Any, out Response : Any> {
    fun invoke(request: Request): Response
}

interface CoroutineUseCase<in Request, out Response> {
    suspend operator fun invoke(request: Request): Response
}
