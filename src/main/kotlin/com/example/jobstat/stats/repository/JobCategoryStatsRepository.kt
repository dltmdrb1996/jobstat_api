package com.example.jobstat.stats.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.example.jobstat.core.base.repository.StatsMongoRepository
import com.example.jobstat.core.base.repository.StatsMongoRepositoryImpl
import com.example.jobstat.stats.model.JobCategoryStatsDocument
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface JobCategoryStatsRepository : StatsMongoRepository<JobCategoryStatsDocument, String> {
    fun findByCompetitionRateGreaterThan(rate: Double): List<JobCategoryStatsDocument>

    fun findByAvgExperienceYearsBetween(
        minYears: Double,
        maxYears: Double,
    ): List<JobCategoryStatsDocument>

    fun findByRequiredSkillId(skillId: Long): List<JobCategoryStatsDocument>

    fun findTopByApplicationCount(limit: Int): List<JobCategoryStatsDocument>
}

@Repository
class JobCategoryStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<JobCategoryStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<JobCategoryStatsDocument, String>(entityInformation, mongoOperations),
    JobCategoryStatsRepository {
    override fun findByCompetitionRateGreaterThan(rate: Double): List<JobCategoryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.competition_rate", rate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByAvgExperienceYearsBetween(
        minYears: Double,
        maxYears: Double,
    ): List<JobCategoryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.gte("stats.avg_experience_years", minYears),
                    Filters.lte("stats.avg_experience_years", maxYears),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByRequiredSkillId(skillId: Long): List<JobCategoryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("required_skills.skill_id", skillId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopByApplicationCount(limit: Int): List<JobCategoryStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("stats.application_count"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
