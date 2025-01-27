package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.BaseTimeSeriesDocument
import com.example.jobstat.core.state.BaseDate
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface BaseTimeSeriesRepository<T : BaseTimeSeriesDocument, ID : Any> : BaseMongoRepository<T, ID> {
    fun findByBaseDate(baseDate: BaseDate): T?

    fun findByBaseDateBetween(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<T>

    fun findLatest(): T?

    fun findLatestN(n: Int): List<T>
}

abstract class BaseTimeSeriesRepositoryImpl<T : BaseTimeSeriesDocument, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseMongoRepositoryImpl<T, ID>(entityInformation, mongoOperations),
    BaseTimeSeriesRepository<T, ID> {
    /**
     * 특정 기준일자의 데이터를 조회합니다.
     *
     * @param baseDate 조회할 기준일자
     * @return 해당 기준일자의 데이터
     */
    override fun findByBaseDate(baseDate: BaseDate): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("base_date", baseDate.toString()))
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByBaseDateBetween(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.gte("base_date", startDate.toString()),
                    Filters.lte("base_date", endDate.toString()),
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
