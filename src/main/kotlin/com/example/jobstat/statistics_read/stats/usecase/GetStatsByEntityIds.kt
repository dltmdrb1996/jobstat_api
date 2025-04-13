package com.example.jobstat.statistics_read.stats.usecase

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.statistics_read.stats.registry.StatsType
import com.example.jobstat.statistics_read.stats.service.StatsAnalysisService
import org.springframework.transaction.annotation.Transactional
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
    override fun execute(request: Request): Response<*> = with(request) {
        // 여러 엔티티 ID에 해당하는 통계 조회
        statsAnalysisService.findStatsByEntityIds<BaseStatsDocument>(
            statsType = statsType,
            baseDate = baseDate,
            entityIds = entityIds,
        ).let(::Response)
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
