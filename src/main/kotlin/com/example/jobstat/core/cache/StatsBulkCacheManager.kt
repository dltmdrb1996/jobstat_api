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

/**
 * BaseStatsDocument 타입의 객체들을 bulk로 캐싱하기 위한 매니저
 * 최적화된 bulk 연산을 위해 별도의 캐시 인스턴스를 사용
 */
interface StatsBulkCacheManager {
    /**
     * 개별 문서를 캐시에 저장
     * @param key 캐시 키
     * @param document 저장할 문서
     */
    fun put(
        key: String,
        document: BaseStatsDocument,
    )

    /**
     * 여러 문서를 한번에 캐시에 저장
     * @param documentsMap 캐시 키와 문서의 맵
     */
    fun putAll(documentsMap: Map<String, BaseStatsDocument>)

    /**
     * 캐시에서 문서 조회
     * @param key 캐시 키
     * @return 캐시된 문서 또는 null
     */
    fun <T : BaseStatsDocument> get(key: String): T?

    /**
     * 여러 키에 대한 문서를 한번에 조회
     * @param keys 조회할 키 목록
     * @return 캐시된 문서 맵
     */
    fun <T : BaseStatsDocument> getAll(keys: Collection<String>): Map<String, T>

    /**
     * 캐시 키 생성 헬퍼 메소드
     */
    fun createCacheKey(
        baseDate: BaseDate,
        statsType: StatsType,
        entityId: Long,
    ): String

    /**
     * 캐시 통계 조회
     */
    fun getCacheStats(): String

    /**
     * 캐시 내용 초기화
     */
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

    /**
     * 개별 문서를 캐시에 저장
     * @param key 캐시 키
     * @param document 저장할 문서
     */
    override fun put(
        key: String,
        document: BaseStatsDocument,
    ) {
        statsDocumentCache.put(key, document)

        // Spring Cache와 동기화 (선택적으로 구현 가능)
        // cacheManager.getCache(CacheConfig.STATS_DOCUMENT_CACHE)?.put(key, document)
    }

    /**
     * 여러 문서를 한번에 캐시에 저장
     * @param documentsMap 캐시 키와 문서의 맵
     */
    override fun putAll(documentsMap: Map<String, BaseStatsDocument>) {
        // 타입 변환 없이 직접 bulkCache에 저장
        statsDocumentCache.putAll(documentsMap)
        log.debug("Bulk cached {} documents", documentsMap.size)

        // Spring Cache와 동기화 (선택적으로 구현 가능)
        // documentsMap.forEach { (key, value) ->
        //     cacheManager.getCache(CacheConfig.STATS_DOCUMENT_CACHE)?.put(key, value)
        // }
    }

    /**
     * 캐시에서 문서 조회
     * @param key 캐시 키
     * @return 캐시된 문서 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : BaseStatsDocument> get(key: String): T? = statsDocumentCache.getIfPresent(key) as T?

    /**
     * 여러 키에 대한 문서를 한번에 조회
     * @param keys 조회할 키 목록
     * @return 캐시된 문서 맵
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : BaseStatsDocument> getAll(keys: Collection<String>): Map<String, T> {
        // 타입 변환 없이 직접 bulk 조회
        val result = statsDocumentCache.getAllPresent(keys)
        return result.mapValues { it.value as T }
    }

    /**
     * 캐시 키 생성 헬퍼 메소드
     */
    override fun createCacheKey(
        baseDate: BaseDate,
        statsType: StatsType,
        entityId: Long,
    ): String {
        // @Cacheable 어노테이션에서 사용하는 것과 동일한 키 구조 사용
        return "$baseDate:$statsType:$entityId"
    }

    /**
     * 캐시 통계 조회
     */
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

    /**
     * 캐시 내용 초기화
     */
    override fun invalidateAll() {
        statsDocumentCache.invalidateAll()
    }

//    @Scheduled(fixedRate = 5000) // 1분마다 로깅
//    fun logBulkCacheStats() {
//        val stats = statsDocumentCache.stats()
//        log.info(
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
