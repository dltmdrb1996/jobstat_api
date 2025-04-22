package com.example.jobstat.core.cache

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.cache.CacheConfig.Companion.EXPIRE_AFTER_ACCESS
import com.example.jobstat.core.cache.CacheConfig.Companion.STATS_CACHE_SIZE
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.statistics_read.stats.registry.StatsType
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface StatsBulkCacheManager {
    fun put(
        key: String,
        document: BaseStatsDocument,
    )

    fun putAll(documentsMap: Map<String, BaseStatsDocument>)

    fun <T : BaseStatsDocument> get(key: String): T?

    fun <T : BaseStatsDocument> getAll(keys: Collection<String>): Map<String, T>

    fun createCacheKey(
        baseDate: BaseDate,
        statsType: StatsType,
        entityId: Long,
    ): String

    fun getCacheStats(): String

    fun invalidateAll()
}

@Component
class StatsBulkCacheManagerImpl : StatsBulkCacheManager {
    private val log = LoggerFactory.getLogger(this::class.java)

    // 별도의 타입 안전한 캐시 인스턴스 생성 (필요한 타입 캐스팅 최소화)
    private val statsDocumentCache: Cache<String, BaseStatsDocument> =
        Caffeine
            .newBuilder()
            .maximumSize(STATS_CACHE_SIZE)
            .expireAfterWrite(EXPIRE_AFTER_ACCESS)
            .recordStats()
            .build()

    override fun put(
        key: String,
        document: BaseStatsDocument,
    ) {
        statsDocumentCache.put(key, document)
    }

    override fun putAll(documentsMap: Map<String, BaseStatsDocument>) {
        statsDocumentCache.putAll(documentsMap)
        log.debug("Bulk cached {} documents", documentsMap.size)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BaseStatsDocument> get(key: String): T? = statsDocumentCache.getIfPresent(key) as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T : BaseStatsDocument> getAll(keys: Collection<String>): Map<String, T> {
        // 타입 변환 없이 직접 bulk 조회
        val result = statsDocumentCache.getAllPresent(keys)
        return result.mapValues { it.value as T }
    }

    override fun createCacheKey(
        baseDate: BaseDate,
        statsType: StatsType,
        entityId: Long,
    ): String = "$baseDate:$statsType:$entityId"

    override fun getCacheStats(): String {
        val stats = statsDocumentCache.stats()
        return """
            StatsDocument 캐시 통계:
            - 히트: ${stats.hitCount()}
            - 미스: ${stats.missCount()}
            - 히트율: ${String.format("%.2f", stats.hitRate() * 100)}%
            - 제거: ${stats.evictionCount()}
            """.trimIndent()
    }

    override fun invalidateAll() {
        statsDocumentCache.invalidateAll()
    }

//    @Scheduled(fixedRate = 5000) // 1분마다 로깅
//    fun logBulkCacheStats() {
//        val stats = statsDocumentCache.stats()
//        log.debug(
//            """
//        StatsDocument Bulk 캐시 통계:
//        - 히트: ${stats.hitCount()}
//        - 미스: ${stats.missCount()}
//        - 히트율: ${String.format("%.2f", stats.hitRate() * 100)}%
//        - 제거: ${stats.evictionCount()}
//        """.trimIndent()
//        )
//    }
}
