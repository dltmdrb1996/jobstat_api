package com.wildrew.jobstat.statistics_read.rankings.document

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_salary_rankings")
class CompanySalaryRankingsDocument(
    id: String? = null,
    page: Int = 1,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanySalaryMetrics,
    @Field("rankings")
    override val rankings: List<CompanySalaryRankingEntry>,
) : SimpleRankingDocument<CompanySalaryRankingsDocument.CompanySalaryRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class CompanySalaryMetrics(
        @Field("total_count")
        override val totalCount: Int,
        @Field("ranked_count")
        override val rankedCount: Int,
        @Field("new_entries")
        override val newEntries: Int,
        @Field("dropped_entries")
        override val droppedEntries: Int,
        @Field("volatility_metrics")
        override val volatilityMetrics: VolatilityMetrics,
        @Field("compensation_metrics")
        val compensationMetrics: CompensationMetrics,
    ) : RankingMetrics {
        data class CompensationMetrics(
            @Field("market_position")
            val marketPosition: MarketPosition,
            @Field("benefits_analysis")
            val benefitsAnalysis: BenefitsAnalysis,
            @Field("equity_distribution")
            val equityDistribution: EquityDistribution,
        ) {
            data class MarketPosition(
                val marketPercentile: Double,
                val competitiveIndex: Double,
                val industryComparison: Map<Long, Double>,
            )

            data class BenefitsAnalysis(
                val benefitsValue: Long,
                val coverageRatio: Double,
                val satisfactionScore: Double,
            )

            data class EquityDistribution(
                val equityOffering: Boolean,
                val avgEquityValue: Long?,
                val vestingStructure: String?,
            )
        }
    }

    data class CompanySalaryRankingEntry(
        @Field("document_id")
        override val documentId: String,
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
        @Field("value_change")
        override val valueChange: Double = 0.0,
        @Field("salary_details")
        val salaryDetails: SalaryDetails,
        @Field("compensation_package")
        val compensationPackage: CompensationPackage,
    ) : SimpleRankingEntry {
        data class SalaryDetails(
            @Field("avg_salary")
            val avgSalary: Long,
            @Field("median_salary")
            val medianSalary: Long,
            @Field("salary_ranges")
            val salaryRanges: Map<String, SalaryRange>,
            @Field("salary_growth")
            val salaryGrowth: SalaryGrowth,
        ) {
            data class SalaryRange(
                val min: Long,
                val max: Long,
                val p25: Long,
                val p75: Long,
            )

            data class SalaryGrowth(
                val annualIncrease: Double,
                val promotionIncrease: Double,
                val performanceBonus: Double,
            )
        }

        data class CompensationPackage(
            @Field("base_salary_ratio")
            val baseSalaryRatio: Double,
            @Field("bonus_structure")
            val bonusStructure: BonusStructure,
            @Field("benefits_value")
            val benefitsValue: Long,
            @Field("total_rewards")
            val totalRewards: TotalRewards,
        ) {
            data class BonusStructure(
                val performanceBonus: Double,
                val signingBonus: Double?,
                val stockOptions: Double?,
            )

            data class TotalRewards(
                val cashCompensation: Long,
                val equityValue: Long?,
                val benefitsValue: Long,
                val totalValue: Long,
            )
        }
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.salaryDetails.avgSalary > 0 }) { "Average salary must be positive" }
        require(
            rankings.all {
                it.compensationPackage.baseSalaryRatio in 0.0..1.0
            },
        ) { "Base salary ratio must be between 0 and 1" }
    }
}
