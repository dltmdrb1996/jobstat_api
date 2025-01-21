package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.rankings.model.SkillPostingCountRankingsDocument
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface SkillPostingCountRankingsRepository : SimpleRankingRepository<SkillPostingCountRankingsDocument, SkillPostingCountRankingsDocument.SkillPostingRankingEntry, String> {
    // 산업별 수요 분석
    fun findByIndustryDemand(
        baseDate: String,
        industryId: Long,
        minPostings: Int,
    ): List<SkillPostingCountRankingsDocument>

    // 기업 규모별 수요 분석
    fun findByCompanySizeDemand(
        baseDate: String,
        companySize: String,
        minPostings: Int,
    ): List<SkillPostingCountRankingsDocument>

    // 계절성 분석
    fun findBySeasonality(
        baseDate: String,
        minSeasonalityIndex: Double,
    ): List<SkillPostingCountRankingsDocument>
}

@Repository
class SkillPostingCountRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<SkillPostingCountRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<SkillPostingCountRankingsDocument, SkillPostingCountRankingsDocument.SkillPostingRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    SkillPostingCountRankingsRepository {
    override fun findByIndustryDemand(
        baseDate: String,
        industryId: Long,
        minPostings: Int,
    ): List<SkillPostingCountRankingsDocument> {
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
                                        "\$metrics.posting_distribution.by_industry",
                                    ),
                                ),
                                minPostings,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending("metrics.posting_distribution.by_industry.$industryId"),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByCompanySizeDemand(
        baseDate: String,
        companySize: String,
        minPostings: Int,
    ): List<SkillPostingCountRankingsDocument> {
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
                                        companySize,
                                        "\$metrics.posting_distribution.by_company_size",
                                    ),
                                ),
                                minPostings,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.posting_distribution.by_company_size.$companySize",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findBySeasonality(
        baseDate: String,
        minSeasonalityIndex: Double,
    ): List<SkillPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("posting_trend.seasonality_index", minSeasonalityIndex),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("posting_trend.seasonality_index"),
                        Sorts.descending("posting_trend.year_over_year_change"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
