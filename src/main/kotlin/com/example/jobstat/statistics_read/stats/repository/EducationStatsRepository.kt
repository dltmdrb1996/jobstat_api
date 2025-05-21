package com.example.jobstat.statistics_read.stats.repository

import com.example.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.example.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.example.jobstat.statistics_read.stats.document.EducationStatsDocument
import com.example.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.example.jobstat.statistics_read.stats.registry.StatsType
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.EDUCATION)
@NoRepositoryBean
interface EducationStatsRepository : StatsMongoRepository<EducationStatsDocument, String> {
    fun findByRequirementRateGreaterThan(rate: Double): List<EducationStatsDocument>

    fun findByLevel(level: String): List<EducationStatsDocument>

    fun findByMarketValueIndexGreaterThan(index: Double): List<EducationStatsDocument>

    fun findTopByEmploymentRate(limit: Int): List<EducationStatsDocument>
}

@Repository
class EducationStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<EducationStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<EducationStatsDocument, String>(entityInformation, mongoOperations),
    EducationStatsRepository {
    override fun findByRequirementRateGreaterThan(rate: Double): List<EducationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.requirement_rate", rate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByLevel(level: String): List<EducationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("level", level),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByMarketValueIndexGreaterThan(index: Double): List<EducationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.market_value_index", index),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopByEmploymentRate(limit: Int): List<EducationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("stats.degree_holder_employment_rate"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
