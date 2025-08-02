package com.wildrew.jobstat.statistics_read.rankings.document

import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.DistributionRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_size_salary_rankings")
class CompanySizeSalaryRankingsDocument(
    id: String? = null,
    page: Int = 1,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanySizeSalaryMetrics,
    @Field("group_entity_type")
    override val groupEntityType: EntityType = EntityType.COMPANY_SIZE,
    @Field("target_entity_type")
    override val targetEntityType: EntityType = EntityType.SALARY,
    @Field("rankings")
    override val rankings: List<CompanySizeSalaryRankingEntry>,
) : DistributionRankingDocument<CompanySizeSalaryRankingsDocument.CompanySizeSalaryRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        groupEntityType,
        targetEntityType,
        rankings,
        page,
    ) {
    data class CompanySizeSalaryMetrics(
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
        @Field("salary_trends")
        val salaryTrends: SalaryTrends,
    ) : RankingMetrics {
        data class SalaryTrends(
            @Field("overall_distribution")
            val overallDistribution: Map<String, Double>,
            @Field("year_over_year_changes")
            val yearOverYearChanges: Map<String, Double>,
            @Field("market_comparison")
            val marketComparison: Map<String, Double>,
            @Field("industry_patterns")
            val industryPatterns: List<IndustryPattern>,
        ) {
            data class IndustryPattern(
                @Field("industry_id")
                val industryId: Long,
                @Field("industry_name")
                val industryName: String,
                @Field("distribution")
                val distribution: Map<String, Double>,
            )
        }
    }

    data class CompanySizeSalaryRankingEntry(
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
        @Field("distribution")
        override val distribution: Map<String, Double>,
        @Field("dominant_category")
        override val dominantCategory: String,
        @Field("distribution_metrics")
        override val distributionMetrics: DistributionMetrics,
        @Field("total_postings")
        val totalPostings: Int,
        @Field("value_change")
        override val valueChange: Double = 0.0,
        @Field("salary_metrics")
        val salaryMetrics: SalaryMetrics,
        @Field("experience_distribution")
        val experienceDistribution: Map<String, SalaryRange>,
        @Field("trend_indicators")
        val trendIndicators: TrendIndicators,
    ) : DistributionRankingEntry {
        data class SalaryMetrics(
            @Field("avg_salary")
            val avgSalary: Long,
            @Field("median_salary")
            val medianSalary: Long,
            @Field("percentile_range")
            val percentileRange: SalaryRange,
            @Field("compensation_ratio")
            val compensationRatio: CompensationRatio,
        ) {
            data class CompensationRatio(
                val baseSalary: Double,
                val bonus: Double,
                val benefits: Double,
            )
        }

        data class SalaryRange(
            val min: Long,
            val max: Long,
            val p25: Long,
            val p75: Long,
        )

        data class TrendIndicators(
            @Field("growth_rate")
            val growthRate: Double,
            @Field("change_velocity")
            val changeVelocity: Double,
            @Field("stability_score")
            val stabilityScore: Double,
            @Field("future_projection")
            val futureProjection: String,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(
            rankings.all {
                it.distribution.values.sum() in 0.99..1.01
            },
        ) { "Distribution percentages must sum to approximately 100%" }
        require(
            rankings.all {
                it.salaryMetrics.percentileRange.max > it.salaryMetrics.percentileRange.min
            },
        ) { "Maximum salary must be greater than minimum salary" }
    }
}
