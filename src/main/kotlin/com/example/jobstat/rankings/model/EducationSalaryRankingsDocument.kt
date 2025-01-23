package com.example.jobstat.rankings.model

import com.example.jobstat.core.base.mongo.SnapshotPeriod
import com.example.jobstat.core.base.mongo.ranking.RankingMetrics
import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.base.mongo.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "education_salary_rankings")
class EducationSalaryRankingsDocument(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: EducationSalaryMetrics,
    @Field("rankings")
    override val rankings: List<EducationSalaryRankingEntry>,
) : SimpleRankingDocument<EducationSalaryRankingsDocument.EducationSalaryRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
    ) {
    data class EducationSalaryMetrics(
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
        @Field("education_metrics")
        val educationMetrics: EducationMetrics,
    ) : RankingMetrics {
        data class EducationMetrics(
            @Field("roi_analysis")
            val roiAnalysis: RoiAnalysis,
            @Field("industry_impact")
            val industryImpact: Map<Long, IndustryEducationMetrics>,
            @Field("career_progression")
            val careerProgression: CareerProgressionMetrics,
        ) {
            data class RoiAnalysis(
                val educationCost: Map<String, Long>,
                val paybackPeriod: Map<String, Double>,
                val lifetimeValue: Map<String, Long>,
            )

            data class IndustryEducationMetrics(
                val requiredRate: Double,
                val preferredRate: Double,
                val salaryPremium: Double,
            )

            data class CareerProgressionMetrics(
                val promotionRate: Double,
                val careerMobility: Double,
                val skillDevelopment: Double,
            )
        }
    }

    data class EducationSalaryRankingEntry(
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
        @Field("salary_metrics")
        val salaryMetrics: SalaryMetrics,
        @Field("education_impact")
        val educationImpact: EducationImpact,
    ) : SimpleRankingEntry {
        data class SalaryMetrics(
            @Field("avg_salary")
            val avgSalary: Long,
            @Field("median_salary")
            val medianSalary: Long,
            @Field("salary_range")
            val salaryRange: SalaryRange,
            @Field("experience_based_salary")
            val experienceBasedSalary: Map<String, Long>,
        ) {
            data class SalaryRange(
                val min: Long,
                val max: Long,
                val p25: Long,
                val p75: Long,
            )
        }

        data class EducationImpact(
            @Field("employment_metrics")
            val employmentMetrics: EmploymentMetrics,
            @Field("skill_requirements")
            val skillRequirements: SkillRequirements,
            @Field("career_prospects")
            val careerProspects: CareerProspects,
        ) {
            data class EmploymentMetrics(
                val employmentRate: Double,
                val jobSecurityIndex: Double,
                val careerSwitchability: Double,
            )

            data class SkillRequirements(
                val requiredSkills: List<String>,
                val skillGrowthRate: Double,
                val skillMarketValue: Double,
            )

            data class CareerProspects(
                val promotionPotential: Double,
                val industryMobility: Double,
                val careerGrowthIndex: Double,
                @Field("opportunity_fields")
                val opportunityFields: List<OpportunityField>,
            ) {
                data class OpportunityField(
                    val fieldName: String,
                    val growthPotential: Double,
                    val salaryPotential: Long,
                )
            }
        }
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(rankings.all { it.salaryMetrics.avgSalary > 0 }) { "Average salary must be positive" }
        require(
            rankings.all {
                it.educationImpact.employmentMetrics.employmentRate in 0.0..100.0
            },
        ) { "Employment rate must be between 0 and 100 percent" }
    }
}
