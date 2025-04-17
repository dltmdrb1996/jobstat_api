package com.example.jobstat.statistics_read.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.statistics_read.rankings.document.IndustrySalaryRankingsDocument
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

@RankingRepositoryType(RankingType.INDUSTRY_SALARY)
@NoRepositoryBean
interface IndustrySalaryRankingsRepository : SimpleRankingRepository<IndustrySalaryRankingsDocument, IndustrySalaryRankingsDocument.IndustrySalaryRankingEntry, String> {
    // 지역별 조정 급여 분석
    fun findRegionalAdjustedSalaries(
        baseDate: String,
        locationId: Long,
    ): List<IndustrySalaryRankingsDocument>

    // 경력별 프리미엄 분석
    fun findHighestExperiencePremium(
        baseDate: String,
        experienceLevel: String,
    ): List<IndustrySalaryRankingsDocument>

    // 총 보상 패키지 분석
    fun findComprehensiveCompensationAnalysis(baseDate: String): List<IndustrySalaryRankingsDocument>
}

@Repository
class IndustrySalaryRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<IndustrySalaryRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<IndustrySalaryRankingsDocument, IndustrySalaryRankingsDocument.IndustrySalaryRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    IndustrySalaryRankingsRepository {
    override fun findRegionalAdjustedSalaries(
        baseDate: String,
        locationId: Long,
    ): List<IndustrySalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.addFields(
                    Field(
                        "regional_metrics",
                        Document(
                            "\$getField",
                            listOf(
                                locationId.toString(),
                                "\$metrics.compensation_metrics.regional_variance",
                            ),
                        ),
                    ),
                ),
                Aggregates.match(Filters.exists("regional_metrics", true)),
                Aggregates.sort(Sorts.descending("regional_metrics.adjusted_salary")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findHighestExperiencePremium(
        baseDate: String,
        experienceLevel: String,
    ): List<IndustrySalaryRankingsDocument> {
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
                                        experienceLevel,
                                        "\$metrics.compensation_metrics.experience_premium",
                                    ),
                                ),
                                0.0,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.compensation_metrics.experience_premium.$experienceLevel",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findComprehensiveCompensationAnalysis(baseDate: String): List<IndustrySalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.addFields(
                    Field(
                        "total_compensation_score",
                        Document(
                            "\$add",
                            listOf(
                                Document(
                                    "\$multiply",
                                    listOf(
                                        "\$compensation_structure.base_ratio",
                                        "\$salary_details.avg_salary",
                                    ),
                                ),
                                Document(
                                    "\$multiply",
                                    listOf(
                                        "\$compensation_structure.bonus_ratio",
                                        "\$salary_details.avg_salary",
                                    ),
                                ),
                                "\$compensation_structure.benefits_value",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("total_compensation_score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
