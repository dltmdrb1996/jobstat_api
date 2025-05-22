package com.wildrew.app.statistics_read.rankings.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.SimpleRankingRepository
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.wildrew.app.statistics_read.rankings.document.EducationSalaryRankingsDocument
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.EDUCATION_SALARY)
@NoRepositoryBean
interface EducationSalaryRankingsRepository :
    SimpleRankingRepository<EducationSalaryRankingsDocument, EducationSalaryRankingsDocument.EducationSalaryRankingEntry, String> {
    // ROI 기반 교육 분석
    fun findHighestRoiEducations(
        baseDate: String,
        maxPaybackPeriod: Double,
    ): List<EducationSalaryRankingsDocument>

    // 산업별 교육 영향도 분석
    fun findEducationImpactByIndustry(
        baseDate: String,
        industryId: Long,
    ): List<EducationSalaryRankingsDocument>

    // 커리어 성과 기반 분석
    fun findTopCareerProspectEducations(baseDate: String): List<EducationSalaryRankingsDocument>
}

@Repository
class EducationSalaryRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<EducationSalaryRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<EducationSalaryRankingsDocument, EducationSalaryRankingsDocument.EducationSalaryRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    EducationSalaryRankingsRepository {
    override fun findHighestRoiEducations(
        baseDate: String,
        maxPaybackPeriod: Double,
    ): List<EducationSalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$lt",
                            listOf(
                                Document("\$max", "\$metrics.education_metrics.roi_analysis.payback_period"),
                                maxPaybackPeriod,
                            ),
                        ),
                    ),
                ),
                Aggregates.addFields(
                    Field(
                        "roi_score",
                        Document(
                            "\$divide",
                            listOf(
                                Document("\$avg", "\$metrics.education_metrics.roi_analysis.lifetime_value"),
                                Document("\$avg", "\$metrics.education_metrics.roi_analysis.education_cost"),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("roi_score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findEducationImpactByIndustry(
        baseDate: String,
        industryId: Long,
    ): List<EducationSalaryRankingsDocument> {
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
                                        industryId.toString(),
                                        "\$metrics.education_metrics.industry_impact.salary_premium",
                                    ),
                                ),
                                0.0,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.education_metrics.industry_impact.$industryId.salary_premium",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopCareerProspectEducations(baseDate: String): List<EducationSalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.addFields(
                    Field(
                        "career_score",
                        Document(
                            "\$add",
                            listOf(
                                "\$education_impact.career_prospects.promotion_potential",
                                "\$education_impact.career_prospects.industry_mobility",
                                "\$education_impact.career_prospects.career_growth_index",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("career_score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
