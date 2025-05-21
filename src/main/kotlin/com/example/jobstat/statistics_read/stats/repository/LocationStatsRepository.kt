package com.example.jobstat.statistics_read.stats.repository

import com.example.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.example.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.example.jobstat.statistics_read.stats.document.LocationStatsDocument
import com.example.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.example.jobstat.statistics_read.stats.registry.StatsType
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.LOCATION)
@NoRepositoryBean
interface LocationStatsRepository : StatsMongoRepository<LocationStatsDocument, String> {
    fun findByCostOfLivingIndexLessThan(index: Double): List<LocationStatsDocument>

    fun findByIndustryIdAndBaseDate(
        industryId: Long,
        baseDate: String,
    ): List<LocationStatsDocument>

    fun findTopByPostingCount(limit: Int): List<LocationStatsDocument>

    fun findByCompanySize(companySize: String): List<LocationStatsDocument>
}

@Repository
class LocationStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<LocationStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<LocationStatsDocument, String>(entityInformation, mongoOperations),
    LocationStatsRepository {
    override fun findByCostOfLivingIndexLessThan(index: Double): List<LocationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.lt("cost_of_living_index", index),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByIndustryIdAndBaseDate(
        industryId: Long,
        baseDate: String,
    ): List<LocationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("industry_distribution.industry_id", industryId),
                    Filters.eq("base_date", baseDate),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopByPostingCount(limit: Int): List<LocationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("stats.posting_count"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByCompanySize(companySize: String): List<LocationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("company_size_distribution.company_size", companySize),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
