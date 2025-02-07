package com.example.jobstat.core.usecase.impl

interface SagaUseCase<Request, Response> {
    data class SagaContext<Request>(
        val request: Request,
        val stepResults: MutableMap<String, Any> = mutableMapOf(),
    )

    data class SagaStep<Request>(
        val name: String,
        val execute: (SagaContext<Request>) -> Any,
        val compensate: (SagaContext<Request>) -> Unit,
    )

    fun defineSteps(): List<SagaStep<Request>>

    fun createResponse(context: SagaContext<Request>): Response

    fun execute(request: Request): Response {
        val context = SagaContext(request)
        val executedSteps = mutableListOf<SagaStep<Request>>()

        try {
            defineSteps().forEach { step ->
                val result = step.execute(context)
                context.stepResults[step.name] = result
                executedSteps.add(step)
            }
            return createResponse(context)
        } catch (e: Exception) {
            compensate(context, executedSteps)
            throw e
        }
    }

    fun compensate(
        context: SagaContext<Request>,
        executedSteps: List<SagaStep<Request>>,
    ) {
        executedSteps.reversed().forEach { step ->
            try {
                step.compensate(context)
            } catch (e: Exception) {
                // 보상 단계가 실패해도 다른 보상 단계를 계속 진행
                println("보상 처리 실패 - 단계 ${step.name}: ${e.message}")
            }
        }
    }

    operator fun invoke(request: Request): Result<Response> =
        try {
            Result.success(execute(request))
        } catch (e: Exception) {
            Result.failure(e)
        }
}
