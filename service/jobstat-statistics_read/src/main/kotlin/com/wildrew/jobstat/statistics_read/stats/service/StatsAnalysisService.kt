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
        // 캐시를 통한 조회 활용
        val resultMap = findStatsByEntityIdsAndBaseDate<T>(statsType, baseDate, entityIds)

        // 결과를 리스트 형태로 변환하여 반환 (null 값 제외)
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

        // 캐시에서 먼저 조회
        val cached = statsBulkCacheManager.get<T>(cacheKey)
        if (cached != null) {
            log.trace("Cache hit for latest stats: {}", cacheKey)
            return cached
        }

        // 캐시에 없으면 DB에서 조회 후 캐시에 저장
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

        // 캐시에서 먼저 조회
        val cached = statsBulkCacheManager.get<T>(cacheKey)
        if (cached != null) {
            log.trace("Cache hit for stats by date: {}", cacheKey)
            return cached
        }

        // 캐시에 없으면 DB에서 조회 후 캐시에 저장
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

        // 모든 엔티티에 대한 캐시 키 생성
        val cacheKeys =
            entityIds
                .map { entityId ->
                    entityId to statsBulkCacheManager.createCacheKey(baseDate, statsType, entityId)
                }.toMap()

        // 캐시에서 일괄 조회
        val cachedResults = statsBulkCacheManager.getAll<T>(cacheKeys.values)

        // 캐시 미스된 엔티티 ID 확인
        val missingEntityIds =
            entityIds.filter { entityId ->
                val key = cacheKeys[entityId]
                key !in cachedResults
            }

        // 캐시 미스가 없으면 캐시 결과 바로 반환
        if (missingEntityIds.isEmpty()) {
            log.debug("Bulk cache hit for all {} entities", entityIds.size)
            // 결과 변환: 캐시 키 → 엔티티 ID 기준으로 변환
            return entityIds.associateWith { entityId ->
                cachedResults[cacheKeys[entityId]]
            }
        }

        // 캐시 미스된 엔티티들만 DB에서 일괄 조회
        log.debug("Bulk cache miss for {} entities", missingEntityIds.size)
        val dbResults =
            statsRepositoryRegistry
                .getRepository<T>(statsType)
                .findByBaseDateAndEntityIds(baseDate, missingEntityIds)

        // DB 결과를 엔티티 ID로 매핑
        val dbResultsMap = dbResults.associateBy { it.entityId }

        // DB 결과를 캐시에 일괄 저장
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

        // 캐시 결과와 DB 결과 합쳐서 반환
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

        // 모든 엔티티에 대한 캐시 키 생성
        val cacheKeys =
            entityIds
                .map { entityId ->
                    entityId to statsBulkCacheManager.createCacheKey(baseDate, statsType, entityId)
                }.toMap()

        // 캐시에서 일괄 조회
        val cachedResults = statsBulkCacheManager.getAll<T>(cacheKeys.values)

        // 캐시 미스된 엔티티 ID 확인
        val missingEntityIds =
            entityIds.filter { entityId ->
                val key = cacheKeys[entityId]
                key !in cachedResults
            }

        // 캐시 미스가 없으면 캐시 결과 바로 반환
        if (missingEntityIds.isEmpty()) {
            log.debug("Bulk cache hit for all {} entities", entityIds.size)
            // 결과 변환: 캐시 키 → 엔티티 ID 기준으로 변환
            return entityIds.associateWith { entityId ->
                cachedResults[cacheKeys[entityId]]
            }
        }

        // 캐시 미스된 엔티티들만 DB에서 일괄 조회
        log.debug("Bulk cache miss for {} entities", missingEntityIds.size)
        // findLatestStatsByEntityIds 메소드가 StatsMongoRepository에 없어서 개별 조회로 대체
        val dbResults =
            missingEntityIds.mapNotNull { entityId ->
                val result = statsRepositoryRegistry.getRepository<T>(statsType).findLatestStatsByEntityId(entityId)
                if (result != null) {
                    result
                } else {
                    null
                }
            }

        // DB 결과를 엔티티 ID로 매핑
        val dbResultsMap = dbResults.associateBy { it.entityId }

        // DB 결과를 캐시에 일괄 저장
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

        // 캐시 결과와 DB 결과 합쳐서 반환
        return entityIds.associateWith { entityId ->
            cachedResults[cacheKeys[entityId]] ?: dbResultsMap[entityId]
        }
    }
}
