package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.RelationshipRankingDocument
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
interface RelationshipRankingRepository<
    T : RelationshipRankingDocument<E>,
    E : RelationshipRankingDocument.RelationshipRankingEntry,
    ID : Any,
> : BaseRankingRepository<T, E, ID> {
    fun findByPrimaryEntityId(
        primaryEntityId: Long,
        baseDate: BaseDate,
    ): T?

    fun findTopNRelatedEntities(
        primaryEntityId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    fun findByRelatedEntityId(
        relatedEntityId: Long,
        baseDate: BaseDate,
    ): List<T>

    fun findStrongRelationships(
        baseDate: BaseDate,
        minScore: Double,
    ): List<T>

    fun findGrowingRelationships(
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<T>

    fun findCommonRelationships(
        primaryEntityId1: Long,
        primaryEntityId2: Long,
        baseDate: BaseDate,
    ): List<T>

    fun findStrongestPairs(
        baseDate: BaseDate,
        limit: Int,
    ): List<T>

    fun findRelationshipTrends(
        primaryEntityId: Long,
        months: Int,
    ): List<T>

    fun findEmergingRelationships(
        months: Int,
        minGrowthRate: Double,
    ): List<T>

    fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T?
}

abstract class RelationshipRankingRepositoryImpl<
    T : RelationshipRankingDocument<E>,
    E : RelationshipRankingDocument.RelationshipRankingEntry,
    ID : Any,
>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    RelationshipRankingRepository<T, E, ID> {
    /**
     * 지정된 주요 엔티티와 기준일자에 해당하는 관계 정보를 조회합니다.
     *
     * @param primaryEntityId 주요 엔티티 ID
     * @param baseDate 기준 날짜
     * @return 해당 엔티티의 관계 정보, 없으면 null
     */
    override fun findByPrimaryEntityId(
        primaryEntityId: Long,
        baseDate: BaseDate,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.eq("rankings.primary_entity_id", primaryEntityId),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    /**
     * 지정된 주요 엔티티와 가장 강한 관계를 가진 상위 N개의 엔티티를 조회합니다.
     * 관계 점수(score)를 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param primaryEntityId 주요 엔티티 ID
     * @param baseDate 기준 날짜
     * @param limit 조회할 관련 엔티티 수
     * @return 상위 N개의 관련 엔티티 목록
     */
    override fun findTopNRelatedEntities(
        primaryEntityId: Long,
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate.toString()),
                        Filters.eq("rankings.primary_entity_id", primaryEntityId),
                    ),
                ),
                Aggregates.unwind("\$rankings"),
                Aggregates.match(Filters.eq("rankings.primary_entity_id", primaryEntityId)),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.sort(Sorts.descending("rankings.related_rankings.score")),
                Aggregates.limit(limit),
                Aggregates.group(
                    "\$_id",
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.first("primary_entity_type", "\$primary_entity_type"),
                    Accumulators.first("related_entity_type", "\$related_entity_type"),
                    Accumulators.push(
                        "rankings",
                        Document(
                            "\$mergeObjects",
                            listOf(
                                "\$rankings",
                                Document("related_rankings", listOf("\$rankings.related_rankings")),
                            ),
                        ),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 지정된 관련 엔티티가 포함된 모든 관계를 조회합니다.
     *
     * @param relatedEntityId 관련 엔티티 ID
     * @param baseDate 기준 날짜
     * @return 해당 관련 엔티티가 포함된 관계 목록
     */
    override fun findByRelatedEntityId(
        relatedEntityId: Long,
        baseDate: BaseDate,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.elemMatch(
                        "rankings.related_rankings",
                        Filters.eq("entity_id", relatedEntityId),
                    ),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 최소 점수 이상의 강한 관계를 가진 엔티티들을 조회합니다.
     * 관계 점수를 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 날짜
     * @param minScore 최소 관계 점수
     * @return 강한 관계를 가진 엔티티 목록
     */
    override fun findStrongRelationships(
        baseDate: BaseDate,
        minScore: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate.toString())),
                Aggregates.unwind("\$rankings"),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.match(
                    Filters.gte("rankings.related_rankings.score", minScore),
                ),
                Aggregates.sort(Sorts.descending("rankings.related_rankings.score")),
                Aggregates.group(
                    "\$_id",
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.first("primary_entity_type", "\$primary_entity_type"),
                    Accumulators.first("related_entity_type", "\$related_entity_type"),
                    Accumulators.push(
                        "rankings",
                        Document(
                            "\$mergeObjects",
                            listOf(
                                "\$rankings",
                                Document("related_rankings", listOf("\$rankings.related_rankings")),
                            ),
                        ),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 시작일자와 종료일자 사이에 성장하는 관계를 가진 엔티티들을 조회합니다.
     * 성장률을 기준으로 내림차순 정렬하여 상위 N개를 반환합니다.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param limit 조회할 엔티티 수
     * @return 성장하는 관계를 가진 상위 N개 엔티티 목록
     */
    override fun findGrowingRelationships(
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                // 종료 날짜의 데이터를 기준으로 함
                Aggregates.match(Filters.eq("base_date", endDate.toString())),
                Aggregates.unwind("\$rankings"),
                Aggregates.unwind("\$rankings.related_rankings"),
                // growth_rate가 양수인 관계만 필터링
                Aggregates.match(Filters.gt("rankings.related_rankings.growth_rate", 0)),
                // 성장률 기준으로 정렬
                Aggregates.sort(Sorts.descending("rankings.related_rankings.growth_rate")),
                Aggregates.limit(limit),
                // 결과를 원래 문서 형태로 재구성
                Aggregates.group(
                    "\$_id",
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.first("primary_entity_type", "\$primary_entity_type"),
                    Accumulators.first("related_entity_type", "\$related_entity_type"),
                    Accumulators.push(
                        "rankings",
                        Document(
                            "\$mergeObjects",
                            listOf(
                                "\$rankings",
                                Document("related_rankings", listOf("\$rankings.related_rankings")),
                            ),
                        ),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 두 주요 엔티티가 공통으로 가지는 관계들을 조회합니다.
     * 평균 관계 점수를 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param primaryEntityId1 첫 번째 주요 엔티티 ID
     * @param primaryEntityId2 두 번째 주요 엔티티 ID
     * @param baseDate 기준 날짜
     * @return 공통 관계를 가진 엔티티 목록
     */
    override fun findCommonRelationships(
        primaryEntityId1: Long,
        primaryEntityId2: Long,
        baseDate: BaseDate,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate.toString())),
                Aggregates.match(
                    Filters.or(
                        Filters.eq("rankings.primary_entity_id", primaryEntityId1),
                        Filters.eq("rankings.primary_entity_id", primaryEntityId2),
                    ),
                ),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.group(
                    "\$rankings.related_rankings.entity_id",
                    Accumulators.sum("count", 1),
                    Accumulators.avg("avg_score", "\$rankings.related_rankings.score"),
                ),
                Aggregates.match(Filters.eq("count", 2)),
                Aggregates.sort(Sorts.descending("avg_score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 가장 강한 관계를 가진 엔티티 쌍들을 조회합니다.
     * 관계 점수를 기준으로 내림차순 정렬하여 상위 N개를 반환합니다.
     *
     * @param baseDate 기준 날짜
     * @param limit 조회할 엔티티 쌍의 수
     * @return 가장 강한 관계를 가진 엔티티 쌍 목록
     */
    override fun findStrongestPairs(
        baseDate: BaseDate,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate.toString())),
                Aggregates.unwind("\$rankings"),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.sort(Sorts.descending("rankings.related_rankings.score")),
                Aggregates.limit(limit),
                Aggregates.group(
                    "\$_id",
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.first("primary_entity_type", "\$primary_entity_type"),
                    Accumulators.first("related_entity_type", "\$related_entity_type"),
                    Accumulators.push(
                        "rankings",
                        Document(
                            "\$mergeObjects",
                            listOf(
                                "\$rankings",
                                Document("related_rankings", listOf("\$rankings.related_rankings")),
                            ),
                        ),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티의 기준일자 데이터를 조회합니다.
     * 엔티티는 주요 엔티티이거나 관련 엔티티일 수 있습니다.
     *
     * @param entityId 엔티티 ID
     * @param baseDate 기준 날짜
     * @return 해당 엔티티의 관계 데이터, 없으면 null
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
                    Filters.or(
                        Filters.eq("rankings.primary_entity_id", entityId),
                        Filters.elemMatch(
                            "rankings.related_rankings",
                            Filters.eq("entity_id", entityId),
                        ),
                    ),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    /**
     * 특정 주요 엔티티의 관계 트렌드를 조회합니다.
     * 최근 데이터부터 지정된 개월 수만큼의 관계 변화를 보여줍니다.
     *
     * @param primaryEntityId 주요 엔티티 ID
     * @param months 조회할 개월 수
     * @return 엔티티의 관계 트렌드 데이터
     */
    override fun findRelationshipTrends(
        primaryEntityId: Long,
        months: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("rankings.primary_entity_id", primaryEntityId),
            ).sort(Sorts.descending("base_date"))
            .limit(months)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 최근 급부상하는 관계들을 조회합니다.
     * 지정된 기간 동안 최소 성장률 이상의 성장을 보인 관계들을 반환합니다.
     * 성장률을 기준으로 내림차순 정렬됩니다.
     *
     * @param months 조회할 개월 수
     * @param minGrowthRate 최소 성장률
     * @return 급부상하는 관계들의 목록
     */
    override fun findEmergingRelationships(
        months: Int,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                // 최근 데이터부터 조회
                Aggregates.sort(Sorts.descending("base_date")),
                Aggregates.limit(months),
                // 문서 구조 풀기
                Aggregates.unwind("\$rankings"),
                Aggregates.unwind("\$rankings.related_rankings"),
                // growth_rate가 최소값 이상인 것만 필터링
                Aggregates.match(
                    Filters.gte("rankings.related_rankings.growth_rate", minGrowthRate),
                ),
                // 결과를 원래 문서 구조로 재구성
                Aggregates.group(
                    "\$_id",
                    Accumulators.first("base_date", "\$base_date"),
                    Accumulators.first("period", "\$period"),
                    Accumulators.first("metrics", "\$metrics"),
                    Accumulators.first("primary_entity_type", "\$primary_entity_type"),
                    Accumulators.first("related_entity_type", "\$related_entity_type"),
                    Accumulators.push(
                        "rankings",
                        Document(
                            "\$mergeObjects",
                            listOf(
                                "\$rankings",
                                Document("related_rankings", listOf("\$rankings.related_rankings")),
                            ),
                        ),
                    ),
                ),
                // growth_rate 기준으로 정렬
                Aggregates.sort(
                    Sorts.descending("rankings.related_rankings.growth_rate"),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
