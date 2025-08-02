package com.wildrew.jobstat.statistics_read.rankings.document

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "skill_salary_rankings")
class SkillSalaryRankingsDocument(
    id: String? = null,
    page: Int = 1,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: SkillSalaryMetrics,
    @Field("rankings")
    override var rankings: List<SkillSalaryRankingEntry>,
) : SimpleRankingDocument<SkillSalaryRankingsDocument.SkillSalaryRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class SkillSalaryMetrics(
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
        @Field("salary_distribution")
        val salaryDistribution: SalaryDistribution,
    ) : RankingMetrics {
        data class SalaryDistribution(
            @Field("median_salary")
            val medianSalary: Long,
            @Field("percentiles")
            val percentiles: Map<Int, Long>,
            @Field("industry_comparison")
            val industryComparison: Map<String, Long>,
        )
    }

    data class SkillSalaryRankingEntry(
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
        @Field("avg_salary")
        val avgSalary: Long,
        @Field("median_salary")
        val medianSalary: Long,
        @Field("salary_range")
        val salaryRange: SalaryRange,
        @Field("experience_premium")
        val experiencePremium: Map<String, Double>,
    ) : SimpleRankingEntry {
        data class SalaryRange(
            val min: Long,
            val max: Long,
            val p25: Long,
            val p75: Long,
        )
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.avgSalary > 0 }) { "All salaries must be positive" }
    }
}
