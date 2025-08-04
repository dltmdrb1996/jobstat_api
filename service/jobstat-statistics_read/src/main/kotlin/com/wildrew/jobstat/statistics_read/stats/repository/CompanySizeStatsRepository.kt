package com.wildrew.jobstat.statistics_read.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_model.CompanySize
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.stats.document.CompanySizeStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface CompanySizeStatsRepository : StatsMongoRepository<CompanySizeStatsDocument, String> {
    fun findBySize(size: CompanySize): List<CompanySizeStatsDocument>

    fun findByAvgSalaryGreaterThan(salary: Long): List<CompanySizeStatsDocument>

    fun findByBenefitProvisionRateGreaterThan(rate: Double): List<CompanySizeStatsDocument>

    fun findTopByHiringVolume(limit: Int): List<CompanySizeStatsDocument>
}

@Repository
@StatsRepositoryType(StatsType.COMPANY)
class CompanySizeStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanySizeStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<CompanySizeStatsDocument, String>(entityInformation, mongoOperations),
    CompanySizeStatsRepository {
    override fun findBySize(size: CompanySize): List<CompanySizeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("size", size.name),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByAvgSalaryGreaterThan(salary: Long): List<CompanySizeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.avg_salary", salary),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByBenefitProvisionRateGreaterThan(rate: Double): List<CompanySizeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.benefit_provision_rate", rate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopByHiringVolume(limit: Int): List<CompanySizeStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("stats.posting_count"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
