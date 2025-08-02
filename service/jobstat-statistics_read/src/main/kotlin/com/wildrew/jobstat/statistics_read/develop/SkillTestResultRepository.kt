package com.wildrew.jobstat.statistics_read.develop

import com.mongodb.client.model.Accumulators.avg
import com.mongodb.client.model.Aggregates.group
import com.mongodb.client.model.Aggregates.match
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepository
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface SkillTestResultRepository : ReferenceMongoRepository<SkillTestResultDocument, String> {
    fun findBySkillId(skillId: Long): List<SkillTestResultDocument>

    fun findByTestType(testType: String): List<SkillTestResultDocument>

    fun findTopPerformers(
        skillId: Long,
        limit: Int,
    ): List<SkillTestResultDocument>

    fun getAverageScoreBySkill(skillId: Long): Double

    fun findByScoreRange(
        minScore: Double,
        maxScore: Double,
    ): List<SkillTestResultDocument>
}

@Repository
class SkillTestResultRepositoryImpl(
    private val entityInformation: MongoEntityInformation<SkillTestResultDocument, String>,
    private val mongoOperations: MongoOperations,
) : ReferenceMongoRepositoryImpl<SkillTestResultDocument, String>(entityInformation, mongoOperations),
    SkillTestResultRepository {
    init {
//        mongoOperations.indexOps(entityInformation.javaType).ensureIndex(
//            Index()
//                .on("skill_id", Sort.Direction.ASC)
//                .on("avg_score", Sort.Direction.DESC)
//                .background()
//        )
    }

    override fun findBySkillId(skillId: Long): List<SkillTestResultDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("skill_id", skillId),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByTestType(testType: String): List<SkillTestResultDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("test_type", testType),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopPerformers(
        skillId: Long,
        limit: Int,
    ): List<SkillTestResultDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("skill_id", skillId),
            ).sort(Sorts.descending("avg_score"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun getAverageScoreBySkill(skillId: Long): Double {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val result =
            collection
                .aggregate(
                    listOf(
                        match(Filters.eq("skill_id", skillId)),
                        group(null, avg("averageScore", "\$avg_score")),
                    ),
                ).first()

        return result?.getDouble("averageScore") ?: 0.0
    }

    override fun findByScoreRange(
        minScore: Double,
        maxScore: Double,
    ): List<SkillTestResultDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.gte("avg_score", minScore),
                    Filters.lte("avg_score", maxScore),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
