package com.wildrew.jobstat.statistics_read.rankings.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.wildrew.jobstat.statistics_read.rankings.document.CompanyRetentionRateRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface CompanyRetentionRateRankingsRepository : SimpleRankingRepository<CompanyRetentionRateRankingsDocument, CompanyRetentionRateRankingsDocument.CompanyRetentionRankingEntry, String> {
    // 이직률 패턴 분석
    fun findLowTurnoverCompanies(
        baseDate: String,
        maxTurnoverRate: Double,
    ): List<CompanyRetentionRateRankingsDocument>

    // 경력 단계별 유지율 분석
    fun findRetentionByTenure(
        baseDate: String,
        companyId: Long,
    ): Map<String, Double>

    // 부서별 유지율 비교 분석
    fun findDepartmentRetentionComparison(
        baseDate: String,
        companyId: Long,
    ): Map<String, Double>
}

@Repository
@RankingRepositoryType(RankingType.COMPANY_RETENTION_RATE)
class CompanyRetentionRateRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanyRetentionRateRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<CompanyRetentionRateRankingsDocument, CompanyRetentionRateRankingsDocument.CompanyRetentionRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanyRetentionRateRankingsRepository {
    override fun findLowTurnoverCompanies(
        baseDate: String,
        maxTurnoverRate: Double,
    ): List<CompanyRetentionRateRankingsDocument> {
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
                                Document(
                                    "\$sum",
                                    listOf(
                                        "\$rankings.retention_details.turnover_analysis.voluntary_turnover",
                                        "\$rankings.retention_details.turnover_analysis.involuntary_turnover",
                                    ),
                                ),
                                maxTurnoverRate,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.ascending("rankings.retention_details.turnover_analysis.voluntary_turnover")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findRetentionByTenure(
        baseDate: String,
        companyId: Long,
    ): Map<String, Double> {
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
                .retentionDetails.tenureMetrics.tenureDistribution
                .mapValues { entry -> entry.value.toDouble() }
        } ?: emptyMap()
    }

    override fun findDepartmentRetentionComparison(
        baseDate: String,
        companyId: Long,
    ): Map<String, Double> {
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
                .retentionDetails.departmentRetention
        } ?: emptyMap()
    }
}
