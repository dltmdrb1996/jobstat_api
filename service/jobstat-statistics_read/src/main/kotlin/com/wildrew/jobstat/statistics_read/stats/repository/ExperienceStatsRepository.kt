package com.wildrew.jobstat.statistics_read.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.stats.document.ExperienceStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.EXPERIENCE)
@NoRepositoryBean
interface ExperienceStatsRepository : StatsMongoRepository<ExperienceStatsDocument, String> {
    fun findByRange(range: String): List<ExperienceStatsDocument>

    fun findByHiringRateGreaterThan(rate: Double): List<ExperienceStatsDocument>

    fun findByMarketDemandIndexGreaterThan(index: Double): List<ExperienceStatsDocument>

    fun findTopBySalaryGrowth(limit: Int): List<ExperienceStatsDocument>
}

@Repository
class ExperienceStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<ExperienceStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<ExperienceStatsDocument, String>(entityInformation, mongoOperations),
    ExperienceStatsRepository {
    override fun findByRange(range: String): List<ExperienceStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("range", range),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByHiringRateGreaterThan(rate: Double): List<ExperienceStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.hiring_rate", rate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByMarketDemandIndexGreaterThan(index: Double): List<ExperienceStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.market_demand_index", index),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopBySalaryGrowth(limit: Int): List<ExperienceStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("salary_metrics.salary_growth.annual_increase_rate"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
