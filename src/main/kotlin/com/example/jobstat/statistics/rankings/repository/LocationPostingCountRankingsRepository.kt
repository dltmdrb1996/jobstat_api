package com.example.jobstat.statistics.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.statistics.rankings.document.LocationPostingCountRankingsDocument
import com.example.jobstat.statistics.rankings.model.RankingType
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.LOCATION_POSTING_COUNT)
@NoRepositoryBean
interface LocationPostingCountRankingsRepository : SimpleRankingRepository<LocationPostingCountRankingsDocument, LocationPostingCountRankingsDocument.LocationPostingRankingEntry, String> {
    // 지역 고용 시장 분석
    fun findByEmploymentStats(
        baseDate: String,
        minEmploymentRate: Double,
    ): List<LocationPostingCountRankingsDocument>

    // 시장 잠재력 분석
    fun findByGrowthPotential(
        baseDate: String,
        minGrowthPotential: Double,
    ): List<LocationPostingCountRankingsDocument>

    // 원격 근무 기회 분석
    fun findByRemoteWorkOpportunities(
        baseDate: String,
        minRemoteRatio: Double,
    ): List<LocationPostingCountRankingsDocument>
}

@Repository
class LocationPostingCountRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<LocationPostingCountRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<LocationPostingCountRankingsDocument, LocationPostingCountRankingsDocument.LocationPostingRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    LocationPostingCountRankingsRepository {
    override fun findByEmploymentStats(
        baseDate: String,
        minEmploymentRate: Double,
    ): List<LocationPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte(
                        "metrics.market_metrics.employment_stats.employment_rate",
                        minEmploymentRate,
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("metrics.market_metrics.employment_stats.employment_rate"),
                        Sorts.descending("metrics.market_metrics.employment_stats.labor_force_participation"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByGrowthPotential(
        baseDate: String,
        minGrowthPotential: Double,
    ): List<LocationPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("market_indicators.growth_potential", minGrowthPotential),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("market_indicators.growth_potential"),
                        Sorts.descending("market_indicators.opportunity_index"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByRemoteWorkOpportunities(
        baseDate: String,
        minRemoteRatio: Double,
    ): List<LocationPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.addFields(
                    Field(
                        "remote_ratio",
                        Document(
                            "\$divide",
                            listOf(
                                "\$posting_stats.remote_postings",
                                "\$posting_stats.total_postings",
                            ),
                        ),
                    ),
                ),
                Aggregates.match(Filters.gte("remote_ratio", minRemoteRatio)),
                Aggregates.sort(Sorts.descending("remote_ratio")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
