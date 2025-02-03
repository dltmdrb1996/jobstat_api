package com.example.jobstat.statistics.stats.usecase

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics.stats.registry.StatsType
import com.example.jobstat.statistics.stats.service.StatsAnalysisService
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service

@Service
class GetStatsByEntityIds(
    private val statsAnalysisService: StatsAnalysisService,
    validator: Validator,
) : ValidUseCase<GetStatsByEntityIds.Request, GetStatsByEntityIds.Response<*>>(validator) {
    @Transactional
    override fun execute(request: Request): Response<*> {
        val stats =
            statsAnalysisService.findStatsByEntityIds<BaseStatsDocument>(
                statsType = request.statsType,
                baseDate = request.baseDate,
                entityIds = request.entityIds,
            )
        return Response(stats)
    }

    data class Request(
        @field:NotNull val statsType: StatsType,
        @field:NotNull val baseDate: BaseDate,
        @field:NotEmpty val entityIds: List<Long>,
    )

    data class Response<T : BaseStatsDocument>(
        val stats: List<T>,
    )
}
