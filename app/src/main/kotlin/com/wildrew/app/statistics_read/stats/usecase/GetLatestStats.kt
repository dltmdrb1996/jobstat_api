package com.wildrew.app.statistics_read.stats.usecase

import com.wildrew.app.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.app.statistics_read.stats.document.*
import com.wildrew.app.statistics_read.stats.registry.StatsType
import com.wildrew.app.statistics_read.stats.service.StatsAnalysisService
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class GetLatestStats(
    private val statsAnalysisService: StatsAnalysisService,
    validator: Validator,
) : ValidUseCase<GetLatestStats.Request, GetLatestStats.Response<*>>(validator) {
    @Transactional
    override fun execute(request: Request): Response<*> =
        with(request) {
            // 엔티티 ID에 해당하는 최신 통계 조회
            statsAnalysisService
                .findLatestStats<BaseStatsDocument>(
                    statsType = statsType,
                    entityId = entityId,
                ).let(::Response)
        }

    data class Request(
        @field:NotNull val statsType: StatsType,
        @field:Min(1) val entityId: Long,
    )

    data class Response<T : BaseStatsDocument>(
        val stats: T?,
    )
}
