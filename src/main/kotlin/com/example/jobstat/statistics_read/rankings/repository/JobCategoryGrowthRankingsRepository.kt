package com.example.jobstat.statistics_read.rankings.repository

import com.example.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepository
import com.example.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.statistics_read.rankings.document.JobCategoryGrowthRankingsDocument
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.JOB_CATEGORY_GROWTH)
@NoRepositoryBean
interface JobCategoryGrowthRankingsRepository :
    SimpleRankingRepository<JobCategoryGrowthRankingsDocument, JobCategoryGrowthRankingsDocument.JobCategoryGrowthRankingEntry, String> {
    // 산업 연관성 분석
    fun findByIndustryCorrelation(
        baseDate: String,
        industryId: Long,
        minCorrelation: Double,
    ): List<JobCategoryGrowthRankingsDocument>

    // 신규 트렌드 영향도 분석
    fun findByEmergingTrendImpact(
        baseDate: String,
        minImpactScore: Double,
    ): List<JobCategoryGrowthRankingsDocument>

    // 성장 지속가능성 분석
    fun findSustainableGrowthCategories(
        baseDate: String,
        minSustainability: Double,
    ): List<JobCategoryGrowthRankingsDocument>
}

@Repository
class JobCategoryGrowthRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<JobCategoryGrowthRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<JobCategoryGrowthRankingsDocument, JobCategoryGrowthRankingsDocument.JobCategoryGrowthRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    JobCategoryGrowthRankingsRepository {
    override fun findByIndustryCorrelation(
        baseDate: String,
        industryId: Long,
        minCorrelation: Double,
    ): List<JobCategoryGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$gte",
                            listOf(
                                Document(
                                    "\$getField",
                                    listOf(
                                        industryId.toString(),
                                        "\$metrics.market_dynamics.industry_growth_correlation",
                                    ),
                                ),
                                minCorrelation,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.market_dynamics.industry_growth_correlation.$industryId",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByEmergingTrendImpact(
        baseDate: String,
        minImpactScore: Double,
    ): List<JobCategoryGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.unwind("\$metrics.market_dynamics.emerging_trends"),
                Aggregates.match(
                    Filters.gte(
                        "metrics.market_dynamics.emerging_trends.impact_score",
                        minImpactScore,
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending("metrics.market_dynamics.emerging_trends.impact_score"),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSustainableGrowthCategories(
        baseDate: String,
        minSustainability: Double,
    ): List<JobCategoryGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("future_outlook.growth_sustainability", minSustainability),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("future_outlook.growth_sustainability"),
                        Sorts.descending("future_outlook.opportunity_score"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
