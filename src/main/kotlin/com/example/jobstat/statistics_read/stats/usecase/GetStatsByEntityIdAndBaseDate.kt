package com.example.jobstat.statistics_read.stats.usecase

import com.example.jobstat.core.core_model.BaseDate
import com.example.jobstat.core.core_mongo_base.model.stats.BaseStatsDocument
import com.example.jobstat.core.core_usecase.base.UseCase
import com.example.jobstat.core.core_usecase.base.ValidUseCase
import com.example.jobstat.statistics_read.rankings.model.RankingWithStats
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.model.rankingtype.toStatsType
import com.example.jobstat.statistics_read.rankings.service.RankingAnalysisService
import com.example.jobstat.statistics_read.stats.document.*
import com.example.jobstat.statistics_read.stats.registry.StatsType
import com.example.jobstat.statistics_read.stats.service.StatsAnalysisService
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
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
                ).let(::Response)
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
