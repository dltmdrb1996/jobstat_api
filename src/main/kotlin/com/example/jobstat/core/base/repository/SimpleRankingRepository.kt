package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.SimpleRankingDocument
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
        baseDate: String,
        minValue: Double,
        maxValue: Double,
    ): List<T>

//    fun findByPercentileRange(baseDate: String, minPercentile: Double, maxPercentile: Double): List<T>
    fun findByGrowthRate(
        baseDate: String,
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
        baseDate: String,
    ): T?
}

abstract class SimpleRankingRepositoryImpl<T : SimpleRankingDocument<E>, E : SimpleRankingDocument.SimpleRankingEntry, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    SimpleRankingRepository<T, E, ID> {
    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.elemMatch(
                        "rankings",
                        Filters.eq("entity_id", entityId),
                    ),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByValueRange(
        baseDate: String,
        minValue: Double,
        maxValue: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.gte("rankings.value", minValue),
                    Filters.lte("rankings.value", maxValue),
                ),
            ).sort(Sorts.descending("rankings.value"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

//    override fun findByPercentileRange(baseDate: String, minPercentile: Double, maxPercentile: Double): List<T> {
//        val collection = mongoOperations.getCollection(entityInformation.collectionName)
//
//        // rankings의 percentile 필드가 RankingInfo 내부에 있으므로 경로 수정
//        return collection.find(
//            Filters.and(
//                Filters.eq("base_date", baseDate),
//                Filters.gte("percentile", minPercentile),  // 경로 수정
//                Filters.lte("percentile", maxPercentile)   // 경로 수정
//            )
//        )
//            .sort(Sorts.descending("percentile"))  // 정렬 경로도 수정
//            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
//            .toList()
//    }

    override fun findByGrowthRate(
        baseDate: String,
        minGrowthRate: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
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
