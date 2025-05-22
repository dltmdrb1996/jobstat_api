package com.wildrew.app.statistics_read.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.wildrew.app.statistics_read.stats.document.CertificationStatsDocument
import com.wildrew.app.statistics_read.stats.registry.StatsRepositoryType
import com.wildrew.app.statistics_read.stats.registry.StatsType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.CERTIFICATION)
@NoRepositoryBean
interface CertificationStatsRepository : StatsMongoRepository<CertificationStatsDocument, String> {
    fun findByPassRateGreaterThan(rate: Double): List<CertificationStatsDocument>

    fun findByMarketValueScoreGreaterThan(score: Double): List<CertificationStatsDocument>

    fun findByRequiredCount(count: Int): List<CertificationStatsDocument>

    fun findTopByDemandScore(limit: Int): List<CertificationStatsDocument>
}

@Repository
class CertificationStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CertificationStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<CertificationStatsDocument, String>(entityInformation, mongoOperations),
    CertificationStatsRepository {
    override fun findByPassRateGreaterThan(rate: Double): List<CertificationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("exam_metrics.pass_rate", rate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByMarketValueScoreGreaterThan(score: Double): List<CertificationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.market_value_score", score),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByRequiredCount(count: Int): List<CertificationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("stats.required_count", count),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopByDemandScore(limit: Int): List<CertificationStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("market_demand.current_demand.demand_score"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
