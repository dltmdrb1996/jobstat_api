package com.wildrew.jobstat.statistics_read.stats.usecase

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
class GetStatsByEntityId(
    private val statsAnalysisService: StatsAnalysisService,
    validator: Validator,
) : ValidUseCase<GetStatsByEntityId.Request, GetStatsByEntityId.Response<*>>(validator) {
    @Transactional
    override fun execute(request: Request): Response<*> =
        with(request) {
            // 엔티티 ID에 해당하는 모든 통계 조회
            statsAnalysisService
                .findStatsByEntityId<BaseStatsDocument>(
                    statsType = statsType,
                    entityId = entityId,
                ).let(com.wildrew.jobstat.statistics_read.stats.usecase.GetStatsByEntityId::Response)
        }

    data class Request(
        @field:NotNull val statsType: StatsType,
        @field:Min(1) val entityId: Long,
    )

    data class Response<T : BaseStatsDocument>(
        val stats: List<T>?,
    )
}
