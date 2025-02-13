package com.example.jobstat.statistics.rankings.repository

import com.example.jobstat.core.base.repository.DistributionRankingRepository
import com.example.jobstat.core.base.repository.DistributionRankingRepositoryImpl
import com.example.jobstat.statistics.rankings.document.CompanySizeEducationRankingsDocument
import com.example.jobstat.statistics.rankings.model.rankingtype.RankingType
import com.mongodb.client.model.*
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.aggregation.Aggregation.ROOT
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.COMPANY_SIZE_EDUCATION)
@NoRepositoryBean
interface CompanySizeEducationRankingsRepository : DistributionRankingRepository<CompanySizeEducationRankingsDocument, CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry, String> {
    // 교육 수준별 급여 분포 분석
    fun findSalaryDistributionByEducation(
        baseDate: String,
        educationLevel: String,
    ): List<CompanySizeEducationRankingsDocument>

    // 교육 요구사항 유연성 분석
    fun findFlexibleEducationRequirements(
        baseDate: String,
        minFlexibleRatio: Double,
    ): List<CompanySizeEducationRankingsDocument>

    // 교육 수준별 성장률 분석
    fun findEducationLevelGrowthTrends(
        startDate: String,
        endDate: String,
        educationLevel: String,
    ): List<CompanySizeEducationRankingsDocument>
}

@Repository
class CompanySizeEducationRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanySizeEducationRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : DistributionRankingRepositoryImpl<CompanySizeEducationRankingsDocument, CompanySizeEducationRankingsDocument.CompanySizeEducationRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanySizeEducationRankingsRepository {
    override fun findSalaryDistributionByEducation(
        baseDate: String,
        educationLevel: String,
    ): List<CompanySizeEducationRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$gt",
                            listOf(
                                Document(
                                    "\$getField",
                                    listOf(
                                        educationLevel,
                                        "\$distribution",
                                    ),
                                ),
                                0.0,
                            ),
                        ),
                    ),
                ),
                Aggregates.addFields(
                    Field(
                        "salary_stats",
                        Document(
                            "\$getField",
                            listOf(
                                educationLevel,
                                "\$salary_distribution",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("salary_stats.avg_salary")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findFlexibleEducationRequirements(
        baseDate: String,
        minFlexibleRatio: Double,
    ): List<CompanySizeEducationRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("education_requirements.flexible_ratio", minFlexibleRatio),
                ),
                Aggregates.sort(Sorts.descending("education_requirements.flexible_ratio")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findEducationLevelGrowthTrends(
        startDate: String,
        endDate: String,
        educationLevel: String,
    ): List<CompanySizeEducationRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.gte("base_date", startDate),
                        Filters.lte("base_date", endDate),
                    ),
                ),
                Aggregates.group(
                    "\$entity_id",
                    Accumulators.first("latest_doc", "$$ROOT"),
                    Accumulators.avg(
                        "avg_distribution",
                        Document(
                            "\$getField",
                            listOf(
                                educationLevel,
                                "\$distribution",
                            ),
                        ),
                    ),
                    Accumulators.stdDevPop(
                        "distribution_stability",
                        Document(
                            "\$getField",
                            listOf(
                                educationLevel,
                                "\$distribution",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("avg_distribution"),
                        Sorts.ascending("distribution_stability"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc ->
                doc.get("latest_doc", Document::class.java)?.let {
                    mongoOperations.converter.read(entityInformation.javaType, it)
                } ?: throw IllegalStateException("latest_doc is null")
            }.filterNotNull()
            .toList()
    }
}
