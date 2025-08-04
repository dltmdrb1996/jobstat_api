package com.wildrew.jobstat.statistics_read.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.jobstat.statistics_read.stats.document.SkillStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

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
@StatsRepositoryType(StatsType.SKILL)
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
        TODO()
    }

    override fun calculatePercentile(
        type: RankingType,
        value: Double,
    ): Double {
        TODO()
    }

    override fun calculateMedianSalary(): Long {
        TODO()
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
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }
}
