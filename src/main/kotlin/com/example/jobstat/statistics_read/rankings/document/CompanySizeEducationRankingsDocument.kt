package com.example.jobstat.statistics_read.rankings.document

import com.example.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.DistributionRankingDocument
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.example.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import com.example.jobstat.statistics_read.core.core_model.CompanySize
import com.example.jobstat.statistics_read.core.core_model.EntityType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_size_education_rankings")
class CompanySizeEducationRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanySizeEducationMetrics,
    @Field("group_entity_type")
    override val groupEntityType: EntityType = EntityType.COMPANY_SIZE,
    @Field("target_entity_type")
    override val targetEntityType: EntityType = EntityType.EDUCATION,
    @Field("rankings")
    override val rankings: List<CompanySizeEducationRankingEntry>,
) : DistributionRankingDocument<CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        groupEntityType,
        targetEntityType,
        rankings,
        page,
    ) {
    data class CompanySizeEducationMetrics(
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
        @Field("education_trends")
        val educationTrends: EducationTrends,
    ) : RankingMetrics {
        data class EducationTrends(
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

    data class CompanySizeEducationRankingEntry(
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
        @Field("education_requirements")
        val educationRequirements: EducationRequirements,
        @Field("salary_distribution")
        val salaryDistribution: Map<String, SalaryMetrics>,
        @Field("trend_indicators")
        val trendIndicators: TrendIndicators,
    ) : DistributionRankingEntry {
        data class EducationRequirements(
            @Field("mandatory_ratio")
            val mandatoryRatio: Double,
            @Field("preferred_ratio")
            val preferredRatio: Double,
            @Field("flexible_ratio")
            val flexibleRatio: Double,
            @Field("requirements_by_job_level")
            val requirementsByJobLevel: Map<String, Map<String, Double>>,
        )

        data class SalaryMetrics(
            @Field("avg_salary")
            val avgSalary: Long,
            @Field("median_salary")
            val medianSalary: Long,
            @Field("salary_range")
            val salaryRange: SalaryRange,
        ) {
            data class SalaryRange(
                val min: Long,
                val max: Long,
                val p25: Long,
                val p75: Long,
            )
        }

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
                it.distribution.values.sum() in 0.99..1.01 // 허용 오차 범위 내의 100%
            },
        ) { "Distribution percentages must sum to approximately 100%" }
        require(
            rankings.all {
                it.distribution.keys.containsAll(listOf("HIGH_SCHOOL", "ASSOCIATE", "BACHELOR", "MASTER", "DOCTORATE"))
            },
        ) { "All education levels must be represented in distribution" }
    }

    data class CompanySizeEducationRankingAggregation(
        @Field("company_size")
        val companySize: CompanySize,
        @Field("education_distribution")
        val educationDistribution: Map<String, Double>,
        @Field("total_companies")
        val totalCompanies: Int,
        @Field("avg_requirements")
        val avgRequirements: Map<String, Double>,
    )
}
