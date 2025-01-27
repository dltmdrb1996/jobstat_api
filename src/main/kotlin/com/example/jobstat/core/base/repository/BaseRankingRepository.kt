package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.BaseRankingDocument
import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import com.example.jobstat.core.state.BaseDate
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface BaseRankingRepository<T : BaseRankingDocument<E>, E : RankingEntry, ID : Any> : BaseTimeSeriesRepository<T, ID> {
    /**
     * 특정 기준일자의 상위 N개의 랭킹 데이터를 조회합니다.
     * 랭킹 순위를 기준으로 오름차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 일자
     * @param limit 조회할 데이터 수
     * @return 상위 N개의 랭킹 엔티티 리스트
     */
    fun findTopN(
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    /**
     * 특정 기준일자의 시작 순위부터 종료 순위 사이의 랭킹 데이터를 조회합니다.
     * 랭킹 순위를 기준으로 오름차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 일자
     * @param startRank 시작 순위
     * @param endRank 종료 순위
     * @return 해당 순위 범위의 랭킹 엔티티 리스트
     */
    fun findByRankRange(
        baseDate: BaseDate,
        startRank: Int,
        endRank: Int,
    ): List<T>

    /**
     * 특정 기준일자의 순위 상승폭이 가장 큰 엔티티들을 조회합니다.
     * 순위 변동(rank\_change)을 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 일자
     * @param limit 조회할 데이터 수
     * @return 순위 상승폭이 가장 큰 엔티티 리스트
     */
    fun findTopMovers(
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    /**
     * 특정 기준일자의 순위 하락폭이 가장 큰 엔티티들을 조회합니다.
     * 순위 변동(rank\_change)을 기준으로 오름차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 일자
     * @param limit 조회할 데이터 수
     * @return 순위 하락폭이 가장 큰 엔티티 리스트
     */
    fun findTopLosers(
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    /**
     * 지정된 기간 동안 순위 변동이 큰(변동성이 높은) 엔티티들을 조회합니다.
     * 최근 순으로 정렬된 데이터 중에서 지정된 순위 변동 폭 이상의 변화가 있는 엔티티들을 반환합니다.
     *
     * @param months 조회할 개월 수
     * @param minRankChange 최소 순위 변동 폭
     * @return 순위 변동성이 높은 엔티티 리스트
     */
    fun findVolatileEntities(
        months: Int,
        minRankChange: Int,
    ): List<T>

    /**
     * 지정된 기간 동안 일정 순위 이내를 꾸준히 유지한 엔티티들을 조회합니다.
     * 해당 기간 동안 지정된 최대 순위 이하를 계속 유지한 엔티티들을 반환합니다.
     *
     * @param months 조회할 개월 수
     * @param maxRank 최대 순위 (이 순위 이하를 유지해야 함)
     * @return 일정 순위를 꾸준히 유지한 엔티티 리스트
     */
    fun findEntitiesWithConsistentRanking(
        months: Int,
        maxRank: Int,
    ): List<T>

    /**
     * 특정 엔티티의 지정된 기간 동안의 순위 이력을 조회합니다.
     * 최근 순으로 정렬하여 해당 엔티티의 순위 변동 이력을 반환합니다.
     *
     * @param entityId 엔티티 ID
     * @param months 조회할 개월 수
     * @return 엔티티의 순위 이력 리스트
     */
    fun findRankingHistory(
        entityId: Long,
        months: Int,
    ): List<T>
}

abstract class BaseRankingRepositoryImpl<T : BaseRankingDocument<E>, E : RankingEntry, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseTimeSeriesRepositoryImpl<T, ID>(entityInformation, mongoOperations),
    BaseRankingRepository<T, E, ID> {
    override fun findTopN(
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate.toString()))
            .sort(Sorts.ascending("rankings.rank")) // rank 오름차순 정렬 추가
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByRankRange(
        baseDate: BaseDate,
        startRank: Int,
        endRank: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.gte("rankings.rank", startRank),
                    Filters.lte("rankings.rank", endRank),
                ),
            ).sort(Sorts.ascending("rankings.rank")) // rank 오름차순 정렬 추가
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopMovers(
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate.toString()),
                        Filters.exists("rankings.rank_change"),
                    ),
                ),
                Aggregates.sort(Sorts.descending("rankings.rank_change")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopLosers(
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate.toString()),
                        Filters.exists("rankings.rank_change"),
                    ),
                ),
                Aggregates.sort(Sorts.ascending("rankings.rank_change")), // rank_change 오름차순 정렬 추가
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findVolatileEntities(
        months: Int,
        minRankChange: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.limit(months),
                Aggregates.match(
                    Filters.or(
                        Filters.lte("rankings.rank_change", -minRankChange),
                        Filters.gte("rankings.rank_change", minRankChange),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findEntitiesWithConsistentRanking(
        months: Int,
        maxRank: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.limit(months),
                Aggregates.match(Filters.lte("rankings.rank", maxRank)),
                Aggregates.group(
                    "\$rankings.entity_id",
                    Accumulators.sum("count", 1),
                ),
                Aggregates.match(Filters.eq("count", months)),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findRankingHistory(
        entityId: Long,
        months: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.elemMatch(
                    "rankings",
                    Filters.eq("entity_id", entityId),
                ),
            ).sort(Sorts.descending("base_date"))
            .limit(months)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
