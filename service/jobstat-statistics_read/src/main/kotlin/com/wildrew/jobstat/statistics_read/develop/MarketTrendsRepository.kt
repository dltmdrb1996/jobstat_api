package com.wildrew.jobstat.statistics_read.develop

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseMongoRepositoryImpl
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface MarketTrendsRepository : BaseMongoRepository<MarketTrendsDocument, String> {
    fun findLatestTrends(): MarketTrendsDocument?

    fun findByCategory(category: String): List<MarketTrendsDocument>

    fun findTopGrowingTechnologies(limit: Int): List<MarketTrendsDocument>

    fun findTrendsByDateRange(
        startDate: String,
        endDate: String,
    ): List<MarketTrendsDocument>
}

@Repository
class MarketTrendsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<MarketTrendsDocument, String>,
    private val mongoOperations: MongoOperations,
) : BaseMongoRepositoryImpl<MarketTrendsDocument, String>(entityInformation, mongoOperations),
    MarketTrendsRepository {
    override fun findLatestTrends(): MarketTrendsDocument? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("base_date"))
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByCategory(category: String): List<MarketTrendsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("emerging_technologies.category", category),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopGrowingTechnologies(limit: Int): List<MarketTrendsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("emerging_technologies.growth_rate"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTrendsByDateRange(
        startDate: String,
        endDate: String,
    ): List<MarketTrendsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.gte("base_date", startDate),
                    Filters.lte("base_date", endDate),
                ),
            ).sort(Sorts.ascending("base_date"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
