package com.wildrew.jobstat.statistics_read.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.stats.document.BenefitStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface BenefitStatsRepository : StatsMongoRepository<BenefitStatsDocument, String> {
    fun findByProvisionRateGreaterThan(rate: Double): List<BenefitStatsDocument>

    fun findBySatisfactionScoreGreaterThan(score: Double): List<BenefitStatsDocument>

    fun findByIndustryId(industryId: Long): List<BenefitStatsDocument>

    fun findTopByEmployeeSatisfaction(limit: Int): List<BenefitStatsDocument>
}

@Repository
@StatsRepositoryType(StatsType.BENEFIT)
class BenefitStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<BenefitStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<BenefitStatsDocument, String>(entityInformation, mongoOperations),
    BenefitStatsRepository {
    override fun findByProvisionRateGreaterThan(rate: Double): List<BenefitStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.provision_rate", rate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findBySatisfactionScoreGreaterThan(score: Double): List<BenefitStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.satisfaction_score", score),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByIndustryId(industryId: Long): List<BenefitStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("industry_distribution.industry_id", industryId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopByEmployeeSatisfaction(limit: Int): List<BenefitStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("employee_satisfaction.overall_satisfaction"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
