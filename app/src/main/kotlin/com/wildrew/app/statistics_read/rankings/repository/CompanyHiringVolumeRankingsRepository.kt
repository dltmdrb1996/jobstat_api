package com.wildrew.app.statistics_read.rankings.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.SimpleRankingRepository
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.wildrew.app.statistics_read.rankings.document.CompanyHiringVolumeRankingsDocument
import com.wildrew.app.statistics_read.rankings.model.rankingtype.RankingType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.COMPANY_HIRING_VOLUME)
@NoRepositoryBean
interface CompanyHiringVolumeRankingsRepository : SimpleRankingRepository<CompanyHiringVolumeRankingsDocument, CompanyHiringVolumeRankingsDocument.CompanyHiringRankingEntry, String> {
    // 부서별 채용 트렌드 분석
    fun findTopHiringDepartments(
        baseDate: String,
        companyId: Long,
    ): Map<String, Int>

    // 채용 효율성 분석
    fun findEfficientHiringCompanies(
        baseDate: String,
        maxTimeToHire: Double,
        minFillRate: Double,
    ): List<CompanyHiringVolumeRankingsDocument>

    // 성장 단계별 채용 패턴 분석
    fun findHiringPatternsByGrowthStage(
        baseDate: String,
        growthStage: String,
    ): List<CompanyHiringVolumeRankingsDocument>
}

@Repository
class CompanyHiringVolumeRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanyHiringVolumeRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<CompanyHiringVolumeRankingsDocument, CompanyHiringVolumeRankingsDocument.CompanyHiringRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanyHiringVolumeRankingsRepository {
    override fun findTopHiringDepartments(
        baseDate: String,
        companyId: Long,
    ): Map<String, Int> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val document =
            collection
                .find(
                    Filters.and(
                        Filters.eq("base_date", baseDate),
                        Filters.eq("rankings.entity_id", companyId),
                    ),
                ).first()

        return document?.let {
            mongoOperations.converter
                .read(entityInformation.javaType, it)
                .rankings
                .first { entry -> entry.entityId == companyId }
                .hiringDetails.hiringByDepartment
        } ?: emptyMap()
    }

    override fun findEfficientHiringCompanies(
        baseDate: String,
        maxTimeToHire: Double,
        minFillRate: Double,
    ): List<CompanyHiringVolumeRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.and(
                        Filters.lte("rankings.hiring_details.hiring_timeline.avg_time_to_hire", maxTimeToHire),
                        Filters.gte(
                            Document(
                                "\$divide",
                                listOf(
                                    "\$rankings.hiring_details.filled_positions",
                                    "\$rankings.hiring_details.total_positions",
                                ),
                            ).toString(),
                            minFillRate,
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.ascending("rankings.hiring_details.hiring_timeline.avg_time_to_hire")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findHiringPatternsByGrowthStage(
        baseDate: String,
        growthStage: String,
    ): List<CompanyHiringVolumeRankingsDocument> {
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
                                "\$rankings.growth_indicators.hiring_growth_rate",
                                Document(
                                    "\$cond",
                                    listOf(
                                        Document("\$eq", listOf(growthStage, "startup")),
                                        50,
                                        Document(
                                            "\$cond",
                                            listOf(
                                                Document("\$eq", listOf(growthStage, "scaleup")),
                                                30,
                                                15,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("rankings.growth_indicators.hiring_growth_rate")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
