package com.example.jobstat.utils.base

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.repository.StatsMongoRepository
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.statistics.fake.AbstractFakeTimeSeriesRepository

abstract class AbstractFakeStatsRepository<T : BaseStatsDocument> :
    AbstractFakeTimeSeriesRepository<T>(),
    StatsMongoRepository<T, String> {
    override fun getCollectionName(): String = "fake_stats"

    override fun findByEntityId(entityId: Long): List<T> =
        documents.values
            .filter { it.entityId == entityId }
            .sortedByDescending { it.baseDate }

    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T? =
        documents.values
            .find { it.entityId == entityId && it.baseDate == baseDate.toString() }

    override fun findByBaseDateAndEntityIds(
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): List<T> =
        documents.values
            .filter { doc ->
                doc.baseDate == baseDate.toString() && doc.entityId in entityIds
            }

    override fun findStatsByEntityIdsBatch(
        baseDate: BaseDate,
        entityIds: List<Long>,
        batchSize: Int,
    ): List<T> = findByBaseDateAndEntityIds(baseDate, entityIds)

    override fun findByBaseDateBetweenAndEntityId(
        startDate: BaseDate,
        endDate: BaseDate,
        entityId: Long,
    ): List<T> =
        documents.values
            .filter { doc ->
                doc.entityId == entityId &&
                    doc.baseDate >= startDate.toString() &&
                    doc.baseDate <= endDate.toString()
            }.sortedBy { it.baseDate }

    override fun findLatestStatsByEntityId(entityId: Long): T? =
        documents.values
            .filter { it.entityId == entityId }
            .maxByOrNull { it.baseDate }

    override fun findTopGrowthSkills(
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<T> =
        documents.values
            .filter { doc ->
                doc.baseDate >= startDate.toString() &&
                    doc.baseDate <= endDate.toString()
            }.sortedByDescending { it.stats.growthRate }
            .take(limit)

    override fun findTopSkillsByIndustry(
        industryId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> = emptyList()

    override fun findTopSkillsByCompanySize(
        companySize: String,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> = emptyList()

    override fun findTopSkillsByJobCategory(
        jobCategoryId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> = emptyList()

    override fun findSkillsWithMultiIndustryGrowth(
        baseDate: BaseDate,
        minIndustryCount: Int,
        minGrowthRate: Double,
    ): List<T> = emptyList()

    override fun findEmergingSkillsByIndustry(
        industryId: Long,
        baseDate: BaseDate,
        minGrowthRate: Double,
    ): List<T> = emptyList()
}
