package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.BaseRankingDocument
import com.example.jobstat.core.base.mongo.ranking.RankingEntry
import org.springframework.data.repository.NoRepositoryBean
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation

@NoRepositoryBean
interface BaseRankingRepository<T : BaseRankingDocument<E>, E : RankingEntry, ID : Any> : BaseTimeSeriesRepository<T, ID> {
    fun findTopN(
        baseDate: String,
        limit: Int,
    ): List<T>

    fun findByRankRange(
        baseDate: String,
        startRank: Int,
        endRank: Int,
    ): List<T>

    fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T>

    fun findTopLosers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T>

    fun findStableEntities(
        months: Int,
        maxRankChange: Int,
    ): List<T>

    fun findVolatileEntities(
        months: Int,
        minRankChange: Int,
    ): List<T>

    fun findEntitiesWithConsistentRanking(
        months: Int,
        maxRank: Int,
    ): List<T>

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
        baseDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate))
            .sort(Sorts.ascending("rankings.rank"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByRankRange(
        baseDate: String,
        startRank: Int,
        endRank: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.gte("rankings.rank", startRank),
                    Filters.lte("rankings.rank", endRank),
                ),
            ).sort(Sorts.ascending("rankings.rank"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", endDate),
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
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(
                    Filters.and(
                        Filters.eq("base_date", endDate),
                        Filters.exists("rankings.rank_change"),
                    ),
                ),
                Aggregates.sort(Sorts.ascending("rankings.rank_change")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findStableEntities(
        months: Int,
        maxRankChange: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.sort(Sorts.descending("base_date")),
                Aggregates.limit(months),
                Aggregates.match(
                    Filters.or(
                        Filters.exists("rankings.rank_change", false),
                        Filters.and(
                            Filters.gte("rankings.rank_change", -maxRankChange),
                            Filters.lte("rankings.rank_change", maxRankChange),
                        ),
                    ),
                ),
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
                Aggregates.sort(Sorts.descending("base_date")),
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
                Aggregates.sort(Sorts.descending("base_date")),
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