package com.example.jobstat.statistics.stats.service

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.statistics.stats.registry.StatsRepositoryRegistry
import com.example.jobstat.statistics.stats.registry.StatsType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

interface StatsAnalysisService {
    fun <T : BaseStatsDocument> findStatsByEntityIdAndBaseDate(
        statsType: StatsType,
        baseDate: BaseDate,
        entityId: Long,
    ): T?

    fun <T : BaseStatsDocument> findLatestStats(
        statsType: StatsType,
        entityId: Long,
        baseDate: BaseDate = BaseDate.now(),
    ): T?

    fun <T : BaseStatsDocument> findStatsByEntityIds(
        statsType: StatsType,
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): List<T>

    fun <T : BaseStatsDocument> findStatsByEntityId(
        statsType: StatsType,
        entityId: Long,
    ): List<T>?
}

@Service
class StatsAnalysisServiceImpl(
    private val statsRepositoryRegistry: StatsRepositoryRegistry,
) : StatsAnalysisService {

    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun <T : BaseStatsDocument> findStatsByEntityId(
        statsType: StatsType,
        entityId: Long,
    ): List<T>? = statsRepositoryRegistry.getRepository<T>(statsType).findByEntityId(entityId)

    override fun <T : BaseStatsDocument> findStatsByEntityIds(
        statsType: StatsType,
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): List<T> {
        if (entityIds.isEmpty()) return emptyList()
        return statsRepositoryRegistry.getRepository<T>(statsType).findByBaseDateAndEntityIds(baseDate, entityIds)
    }

    @Cacheable(
        cacheNames = ["StatsByEntityIdAndBaseDate"],
        key = "#baseDate + ':' + #statsType + ':' + #entityId",
        unless = "#result == null",
    )
    override fun <T : BaseStatsDocument> findLatestStats(
        statsType: StatsType,
        entityId: Long,
        baseDate: BaseDate,
    ): T? = statsRepositoryRegistry.getRepository<T>(statsType).findLatestStatsByEntityId(entityId)

    @Cacheable(
        cacheNames = ["StatsByEntityIdAndBaseDate"],
        key = "#baseDate + ':' + #statsType + ':' + #entityId",
        unless = "#result == null",
    )
    override fun <T : BaseStatsDocument> findStatsByEntityIdAndBaseDate(
        statsType: StatsType,
        baseDate: BaseDate,
        entityId: Long,
    ): T? {
        log.info("findStatsByEntityIdAndBaseDate: $statsType, $baseDate, $entityId")
        val result = statsRepositoryRegistry.getRepository<T>(statsType).findByEntityIdAndBaseDate(entityId, baseDate)
        log.info("findStatsByEntityIdAndBaseDate isNull? = ${result == null} result: ${result?.entityId}")
        return result
    }
}
