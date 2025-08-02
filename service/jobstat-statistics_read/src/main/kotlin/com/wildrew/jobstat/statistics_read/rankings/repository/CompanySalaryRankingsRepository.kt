package com.wildrew.jobstat.statistics_read.rankings.repository

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.SimpleRankingRepositoryImpl
import com.wildrew.jobstat.statistics_read.rankings.document.CompanySalaryRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface CompanySalaryRankingsRepository : SimpleRankingRepository<CompanySalaryRankingsDocument, CompanySalaryRankingsDocument.CompanySalaryRankingEntry, String> {
    // 산업별 급여 분포 분석
    fun findCompaniesAboveIndustryMedian(
        baseDate: String,
        industryId: Long,
    ): List<CompanySalaryRankingsDocument>

    // 직급별 급여 범위 분석
    fun findSalaryRangesByLevel(
        baseDate: String,
        companyId: Long,
    ): Map<String, CompanySalaryRankingsDocument.CompanySalaryRankingEntry.SalaryDetails.SalaryRange>

    // 총 보상 패키지 기반 분석
    fun findTopTotalCompensationCompanies(
        baseDate: String,
        limit: Int,
    ): List<CompanySalaryRankingsDocument>

    // 급여 성장률 기반 분석
    fun findHighestSalaryGrowthCompanies(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<CompanySalaryRankingsDocument>
}

@Repository
@RankingRepositoryType(RankingType.COMPANY_SALARY)
class CompanySalaryRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanySalaryRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<CompanySalaryRankingsDocument, CompanySalaryRankingsDocument.CompanySalaryRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanySalaryRankingsRepository {
    override fun findCompaniesAboveIndustryMedian(
        baseDate: String,
        industryId: Long,
    ): List<CompanySalaryRankingsDocument> {
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
                                "\$salary_details.median_salary",
                                Document(
                                    "\$arrayElemAt",
                                    listOf(
                                        "\$metrics.compensation_metrics.market_position.industry_comparison.$industryId",
                                        0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("salary_details.median_salary")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSalaryRangesByLevel(
        baseDate: String,
        companyId: Long,
    ): Map<String, CompanySalaryRankingsDocument.CompanySalaryRankingEntry.SalaryDetails.SalaryRange> {
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
                .salaryDetails.salaryRanges
        } ?: emptyMap()
    }

    override fun findTopTotalCompensationCompanies(
        baseDate: String,
        limit: Int,
    ): List<CompanySalaryRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.sort(Sorts.descending("rankings.compensation_package.total_rewards.total_value")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findHighestSalaryGrowthCompanies(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<CompanySalaryRankingsDocument> {
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
                    "\$rankings.entity_id",
                    Accumulators.avg("avg_growth", "\$rankings.salary_details.salary_growth.annual_increase"),
                ),
                Aggregates.sort(Sorts.descending("avg_growth")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
