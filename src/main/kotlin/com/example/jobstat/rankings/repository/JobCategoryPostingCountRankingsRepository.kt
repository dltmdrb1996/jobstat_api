package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.rankings.model.JobCategoryPostingCountRankingsDocument
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface JobCategoryPostingCountRankingsRepository
    :
    SimpleRankingRepository<JobCategoryPostingCountRankingsDocument, JobCategoryPostingCountRankingsDocument.JobCategoryPostingRankingEntry, String> {
    // 지역별 수요 분석
    fun findByLocationDemand(
        baseDate: String,
        locationId: Long,
        minDemand: Int,
    ): List<JobCategoryPostingCountRankingsDocument>

    // 원격 근무 트렌드 분석
    fun findHighRemoteWorkCategories(
        baseDate: String,
        minRemoteRatio: Double,
    ): List<JobCategoryPostingCountRankingsDocument>

    // 채용 경쟁률 분석
    fun findByCompetitionRate(
        baseDate: String,
        minCompetitionRate: Double,
    ): List<JobCategoryPostingCountRankingsDocument>
}

@Repository
class JobCategoryPostingCountRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<JobCategoryPostingCountRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<JobCategoryPostingCountRankingsDocument, JobCategoryPostingCountRankingsDocument.JobCategoryPostingRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    JobCategoryPostingCountRankingsRepository {
    override fun findByLocationDemand(
        baseDate: String,
        locationId: Long,
        minDemand: Int,
    ): List<JobCategoryPostingCountRankingsDocument> {
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
                                        locationId.toString(),
                                        "\$metrics.market_analysis.demand_by_location",
                                    ),
                                ),
                                minDemand,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending("metrics.market_analysis.demand_by_location.$locationId"),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findHighRemoteWorkCategories(
        baseDate: String,
        minRemoteRatio: Double,
    ): List<JobCategoryPostingCountRankingsDocument> {
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
                                "\$posting_details.remote_work_postings",
                                "\$posting_details.total_postings",
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

    override fun findByCompetitionRate(
        baseDate: String,
        minCompetitionRate: Double,
    ): List<JobCategoryPostingCountRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("demand_indicators.competition_rate", minCompetitionRate),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("demand_indicators.competition_rate"),
                        Sorts.descending("demand_indicators.application_rate"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
