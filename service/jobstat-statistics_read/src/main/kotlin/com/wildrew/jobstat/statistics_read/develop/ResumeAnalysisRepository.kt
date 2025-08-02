package com.wildrew.jobstat.statistics_read.develop

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepositoryImpl
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface ResumeAnalysisRepository : ReferenceMongoRepository<ResumeAnalysisDocument, String> {
    fun findByCandidateId(candidateId: Long): List<ResumeAnalysisDocument>

    fun findLatestAnalysisByCandidateId(candidateId: Long): ResumeAnalysisDocument?

    fun findByMatchedSkillId(skillId: Long): List<ResumeAnalysisDocument>

    fun findByMatchedJobCategoryId(jobCategoryId: Long): List<ResumeAnalysisDocument>
}

@Repository
class ResumeAnalysisRepositoryImpl(
    private val entityInformation: MongoEntityInformation<ResumeAnalysisDocument, String>,
    private val mongoOperations: MongoOperations,
) : ReferenceMongoRepositoryImpl<ResumeAnalysisDocument, String>(entityInformation, mongoOperations),
    ResumeAnalysisRepository {
    override fun findByCandidateId(candidateId: Long): List<ResumeAnalysisDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("reference_id", candidateId),
            ).sort(Sorts.descending("analysis_date"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findLatestAnalysisByCandidateId(candidateId: Long): ResumeAnalysisDocument? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("reference_id", candidateId),
            ).sort(Sorts.descending("analysis_date"))
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByMatchedSkillId(skillId: Long): List<ResumeAnalysisDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("matched_skills.skill_id", skillId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByMatchedJobCategoryId(jobCategoryId: Long): List<ResumeAnalysisDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("matched_job_categories.job_category_id", jobCategoryId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
