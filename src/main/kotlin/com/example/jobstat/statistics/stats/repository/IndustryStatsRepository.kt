package com.example.jobstat.statistics.stats.repository

import com.example.jobstat.core.base.repository.StatsMongoRepository
import com.example.jobstat.core.base.repository.StatsMongoRepositoryImpl
import com.example.jobstat.statistics.stats.model.IndustryStatsDocument
import com.example.jobstat.statistics.stats.registry.StatsRepositoryType
import com.example.jobstat.statistics.stats.registry.StatsType
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
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
