package com.wildrew.jobstat.statistics_read.rankings.document

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingMetrics
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.SimpleRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.VolatilityMetrics
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "company_growth_rankings")
class CompanyGrowthRankingsDocument(
    id: String? = null,
    page: Int = 1,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("metrics")
    override val metrics: CompanyGrowthMetrics,
    @Field("rankings")
    override val rankings: List<CompanyGrowthRankingEntry>,
) : SimpleRankingDocument<CompanyGrowthRankingsDocument.CompanyGrowthRankingEntry>(
        id,
        baseDate,
        period,
        metrics,
        rankings,
        page,
    ) {
    data class CompanyGrowthMetrics(
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
        @Field("market_metrics")
        val marketMetrics: MarketMetrics,
    ) : RankingMetrics {
        data class MarketMetrics(
            @Field("market_dynamics")
            val marketDynamics: MarketDynamics,
            @Field("growth_patterns")
            val growthPatterns: GrowthPatterns,
            @Field("industry_context")
            val industryContext: Map<Long, IndustryContext>,
        ) {
            data class MarketDynamics(
                val marketShareDistribution: Map<String, Double>,
                val competitiveLandscape: String,
                val marketMaturity: String,
            )

            data class GrowthPatterns(
                val organicGrowth: Double,
                val acquisitionGrowth: Double,
                val marketExpansion: Double,
            )

            data class IndustryContext(
                val industryGrowth: Double,
                val marketPosition: String,
                val competitivePosition: String,
            )
        }
    }

    data class CompanyGrowthRankingEntry(
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
        @Field("growth_metrics")
        val growthMetrics: GrowthMetrics,
        @Field("business_metrics")
        val businessMetrics: BusinessMetrics,
    ) : SimpleRankingEntry {
        data class GrowthMetrics(
            @Field("revenue_growth")
            val revenueGrowth: Double,
            @Field("employee_growth")
            val employeeGrowth: Double,
            @Field("market_share_growth")
            val marketShareGrowth: Double,
            @Field("customer_growth")
            val customerGrowth: Double,
        )

        data class BusinessMetrics(
            @Field("financial_health")
            val financialHealth: FinancialHealth,
            @Field("operational_metrics")
            val operationalMetrics: OperationalMetrics,
            @Field("market_presence")
            val marketPresence: MarketPresence,
        ) {
            data class FinancialHealth(
                val profitability: Double,
                val cashFlow: Double,
                val debtRatio: Double,
            )

            data class OperationalMetrics(
                val employeeProductivity: Double,
                val operationalEfficiency: Double,
                val innovationIndex: Double,
            )

            data class MarketPresence(
                val marketShare: Double,
                val brandStrength: Double,
                val customerSatisfaction: Double,
            )
        }
    }

    override fun validate() {
        require(rankings.isNotEmpty()) { "순위 목록이 비어있으면 안됩니다" }
        require(
            rankings.all {
                it.growthMetrics.revenueGrowth >= -100.0
            },
        ) { "매출 성장률은 -100% 미만이 될 수 없습니다" }
    }
}
