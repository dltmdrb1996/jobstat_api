package com.wildrew.jobstat.statistics_read.stats.usecase

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.stats.document.*
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import com.wildrew.jobstat.statistics_read.stats.service.StatsAnalysisService
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetStatsByEntityIds(
    private val statsAnalysisService: StatsAnalysisService,
    validator: Validator,
) : ValidUseCase<GetStatsByEntityIds.Request, GetStatsByEntityIds.Response<*>>(validator) {
    @Transactional
    override fun execute(request: Request): Response<*> =
        with(request) {
            // 여러 엔티티 ID에 해당하는 통계 조회
            statsAnalysisService
                .findStatsByEntityIds<BaseStatsDocument>(
                    statsType = statsType,
                    baseDate = baseDate,
                    entityIds = entityIds,
                ).let(com.wildrew.jobstat.statistics_read.stats.usecase.GetStatsByEntityIds::Response)
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
