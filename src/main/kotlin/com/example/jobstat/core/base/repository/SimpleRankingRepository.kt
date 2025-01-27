package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
import com.example.jobstat.core.state.BaseDate
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface SimpleRankingRepository<T : SimpleRankingDocument<E>, E : SimpleRankingDocument.SimpleRankingEntry, ID : Any> : BaseRankingRepository<T, E, ID> {
    fun findByValueRange(
        baseDate: BaseDate,
        minValue: Double,
        maxValue: Double,
    ): List<T>

    fun findByGrowthRate(
        baseDate: BaseDate,
        minGrowthRate: Double,
    ): List<T>

    fun findEntitiesWithConsistentGrowth(
        months: Int,
        minGrowthRate: Double,
    ): List<T>

    fun findRisingStars(
        months: Int,
        minRankImprovement: Int,
    ): List<T>

    fun findTrendingEntities(months: Int): List<T>

    fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T?
}

abstract class SimpleRankingRepositoryImpl<T : SimpleRankingDocument<E>, E : SimpleRankingDocument.SimpleRankingEntry, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    SimpleRankingRepository<T, E, ID> {
    /**
     * 특정 엔티티의 기준일자 데이터를 조회합니다.
     *
     * @param entityId 엔티티 ID
     * @param baseDate 기준 날짜
     * @return 해당 엔티티의 랭킹 데이터, 없으면 null
     */
    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.elemMatch(
                        "rankings",
                        Filters.eq("entity_id", entityId),
                    ),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    /**
     * 특정 기준일자에서 지정된 값 범위 내의 엔티티들을 조회합니다.
     * 값을 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 날짜
     * @param minValue 최소값
     * @param maxValue 최대값
     * @return 값 범위 내의 엔티티 목록
     */
    override fun findByValueRange(
        baseDate: BaseDate,
        minValue: Double,
        maxValue: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.gte("rankings.value", minValue),
                    Filters.lte("rankings.value", maxValue),
                ),
            ).sort(Sorts.descending("rankings.value"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 기준일자에서 지정된 성장률 이상의 엔티티들을 조회합니다.
     * 성장률을 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 날짜
     * @param minGrowthRate 최소 성장률
     * @return 지정된 성장률 이상의 엔티티 목록
     */
    override fun findByGrowthRate(
        baseDate: BaseDate,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate.toString())),
                // unwind rankings array to access individual entries
                Aggregates.unwind("\$rankings"),
                // filter by growth_rate
                Aggregates.match(
                    Filters.gte("rankings.growth_rate", minGrowthRate),
                ),
                // sort by growth rate in descending order
                Aggregates.sort(Sorts.descending("rankings.growth_rate")),
                // group back to original document structure
                Aggregates.group(
                    "\$_id",
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.push("rankings", "\$rankings"),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 지정된 기간 동안 일정 성장률 이상을 꾸준히 유지한 엔티티들을 조회합니다.
     * 평균 성장률을 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param months 조회할 개월 수
     * @param minGrowthRate 최소 성장률
     * @return 꾸준한 성장을 보인 엔티티 목록
     */
    override fun findEntitiesWithConsistentGrowth(
        months: Int,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                // 최근 데이터 조회
                Aggregates.sort(Sorts.descending("base_date")),
                Aggregates.limit(months),
                // Unwind rankings 배열
                Aggregates.unwind("\$rankings"),
                // growth_rate 기준으로 필터링
                Aggregates.match(
                    Filters.gte("rankings.growth_rate", minGrowthRate),
                ),
                // entity_id로 그룹화하여 일관된 성장 확인
                Aggregates.group(
                    "\$rankings.entity_id",
                    Accumulators.sum("months_count", 1),
                    Accumulators.avg("avg_growth", "\$rankings.growth_rate"),
                    // 원본 필드들 보존
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.first("rankings", "\$rankings"),
                ),
                // months_count와 avg_growth 조건 체크
                Aggregates.match(
                    Filters.and(
                        Filters.eq("months_count", months),
                        Filters.gte("avg_growth", minGrowthRate),
                    ),
                ),
                // 최종 문서 구조로 재구성
                Aggregates.project(
                    Document(
                        mapOf(
                            "_id" to "\$_id",
                            "base_date" to "\$base_date",
                            "period" to "\$period",
                            "metrics" to "\$metrics",
                            "rankings" to listOf("\$rankings"),
                        ),
                    ),
                ),
                // 평균 성장률 기준으로 정렬
                Aggregates.sort(Sorts.descending("avg_growth")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 최근 순위가 크게 상승한 엔티티들을 조회합니다.
     * 순위 변동을 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param months 조회할 개월 수
     * @param minRankImprovement 최소 순위 상승폭
     * @return 순위가 크게 상승한 엔티티 목록
     */
    override fun findRisingStars(
        months: Int,
        minRankImprovement: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                // 최근 데이터 조회
                Aggregates.match(
                    Filters.and(
                        Filters.exists("rankings.rank_change"),
                        Filters.gte("rankings.rank_change", minRankImprovement),
                    ),
                ),
                // 최근 N개월 데이터만 필터링
                Aggregates.sort(Sorts.descending("base_date")),
                Aggregates.limit(months),
                // rank_change 기준 정렬
                Aggregates.sort(Sorts.descending("rankings.rank_change")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 최근 트렌드를 보이는 엔티티들을 조회합니다.
     * 양의 성장률과 높은 성장 일관성을 보이는 엔티티들을 반환합니다.
     * 성장률을 기준으로 내림차순 정렬됩니다.
     *
     * @param months 조회할 개월 수
     * @return 트렌드를 보이는 엔티티 목록
     */
    override fun findTrendingEntities(months: Int): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                // Get recent data
                Aggregates.sort(Sorts.descending("base_date")),
                Aggregates.limit(months),
                // Unwind rankings array
                Aggregates.unwind("\$rankings"),
                // Check if showing growth trend
                Aggregates.match(
                    Filters.and(
                        Filters.exists("rankings.growth_rate"),
                        Filters.gt("rankings.growth_rate", 0),
                        Filters.exists("rankings.growth_consistency"),
                        Filters.gt("rankings.growth_consistency", 0.5), // 일관성 체크 추가
                    ),
                ),
                // Group back to original document structure
                Aggregates.group(
                    "\$_id",
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.push("rankings", "\$rankings"),
                ),
                // Sort by average growth rate
                Aggregates.sort(Sorts.descending("rankings.growth_rate")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
