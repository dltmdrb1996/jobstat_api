package com.wildrew.jobstat.statistics_read.develop

import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Aggregates.sample
import com.mongodb.client.model.Filters
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepositoryImpl
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface InterviewQuestionRepository : ReferenceMongoRepository<InterviewQuestionDocument, String> {
    fun findByJobCategoryId(jobCategoryId: Long): List<InterviewQuestionDocument>

    fun findByDifficulty(difficulty: String): List<InterviewQuestionDocument>

    fun findBySkillsTested(skill: String): List<InterviewQuestionDocument>

    fun searchByKeyword(keyword: String): List<InterviewQuestionDocument>

    fun findRandomQuestions(
        jobCategoryId: Long,
        count: Int,
    ): List<InterviewQuestionDocument>
}

@Repository
class InterviewQuestionRepositoryImpl(
    private val entityInformation: MongoEntityInformation<InterviewQuestionDocument, String>,
    private val mongoOperations: MongoOperations,
) : ReferenceMongoRepositoryImpl<InterviewQuestionDocument, String>(entityInformation, mongoOperations),
    InterviewQuestionRepository {
    init {
    }

    override fun findByJobCategoryId(jobCategoryId: Long): List<InterviewQuestionDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("job_category_id", jobCategoryId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByDifficulty(difficulty: String): List<InterviewQuestionDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("questions.difficulty", difficulty),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findBySkillsTested(skill: String): List<InterviewQuestionDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("questions.skills_tested", skill),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun searchByKeyword(keyword: String): List<InterviewQuestionDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.text(keyword),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findRandomQuestions(
        jobCategoryId: Long,
        count: Int,
    ): List<InterviewQuestionDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .aggregate(
                listOf(
                    match(Filters.eq("job_category_id", jobCategoryId)),
                    sample(count),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
