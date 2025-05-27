package com.wildrew.jobstat.statistics_read.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.stats.document.ContractTypeStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.CONTRACT_TYPE)
@NoRepositoryBean
interface ContractTypeStatsRepository : StatsMongoRepository<ContractTypeStatsDocument, String> {
    fun findByMarketShareGreaterThan(share: Double): List<ContractTypeStatsDocument>

    fun findByContractDurationAvgGreaterThan(duration: Double): List<ContractTypeStatsDocument>

    fun findByType(type: String): List<ContractTypeStatsDocument>

    fun findTopByRenewalRate(limit: Int): List<ContractTypeStatsDocument>
}

@Repository
class ContractTypeStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<ContractTypeStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<ContractTypeStatsDocument, String>(entityInformation, mongoOperations),
    ContractTypeStatsRepository {
    override fun findByMarketShareGreaterThan(share: Double): List<ContractTypeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.market_share", share),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByContractDurationAvgGreaterThan(duration: Double): List<ContractTypeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.contract_duration_avg", duration),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByType(type: String): List<ContractTypeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("type", type),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopByRenewalRate(limit: Int): List<ContractTypeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("stats.renewal_rate"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
