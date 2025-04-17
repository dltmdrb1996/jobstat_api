package com.example.jobstat.statistics_read.rankings.document

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_retention_rate_rankings")
class CompanyRetentionRateRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanyRetentionMetrics,
    @Field("rankings")
    override val rankings: List<CompanyRetentionRankingEntry>,
) : SimpleRankingDocument<CompanyRetentionRateRankingsDocument.CompanyRetentionRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class CompanyRetentionMetrics(
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
        @Field("retention_metrics")
        val retentionMetrics: RetentionMetrics,
    ) : RankingMetrics {
        data class RetentionMetrics(
            @Field("industry_comparison")
            val industryComparison: IndustryComparison,
            @Field("tenure_analysis")
            val tenureAnalysis: TenureAnalysis,
            @Field("satisfaction_metrics")
            val satisfactionMetrics: SatisfactionMetrics,
        ) {
            data class IndustryComparison(
                val industryAvg: Double,
                val percentileRank: Double,
                val benchmarkData: Map<Long, Double>,
            )

            data class TenureAnalysis(
                val avgTenure: Double,
                val tenureDistribution: Map<String, Double>,
                val criticalPeriods: List<String>,
            )

            data class SatisfactionMetrics(
                val employeeSatisfaction: Double,
                val workLifeBalance: Double,
                val careerGrowth: Double,
            )
        }
    }

    data class CompanyRetentionRankingEntry(
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
        @Field("retention_details")
        val retentionDetails: RetentionDetails,
        @Field("employee_insights")
        val employeeInsights: EmployeeInsights,
    ) : SimpleRankingEntry {
        data class RetentionDetails(
            @Field("overall_retention")
            val overallRetention: Double,
            @Field("department_retention")
            val departmentRetention: Map<String, Double>,
            @Field("tenure_metrics")
            val tenureMetrics: TenureMetrics,
            @Field("turnover_analysis")
            val turnoverAnalysis: TurnoverAnalysis,
        ) {
            data class TenureMetrics(
                val averageTenure: Double,
                val medianTenure: Double,
                val tenureDistribution: Map<String, Int>,
            )

            data class TurnoverAnalysis(
                val voluntaryTurnover: Double,
                val involuntaryTurnover: Double,
                val earlyTurnover: Double,
            )
        }

        data class EmployeeInsights(
            @Field("engagement_metrics")
            val engagementMetrics: EngagementMetrics,
            @Field("career_development")
            val careerDevelopment: CareerDevelopment,
            @Field("workplace_factors")
            val workplaceFactors: WorkplaceFactors,
        ) {
            data class EngagementMetrics(
                val engagementScore: Double,
                val participationRate: Double,
                val satisfactionTrend: String,
            )

            data class CareerDevelopment(
                val promotionRate: Double,
                val skillDevelopment: Double,
                val mentorshipQuality: Double,
            )

            data class WorkplaceFactors(
                val cultureFit: Double,
                val managementQuality: Double,
                val compensationSatisfaction: Double,
            )
        }
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(
            rankings.all {
                it.retentionDetails.overallRetention in 0.0..100.0
            },
        ) { "유지율은 0에서 100 퍼센트 사이여야 합니다" }
    }
}
