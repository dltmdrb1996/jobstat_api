package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.RelationshipRankingDocument
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface RelationshipRankingRepository<T : RelationshipRankingDocument<E>, E : RelationshipRankingDocument.RelationshipRankingEntry, ID : Any> : BaseRankingRepository<T, E, ID> {
    fun findByPrimaryEntityId(
        primaryEntityId: Long,
        baseDate: String,
    ): T?

    fun findTopNRelatedEntities(
        primaryEntityId: Long,
        baseDate: String,
        limit: Int,
    ): List<T>

    fun findByRelatedEntityId(
        relatedEntityId: Long,
        baseDate: String,
    ): List<T>

    fun findStrongRelationships(
        baseDate: String,
        minScore: Double,
    ): List<T>

    fun findGrowingRelationships(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T>

    fun findCommonRelationships(
        primaryEntityId1: Long,
        primaryEntityId2: Long,
        baseDate: String,
    ): List<T>

    fun findStrongestPairs(
        baseDate: String,
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
        baseDate: String,
    ): T?
}

abstract class RelationshipRankingRepositoryImpl<T : RelationshipRankingDocument<E>, E : RelationshipRankingDocument.RelationshipRankingEntry, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    RelationshipRankingRepository<T, E, ID> {
    override fun findByPrimaryEntityId(
        primaryEntityId: Long,
        baseDate: String,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.eq("rankings.primary_entity_id", primaryEntityId),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findTopNRelatedEntities(
        primaryEntityId: Long,
        baseDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", baseDate),
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

    override fun findByRelatedEntityId(
        relatedEntityId: Long,
        baseDate: String,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.elemMatch(
                        "rankings.related_rankings",
                        Filters.eq("entity_id", relatedEntityId),
                    ),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findStrongRelationships(
        baseDate: String,
        minScore: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
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

    override fun findGrowingRelationships(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline = listOf(
            // 종료 날짜의 데이터를 기준으로 함
            Aggregates.match(Filters.eq("base_date", endDate)),
            Aggregates.unwind("\$rankings"),
            Aggregates.unwind("\$rankings.related_rankings"),
            // growth_rate가 양수인 관계만 필터링 (SkillRank의 growthRate 필드 사용)
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
                            Document("related_rankings", listOf("\$rankings.related_rankings"))
                        )
                    )
                )
            )
        )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findCommonRelationships(
        primaryEntityId1: Long,
        primaryEntityId2: Long,
        baseDate: String,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
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

    override fun findStrongestPairs(
        baseDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
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

    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
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

    override fun findEmergingRelationships(
        months: Int,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline = listOf(
            // 최근 데이터부터 조회
            Aggregates.sort(Sorts.descending("base_date")),
            Aggregates.limit(months),
            // 문서 구조 풀기
            Aggregates.unwind("\$rankings"),
            Aggregates.unwind("\$rankings.related_rankings"),
            // growth_rate가 최소값 이상인 것만 필터링
            Aggregates.match(
                Filters.gte("rankings.related_rankings.growth_rate", minGrowthRate)
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
                            Document("related_rankings", listOf("\$rankings.related_rankings"))
                        )
                    )
                )
            ),
            // growth_rate 기준으로 정렬
            Aggregates.sort(
                Sorts.descending("rankings.related_rankings.growth_rate")
            )
        )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
