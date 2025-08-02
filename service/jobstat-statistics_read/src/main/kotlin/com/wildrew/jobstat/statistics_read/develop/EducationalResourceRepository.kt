package com.wildrew.jobstat.statistics_read.develop

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepository
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface EducationalResourceRepository : ReferenceMongoRepository<EducationalResourceDocument, String> {
    fun findBySkillCategories(categories: List<String>): List<EducationalResourceDocument>

    fun findByDifficultyLevel(level: String): List<EducationalResourceDocument>

    fun findByResourceType(type: String): List<EducationalResourceDocument>

    fun searchByKeyword(keyword: String): List<EducationalResourceDocument>

    fun findLatestResources(limit: Int): List<EducationalResourceDocument>
}

@Repository
class EducationalResourceRepositoryImpl(
    private val entityInformation: MongoEntityInformation<EducationalResourceDocument, String>,
    private val mongoOperations: MongoOperations,
) : ReferenceMongoRepositoryImpl<EducationalResourceDocument, String>(entityInformation, mongoOperations),
    EducationalResourceRepository {
    init {
        // 검색을 위한 텍스트 인덱스 생성
//        mongoOperations.indexOps(entityInformation.javaType).ensureIndex(
//        )
    }

    override fun findBySkillCategories(categories: List<String>): List<EducationalResourceDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.`in`("skill_categories", categories),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByDifficultyLevel(level: String): List<EducationalResourceDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("difficulty_level", level),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByResourceType(type: String): List<EducationalResourceDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("resource_type", type),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun searchByKeyword(keyword: String): List<EducationalResourceDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.text(keyword),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findLatestResources(limit: Int): List<EducationalResourceDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("updatedAt"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
