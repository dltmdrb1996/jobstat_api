package com.wildrew.jobstat.statistics_read.stats.usecase

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.stats.document.*
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import com.wildrew.jobstat.statistics_read.stats.service.StatsAnalysisService
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetStatsByEntityIdAndBaseDate(
    private val statsAnalysisService: StatsAnalysisService,
    validator: Validator,
) : ValidUseCase<GetStatsByEntityIdAndBaseDate.Request, GetStatsByEntityIdAndBaseDate.Response<*>>(validator) {
    @Transactional
    override fun execute(request: Request): Response<*> =
        with(request) {
            // 엔티티 ID와 기준일자에 해당하는 통계 조회
            statsAnalysisService
                .findStatsByEntityIdAndBaseDate<BaseStatsDocument>(
                    statsType = statsType,
                    baseDate = baseDate,
                    entityId = entityId,
                ).let(com.wildrew.jobstat.statistics_read.stats.usecase.GetStatsByEntityIdAndBaseDate::Response)
        }

    data class Request(
        @field:NotNull val statsType: StatsType,
        @field:NotNull val baseDate: BaseDate,
        @field:Min(1) val entityId: Long,
    )

    data class Response<T : BaseStatsDocument>(
        val stats: T?,
    )
}
