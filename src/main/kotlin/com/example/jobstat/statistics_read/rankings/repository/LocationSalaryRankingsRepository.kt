package com.example.jobstat.statistics_read.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.statistics_read.rankings.document.LocationSalaryRankingsDocument
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.LOCATION_SALARY)
@NoRepositoryBean
interface LocationSalaryRankingsRepository : SimpleRankingRepository<LocationSalaryRankingsDocument, LocationSalaryRankingsDocument.LocationSalaryRankingEntry, String> {
    // 생활비 조정 급여 분석
    fun findByCostAdjustedSalary(
        baseDate: String,
        minAdjustedSalary: Long,
    ): List<LocationSalaryRankingsDocument>

    // 원격 근무 영향도 분석
    fun findByRemoteWorkImpact(
        baseDate: String,
        minRemoteRatio: Double,
    ): List<LocationSalaryRankingsDocument>

    // 지역별 산업 집중도 분석
    fun findByIndustryConcentration(
        baseDate: String,
        industryId: Long,
        minConcentration: Double,
    ): List<LocationSalaryRankingsDocument>
}

@Repository
class LocationSalaryRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<LocationSalaryRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<LocationSalaryRankingsDocument, LocationSalaryRankingsDocument.LocationSalaryRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    LocationSalaryRankingsRepository {
    override fun findByCostAdjustedSalary(
        baseDate: String,
        minAdjustedSalary: Long,
    ): List<LocationSalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("salary_metrics.adjusted_avg_salary", minAdjustedSalary),
                ),
                Aggregates.addFields(
                    Field(
                        "purchasing_power",
                        Document(
                            "\$divide",
                            listOf(
                                "\$salary_metrics.adjusted_avg_salary",
                                Document(
                                    "\$multiply",
                                    listOf(
                                        "\$location_factors.cost_of_living_index",
                                        1.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("purchasing_power")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByRemoteWorkImpact(
        baseDate: String,
        minRemoteRatio: Double,
    ): List<LocationSalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte(
                        "metrics.location_metrics.remote_work_impact.remote_job_ratio",
                        minRemoteRatio,
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("metrics.location_metrics.remote_work_impact.remote_job_ratio"),
                        Sorts.descending("metrics.location_metrics.remote_work_impact.salary_differential"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByIndustryConcentration(
        baseDate: String,
        industryId: Long,
        minConcentration: Double,
    ): List<LocationSalaryRankingsDocument> {
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
                                        "\$metrics.location_metrics.industry_distribution",
                                    ),
                                ),
                                minConcentration,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.location_metrics.industry_distribution.$industryId",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
