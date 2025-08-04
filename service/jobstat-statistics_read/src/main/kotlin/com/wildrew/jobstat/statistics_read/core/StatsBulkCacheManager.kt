package com.wildrew.jobstat.statistics_read.core

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.statistics_read.core.CacheConfig.Companion.EXPIRE_AFTER_ACCESS
import com.wildrew.jobstat.statistics_read.core.CacheConfig.Companion.STATS_CACHE_SIZE
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
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
}
