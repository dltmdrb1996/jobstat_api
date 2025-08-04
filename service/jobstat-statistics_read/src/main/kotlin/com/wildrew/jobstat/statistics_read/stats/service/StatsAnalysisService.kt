package com.wildrew.jobstat.statistics_read.stats.service

import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.statistics_read.core.StatsBulkCacheManager
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryRegistry
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface StatsAnalysisService {
    /**
     * 특정 엔티티의 특정 기준일자 통계 데이터를 조회합니다.
     */
    fun <T : BaseStatsDocument> findStatsByEntityIdAndBaseDate(
        statsType: StatsType,
        baseDate: BaseDate,
        entityId: Long,
    ): T?

    /**
     * 특정 엔티티의 가장 최근 통계 데이터를 조회합니다.
     */
    fun <T : BaseStatsDocument> findLatestStats(
        statsType: StatsType,
        entityId: Long,
        baseDate: BaseDate = BaseDate.now(),
    ): T?

    /**
     * 특정 엔티티의 모든 통계 데이터를 조회합니다.
     */
    fun <T : BaseStatsDocument> findStatsByEntityId(
        statsType: StatsType,
        entityId: Long,
    ): List<T>?

    /**
     * 특정 기준일자의 여러 엔티티 통계 데이터를 조회합니다.
     */
    fun <T : BaseStatsDocument> findStatsByEntityIds(
        statsType: StatsType,
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): List<T>

    /**
     * 여러 엔티티의 특정 기준일자 통계 데이터를 엔티티 ID를 키로 한 맵으로 조회합니다.
     */
    fun <T : BaseStatsDocument> findStatsByEntityIdsAndBaseDate(
        statsType: StatsType,
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): Map<Long, T?>

    /**
     * 여러 엔티티의 최신 통계 데이터를 엔티티 ID를 키로 한 맵으로 조회합니다.
     */
    fun <T : BaseStatsDocument> findLatestStatsByEntityIds(
        statsType: StatsType,
        entityIds: List<Long>,
        baseDate: BaseDate = BaseDate.now(),
    ): Map<Long, T?>
}

