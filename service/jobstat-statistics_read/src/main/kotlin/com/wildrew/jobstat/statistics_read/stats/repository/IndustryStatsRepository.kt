package com.wildrew.jobstat.statistics_read.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.stats.document.IndustryStatsDocument
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.INDUSTRY)
@NoRepositoryBean
interface IndustryStatsRepository : StatsMongoRepository<IndustryStatsDocument, String> {
    fun findBySkillId(skillId: Long): List<IndustryStatsDocument>

    fun findTopIndustriesByGrowthRate(limit: Int): List<IndustryStatsDocument>

    fun findByLocationIdAndBaseDate(
        locationId: Long,
        baseDate: String,
    ): List<IndustryStatsDocument>

    fun findByCertificationId(certificationId: Long): List<IndustryStatsDocument>

    fun findByEducationLevel(educationLevel: String): List<IndustryStatsDocument>
}

@Repository
class IndustryStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<IndustryStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<IndustryStatsDocument, String>(entityInformation, mongoOperations),
    IndustryStatsRepository {
    override fun findBySkillId(skillId: Long): List<IndustryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("skill_distribution.skill_id", skillId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopIndustriesByGrowthRate(limit: Int): List<IndustryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("stats.growth_rate"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByLocationIdAndBaseDate(
        locationId: Long,
        baseDate: String,
    ): List<IndustryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("location_distribution.location_id", locationId),
                    Filters.eq("base_date", baseDate),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByCertificationId(certificationId: Long): List<IndustryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("certification_requirements.certification_id", certificationId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByEducationLevel(educationLevel: String): List<IndustryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("education_distribution.education_level", educationLevel),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
