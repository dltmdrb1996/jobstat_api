package com.example.jobstat.statistics.stats.usecase

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics.stats.registry.StatsType
import com.example.jobstat.statistics.stats.service.StatsAnalysisService
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class GetStatsByEntityId(
    private val statsAnalysisService: StatsAnalysisService,
    validator: Validator,
) : ValidUseCase<GetStatsByEntityId.Request, GetStatsByEntityId.Response<*>>(validator) {
    @Transactional
    override fun execute(request: Request): Response<*> {
        val stats =
            statsAnalysisService.findStatsByEntityId<BaseStatsDocument>(
                statsType = request.statsType,
                entityId = request.entityId,
            )
        return Response(stats)
    }

    data class Request(
        @field:NotNull val statsType: StatsType,
        @field:Min(1) val entityId: Long,
    )

    data class Response<T : BaseStatsDocument>(
        val stats: List<T>?,
    )
}