@Service
class StatsAnalysisServiceImpl(
    private val statsRepositoryRegistry: StatsRepositoryRegistry,
    private val statsBulkCacheManager: StatsBulkCacheManager,
) : StatsAnalysisService {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 특정 엔티티의 모든 통계 데이터를 조회합니다.
     *
     * 참고: 이 메소드는 여러 문서를 리턴하므로 캐싱하지 않습니다.
     * 각 문서는 개별적으로 findStatsByEntityIdAndBaseDate나 findLatestStats에서 캐싱됩니다.
     */
    override fun <T : BaseStatsDocument> findStatsByEntityId(
        statsType: StatsType,
        entityId: Long,
    ): List<T>? = statsRepositoryRegistry.getRepository<T>(statsType).findByEntityId(entityId)

    /**
     * 특정 기준일자의 여러 엔티티 통계 데이터를 조회합니다.
     *
     * 참고: 이 메소드는 bulk 캐싱 연산을 사용하여 문서를 캐싱합니다.
     * 리턴 타입은 기존 API와의 호환성을 위해 List<T>입니다.
     */
    override fun <T : BaseStatsDocument> findStatsByEntityIds(
        statsType: StatsType,
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): List<T> {
        if (entityIds.isEmpty()) return emptyList()

        val resultMap = findStatsByEntityIdsAndBaseDate<T>(statsType, baseDate, entityIds)

        return resultMap.values.filterNotNull()
    }

    /**
     * 특정 엔티티의 가장 최근 통계 데이터를 조회합니다.
     */
    override fun <T : BaseStatsDocument> findLatestStats(
        statsType: StatsType,
        entityId: Long,
        baseDate: BaseDate,
    ): T? {
        val cacheKey = statsBulkCacheManager.createCacheKey(baseDate, statsType, entityId)

        val cached = statsBulkCacheManager.get<T>(cacheKey)
        if (cached != null) {
            log.trace("Cache hit for latest stats: {}", cacheKey)
            return cached
        }

        val result = statsRepositoryRegistry.getRepository<T>(statsType).findLatestStatsByEntityId(entityId)
        if (result != null) {
            log.trace("Caching latest stats: {}", cacheKey)
            statsBulkCacheManager.put(cacheKey, result)
        }
        return result
    }

    /**
     * 특정 엔티티의 특정 기준일자 통계 데이터를 조회합니다.
     */
    override fun <T : BaseStatsDocument> findStatsByEntityIdAndBaseDate(
        statsType: StatsType,
        baseDate: BaseDate,
        entityId: Long,
    ): T? {
        val cacheKey = statsBulkCacheManager.createCacheKey(baseDate, statsType, entityId)

        val cached = statsBulkCacheManager.get<T>(cacheKey)
        if (cached != null) {
            log.trace("Cache hit for stats by date: {}", cacheKey)
            return cached
        }

        val result = statsRepositoryRegistry.getRepository<T>(statsType).findByEntityIdAndBaseDate(entityId, baseDate)
        if (result != null) {
            log.trace("Caching stats by date: {}", cacheKey)
            statsBulkCacheManager.put(cacheKey, result)
        }
        return result
    }

    /**
     * 여러 엔티티의 특정 기준일자 통계 데이터를 엔티티 ID를 키로 한 맵으로 조회합니다.
     */
    override fun <T : BaseStatsDocument> findStatsByEntityIdsAndBaseDate(
        statsType: StatsType,
        baseDate: BaseDate,
        entityIds: List<Long>,
    ): Map<Long, T?> {
        if (entityIds.isEmpty()) {
            return emptyMap()
        }

        val cacheKeys =
            entityIds
                .map { entityId ->
                    entityId to statsBulkCacheManager.createCacheKey(baseDate, statsType, entityId)
                }.toMap()

        val cachedResults = statsBulkCacheManager.getAll<T>(cacheKeys.values)

        val missingEntityIds =
            entityIds.filter { entityId ->
                val key = cacheKeys[entityId]
                key !in cachedResults
            }

        if (missingEntityIds.isEmpty()) {
            log.debug("Bulk cache hit for all {} entities", entityIds.size)

            return entityIds.associateWith { entityId ->
                cachedResults[cacheKeys[entityId]]
            }
        }

        log.debug("Bulk cache miss for {} entities", missingEntityIds.size)
        val dbResults =
            statsRepositoryRegistry
                .getRepository<T>(statsType)
                .findByBaseDateAndEntityIds(baseDate, missingEntityIds)

        val dbResultsMap = dbResults.associateBy { it.entityId }

        val toCache =
            dbResults
                .mapNotNull { document ->
                    val entityId = document.entityId
                    val key = cacheKeys[entityId]
                    if (key != null) {
                        key to document
                    } else {
                        null
                    }
                }.toMap()

        statsBulkCacheManager.putAll(toCache)

        return entityIds.associateWith { entityId ->
            cachedResults[cacheKeys[entityId]] ?: dbResultsMap[entityId]
        }
    }

    /**
     * 여러 엔티티의 최신 통계 데이터를 엔티티 ID를 키로 한 맵으로 조회합니다.
     */
    override fun <T : BaseStatsDocument> findLatestStatsByEntityIds(
        statsType: StatsType,
        entityIds: List<Long>,
        baseDate: BaseDate,
    ): Map<Long, T?> {
        if (entityIds.isEmpty()) {
            return emptyMap()
        }

        val cacheKeys =
            entityIds
                .map { entityId ->
                    entityId to statsBulkCacheManager.createCacheKey(baseDate, statsType, entityId)
                }.toMap()

        val cachedResults = statsBulkCacheManager.getAll<T>(cacheKeys.values)

        val missingEntityIds =
            entityIds.filter { entityId ->
                val key = cacheKeys[entityId]
                key !in cachedResults
            }

        if (missingEntityIds.isEmpty()) {
            log.debug("Bulk cache hit for all {} entities", entityIds.size)

            return entityIds.associateWith { entityId ->
                cachedResults[cacheKeys[entityId]]
            }
        }

        log.debug("Bulk cache miss for {} entities", missingEntityIds.size)

        val dbResults =
            missingEntityIds.mapNotNull { entityId ->
                val result = statsRepositoryRegistry.getRepository<T>(statsType).findLatestStatsByEntityId(entityId)
                if (result != null) {
                    result
                } else {
                    null
                }
            }

        val dbResultsMap = dbResults.associateBy { it.entityId }

        val toCache =
            dbResults
                .mapNotNull { document ->
                    val entityId = document.entityId
                    val key = cacheKeys[entityId]
                    if (key != null) {
                        key to document
                    } else {
                        null
                    }
                }.toMap()

        statsBulkCacheManager.putAll(toCache)

        return entityIds.associateWith { entityId ->
            cachedResults[cacheKeys[entityId]] ?: dbResultsMap[entityId]
        }
    }
}
