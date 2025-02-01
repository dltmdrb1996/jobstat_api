package com.example.jobstat.rankings.document

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "job_category_salary_rankings")
class JobCategorySalaryRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics") override val metrics: JobCategorySalaryMetrics,
    @Field("rankings") override val rankings: List<JobCategorySalaryRankingEntry>,
) : SimpleRankingDocument<JobCategorySalaryRankingsDocument.JobCategorySalaryRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class JobCategorySalaryMetrics(
        @Field("total_count") override val totalCount: Int,
        @Field("ranked_count") override val rankedCount: Int,
        @Field("new_entries") override val newEntries: Int,
        @Field("dropped_entries") override val droppedEntries: Int,
        @Field("volatility_metrics") override val volatilityMetrics: VolatilityMetrics,
        @Field("salary_analysis") val salaryAnalysis: SalaryAnalysis,
    ) : RankingMetrics {
        data class SalaryAnalysis(
            @Field("industry_comparison") val industryComparison: Map<Long, SalaryComparison>,
            @Field("experience_impact") val experienceImpact: Map<String, Double>,
            @Field("company_size_variance") val companySizeVariance: Map<String, Double>,
        ) {
            data class SalaryComparison(
                val avgSalary: Long,
                val medianSalary: Long,
                val marketPosition: Double, // 시장 평균 대비 위치
            )
        }
    }

    data class JobCategorySalaryRankingEntry(
        @Field("document_id") override val documentId: String,
        @Field("entity_id") override val entityId: Long,
        @Field("name") override val name: String,
        @Field("rank") override val rank: Int,
        @Field("previous_rank") override val previousRank: Int?,
        @Field("rank_change") override val rankChange: Int?,
        @Field("value") override val score: Double,
        @Field("avg_salary") val avgSalary: Long,
        @Field("median_salary") val medianSalary: Long,
        @Field("salary_details") val salaryDetails: SalaryDetails,
    ) : SimpleRankingEntry {
        data class SalaryDetails(
            @Field("salary_range") val salaryRange: SalaryRange,
            @Field("experience_based_salary") val experienceBasedSalary: Map<String, Long>,
            @Field("salary_growth_rate") val salaryGrowthRate: Double,
            @Field("benefits_value") val benefitsValue: Long?,
        ) {
            data class SalaryRange(
                val min: Long,
                val max: Long,
                val p25: Long,
                val p75: Long,
            )
        }
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.avgSalary > 0 }) { "Average salary must be positive" }
        require(rankings.all { it.medianSalary > 0 }) { "Median salary must be positive" }
    }
}
