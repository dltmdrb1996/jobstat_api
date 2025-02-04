package com.example.jobstat.statistics.stats.repository

import com.example.jobstat.core.base.repository.StatsMongoRepository
import com.example.jobstat.core.base.repository.StatsMongoRepositoryImpl
import com.example.jobstat.statistics.stats.document.BenefitStatsDocument
import com.example.jobstat.statistics.stats.registry.StatsRepositoryType
import com.example.jobstat.statistics.stats.registry.StatsType
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.BENEFIT)
@NoRepositoryBean
interface BenefitStatsRepository : StatsMongoRepository<BenefitStatsDocument, String> {
    fun findByProvisionRateGreaterThan(rate: Double): List<BenefitStatsDocument>

    fun findBySatisfactionScoreGreaterThan(score: Double): List<BenefitStatsDocument>

    fun findByIndustryId(industryId: Long): List<BenefitStatsDocument>

    fun findTopByEmployeeSatisfaction(limit: Int): List<BenefitStatsDocument>
}

@Repository
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
