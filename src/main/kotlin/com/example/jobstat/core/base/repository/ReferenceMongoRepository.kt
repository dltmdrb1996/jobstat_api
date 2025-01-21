package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.BaseReferenceDocument
import com.example.jobstat.core.state.DocumentStatus
import com.example.jobstat.core.state.EntityType
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import java.time.Instant

@NoRepositoryBean
interface ReferenceMongoRepository<T : BaseReferenceDocument, ID : Any> : BaseMongoRepository<T, ID> {
    fun findByReferenceId(referenceId: Long): T?

    fun findByReferenceIds(referenceIds: List<Long>): List<T>

    fun findByEntityType(entityType: EntityType): List<T>

    fun findByEntityTypeAndStatus(
        entityType: EntityType,
        status: DocumentStatus,
    ): List<T>

    fun findByReferenceIdAndEntityType(
        referenceId: Long,
        entityType: EntityType,
    ): T?

    fun updateStatus(
        referenceId: Long,
        status: DocumentStatus,
    ): T?
}

abstract class ReferenceMongoRepositoryImpl<T : BaseReferenceDocument, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseMongoRepositoryImpl<T, ID>(entityInformation, mongoOperations),
    ReferenceMongoRepository<T, ID> {
    init {
        // Create compound index for reference lookups
//        mongoOperations.indexOps(entityInformation.javaType).ensureIndex(
//            Index()
//                .on("reference_id", Sort.Direction.ASC)
//                .on("entity_type", Sort.Direction.ASC)
//                .background()
//        )
    }

    override fun findByReferenceId(referenceId: Long): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("reference_id", referenceId))
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByReferenceIds(referenceIds: List<Long>): List<T> {
        if (referenceIds.isEmpty()) return emptyList()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.`in`("reference_id", referenceIds))
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByEntityType(entityType: EntityType): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("entity_type", entityType))
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByEntityTypeAndStatus(
        entityType: EntityType,
        status: DocumentStatus,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("entity_type", entityType),
                    Filters.eq("status", status),
                ),
            ).batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByReferenceIdAndEntityType(
        referenceId: Long,
        entityType: EntityType,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("reference_id", referenceId),
                    Filters.eq("entity_type", entityType),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun updateStatus(
        referenceId: Long,
        status: DocumentStatus,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val updateResult =
            collection.findOneAndUpdate(
                Filters.eq("reference_id", referenceId),
                Updates.combine(
                    Updates.set("status", status),
                    Updates.set("updatedAt", Instant.now()),
                ),
                FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER),
            )

        return updateResult?.let { doc ->
            mongoOperations.converter.read(entityInformation.javaType, doc)
        }
    }
}
