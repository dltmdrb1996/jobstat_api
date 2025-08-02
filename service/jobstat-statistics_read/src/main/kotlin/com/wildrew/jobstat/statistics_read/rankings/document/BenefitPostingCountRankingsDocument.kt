package com.wildrew.jobstat.statistics_read.rankings.document

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "benefit_posting_count_rankings")
class BenefitPostingCountRankingsDocument(
    id: String? = null,
    page: Int = 1,
    @Field("base_date")
    override val baseDate: String,
    @Field("period")
    override val period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: BenefitPostingMetrics,
    @Field("rankings")
    override val rankings: List<BenefitPostingRankingEntry>,
) : SimpleRankingDocument<BenefitPostingCountRankingsDocument.BenefitPostingRankingEntry>(
    id,
    baseDate,
    period,
    metrics,
    rankings,
    page,
) {
    data class BenefitPostingMetrics(
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
        @Field("benefit_metrics")
        val benefitMetrics: BenefitMetrics,
    ) : RankingMetrics {
        data class BenefitMetrics(
            @Field("popularity_metrics")
            val popularityMetrics: PopularityMetrics,
            @Field("industry_analysis")
            val industryAnalysis: Map<Long, IndustryBenefits>,
            @Field("company_size_trends")
            val companySizeTrends: Map<String, BenefitTrend>,
        ) {
            data class PopularityMetrics(
                val adoptionRate: Double,
                val valuePerception: Double,
                val costEffectiveness: Double,
            )

            data class IndustryBenefits(
                val offeringRate: Double,
                val uniqueBenefits: List<String>,
                val marketPosition: String,
            )

            data class BenefitTrend(
                val provisionRate: Double,
                val growthRate: Double,
                val employeePreference: Double,
            )
        }
    }

    data class BenefitPostingRankingEntry(
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
        @Field("benefit_details")
        val benefitDetails: BenefitDetails,
        @Field("offering_metrics")
        val offeringMetrics: OfferingMetrics,
    ) : SimpleRankingEntry {
        data class BenefitDetails(
            @Field("posting_count")
            val postingCount: Int,
            @Field("active_count")
            val activeCount: Int,
            @Field("offering_companies")
            val offeringCompanies: Int,
            @Field("benefit_value")
            val benefitValue: BenefitValue,
        ) {
            data class BenefitValue(
                val monetaryValue: Long?,
                val perceptionScore: Double,
                val satisfactionRate: Double,
            )
        }

        data class OfferingMetrics(
            @Field("company_distribution")
            val companyDistribution: CompanyDistribution,
            @Field("industry_presence")
            val industryPresence: IndustryPresence,
            @Field("employee_impact")
            val employeeImpact: EmployeeImpact,
        ) {
            data class CompanyDistribution(
                val bySize: Map<String, Double>,
                val byType: Map<String, Double>,
                val byLocation: Map<Long, Double>,
            )

            data class IndustryPresence(
                val topIndustries: List<IndustryOffering>,
                val growthTrends: Map<Long, Double>,
            ) {
                data class IndustryOffering(
                    val industryId: Long,
                    val offeringRate: Double,
                    val growthRate: Double,
                )
            }

            data class EmployeeImpact(
                val retentionImpact: Double,
                val satisfactionImpact: Double,
                val recruitmentImpact: Double,
            )
        }
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "Rankings must not be empty" }
        require(
            rankings.all {
                it.benefitDetails.activeCount <= it.benefitDetails.postingCount
            },
        ) { "Active count cannot exceed posting count" }
    }
}
