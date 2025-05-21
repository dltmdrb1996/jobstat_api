package com.example.jobstat.statistics_read.rankings.document

import com.example.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "industry_salary_rankings")
class IndustrySalaryRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: IndustrySalaryMetrics,
    @Field("rankings")
    override val rankings: List<IndustrySalaryRankingEntry>,
) : SimpleRankingDocument<IndustrySalaryRankingsDocument.IndustrySalaryRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class IndustrySalaryMetrics(
        @Field("total_count")
        override val totalCount: Int,
        @Field("ranked_count")
        override val rankedCount: Int,
        @Field("new_entries")
        override val newEntries: Int,
        @Field("dropped_entries")
        override val droppedEntries: Int,
        @Field("volatility_metrics")
        override val volatilityMetrics: com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics,
        @Field("compensation_metrics")
        val compensationMetrics: CompensationMetrics,
    ) : RankingMetrics {
        data class CompensationMetrics(
            @Field("market_analysis")
            val marketAnalysis: MarketAnalysis,
            @Field("regional_variance")
            val regionalVariance: Map<Long, RegionalStats>,
            @Field("experience_premium")
            val experiencePremium: Map<String, Double>,
        ) {
            data class MarketAnalysis(
                val marketMedian: Long,
                val salarySpread: Double,
                val compensationTrend: String,
            )

            data class RegionalStats(
                val avgSalary: Long,
                val costOfLivingIndex: Double,
                val adjustedSalary: Long,
            )
        }
    }

    data class IndustrySalaryRankingEntry(
        @Field("entity_id")
        override val entityId: Long,
        @Field("name")
        override val name: String,
        @Field("rank")
        override val rank: Int,
        @Field("previous_rank")
        override val previousRank: Int?,
        @Field("rank_change")
        override val rankChange: Int?,
        @Field("value")
        override val score: Double,
        @Field("salary_details")
        val salaryDetails: SalaryDetails,
        @Field("compensation_structure")
        val compensationStructure: CompensationStructure,
    ) : SimpleRankingEntry {
        data class SalaryDetails(
            @Field("avg_salary")
            val avgSalary: Long,
            @Field("median_salary")
            val medianSalary: Long,
            @Field("salary_range")
            val salaryRange: SalaryRange,
            @Field("growth_rate")
            val growthRate: Double,
        ) {
            data class SalaryRange(
                val min: Long,
                val max: Long,
                val p25: Long,
                val p75: Long,
            )
        }

        data class CompensationStructure(
            @Field("base_ratio")
            val baseRatio: Double,
            @Field("bonus_ratio")
            val bonusRatio: Double,
            @Field("benefits_value")
            val benefitsValue: Long,
            @Field("equity_ratio")
            val equityRatio: Double?,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(rankings.all { it.salaryDetails.avgSalary > 0 }) { "급여는 양수여야 합니다" }
        require(
            rankings.all {
                it.compensationStructure.baseRatio +
                    it.compensationStructure.bonusRatio +
                    (it.compensationStructure.equityRatio ?: 0.0) <= 1.0
            },
        ) { "보상 비율의 합은 1 이하여야 합니다" }
    }
}
