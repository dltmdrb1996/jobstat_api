package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.BaseTimeSeriesDocument
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface BaseTimeSeriesRepository<T : BaseTimeSeriesDocument, ID : Any> : BaseMongoRepository<T, ID> {
    fun findByBaseDate(baseDate: String): T?

    fun findByBaseDateBetween(
        startDate: String,
        endDate: String,
    ): List<T>

    fun findLatest(): T?

    fun findLatestN(n: Int): List<T>
}

abstract class BaseTimeSeriesRepositoryImpl<T : BaseTimeSeriesDocument, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseMongoRepositoryImpl<T, ID>(entityInformation, mongoOperations),
    BaseTimeSeriesRepository<T, ID> {
    override fun findByBaseDate(baseDate: String): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate))
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByBaseDateBetween(
        startDate: String,
        endDate: String,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.gte("base_date", startDate),
                    Filters.lte("base_date", endDate),
                ),
            ).sort(Sorts.ascending("base_date"))
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findLatest(): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("base_date"))
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findLatestN(n: Int): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("base_date"))
            .limit(n)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
