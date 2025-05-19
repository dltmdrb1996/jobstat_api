package com.example.jobstat.statistics_read.stats.repository

import com.example.jobstat.core.core_mongo_base.repository.StatsMongoRepository
import com.example.jobstat.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.example.jobstat.core.core_util.calculation.StatisticsCalculationUtil
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.stats.document.SkillStatsDocument
import com.example.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.example.jobstat.statistics_read.stats.registry.StatsType
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.SKILL)
@NoRepositoryBean
interface SkillStatsRepository : StatsMongoRepository<SkillStatsDocument, String> {
    fun findByGrowthRateGreaterThan(growthRate: Double): List<SkillStatsDocument>

    fun findEmergingSkills(limit: Int): List<SkillStatsDocument>

    fun findByDemandScoreGreaterThan(demandScore: Double): List<SkillStatsDocument>

    fun findByJobCategoryIdAndBaseDate(
        jobCategoryId: Long,
        baseDate: String,
    ): List<SkillStatsDocument>

    fun calculateRank(
        type: RankingType,
        value: Double,
    ): Int

    fun calculatePercentile(
        type: RankingType,
        value: Double,
    ): Double

    fun calculateMedianSalary(): Long

    fun findByEntityIdAndBaseDateBetween(
        entityId: Long,
        startDate: String,
        endDate: String,
    ): List<SkillStatsDocument>
}

@Repository
class SkillStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<SkillStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<SkillStatsDocument, String>(entityInformation, mongoOperations),
    SkillStatsRepository {
    override fun findByGrowthRateGreaterThan(growthRate: Double): List<SkillStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.growth_rate", growthRate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findEmergingSkills(limit: Int): List<SkillStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("emerging_skill", true))
            .sort(Sorts.descending("stats.growth_rate"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByDemandScoreGreaterThan(demandScore: Double): List<SkillStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.demand_score", demandScore),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByJobCategoryIdAndBaseDate(
        jobCategoryId: Long,
        baseDate: String,
    ): List<SkillStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("related_job_categories.job_category_id", jobCategoryId),
                    Filters.eq("base_date", baseDate),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun calculateRank(
        type: RankingType,
        value: Double,
    ): Int {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        // 현재 value보다 큰 값을 가진 문서의 수를 계산
        return collection
            .countDocuments(
                Filters.and(
                    Filters.gt("rankings.${type.name}.value", value),
                    Filters.eq("base_date", StatisticsCalculationUtil.calculateLastMonthDate()),
                ),
            ).toInt() + 1 // 1을 더해서 1-based rank 반환
    }

    override fun calculatePercentile(
        type: RankingType,
        value: Double,
    ): Double {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        // 전체 문서 수
        val totalCount =
            collection
                .countDocuments(
                    Filters.eq("base_date", StatisticsCalculationUtil.calculateLastMonthDate()),
                ).toDouble()

        // value보다 작거나 같은 값을 가진 문서의 수
        val belowCount =
            collection
                .countDocuments(
                    Filters.and(
                        Filters.lte("rankings.${type.name}.value", value),
                        Filters.eq("base_date", StatisticsCalculationUtil.calculateLastMonthDate()),
                    ),
                ).toDouble()

        return (belowCount / totalCount) * 100
    }

    override fun calculateMedianSalary(): Long {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        // 현재 기준 월의 모든 salary를 가져와서 중간값 계산
        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.eq("base_date", StatisticsCalculationUtil.calculateLastMonthDate()),
                ),
                Aggregates.group(null, Accumulators.push("salaries", "\$stats.avg_salary")),
                Aggregates.project(
                    Document(
                        "\$arrayElemAt",
                        listOf(
                            "\$salaries",
                            Document(
                                "\$floor",
                                Document(
                                    "\$divide",
                                    listOf(Document("\$size", "\$salaries"), 2),
                                ),
                            ),
                        ),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .firstOrNull()
            ?.getDouble("salary")
            ?.toLong()
            ?: 0L
    }

    override fun findByEntityIdAndBaseDateBetween(
        entityId: Long,
        startDate: String,
        endDate: String,
    ): List<SkillStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("entity_id", entityId),
                    Filters.gte("base_date", startDate),
                    Filters.lte("base_date", endDate),
                ),
            ).sort(Sorts.descending("base_date"))
            .hintString("snapshot_lookup_idx")
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findLatestStatsByEntityId(entityId: Long): SkillStatsDocument? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("entity_id", entityId))
            .sort(Sorts.descending("base_date"))
            .limit(1)
            .hintString("snapshot_lookup_idx")
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }
}
