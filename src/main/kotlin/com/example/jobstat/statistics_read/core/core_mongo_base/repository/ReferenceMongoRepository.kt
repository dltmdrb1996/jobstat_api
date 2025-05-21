package com.example.jobstat.statistics_read.core.core_mongo_base.repository

import com.example.jobstat.statistics_read.core.core_mongo_base.model.BaseReferenceDocument
import com.example.jobstat.statistics_read.core.core_model.DocumentStatus
import com.example.jobstat.statistics_read.core.core_model.EntityType
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

    /**
     * 특정 reference ID에 해당하는 문서를 조회합니다.
     *
     * @param referenceId 조회할 reference ID
     * @return 해당 reference ID의 문서, 없으면 null
     */
    override fun findByReferenceId(referenceId: Long): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("reference_id", referenceId))
            .limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    /**
     * 여러 reference ID들에 해당하는 문서들을 조회합니다.
     * referenceIds가 비어있는 경우 빈 리스트를 반환합니다.
     *
     * @param referenceIds 조회할 reference ID 목록
     * @return reference ID들에 해당하는 문서 리스트
     */
    override fun findByReferenceIds(referenceIds: List<Long>): List<T> {
        if (referenceIds.isEmpty()) return emptyList()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.`in`("reference_id", referenceIds))
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티 타입에 해당하는 모든 문서를 조회합니다.
     *
     * @param entityType 조회할 엔티티 타입
     * @return 해당 엔티티 타입의 문서 리스트
     */
    override fun findByEntityType(entityType: EntityType): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("entity_type", entityType))
            .batchSize(OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티 타입과 상태를 가진 모든 문서를 조회합니다.
     *
     * @param entityType 조회할 엔티티 타입
     * @param status 조회할 문서 상태
     * @return 해당 조건을 만족하는 문서 리스트
     */
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

    /**
     * 특정 reference ID와 엔티티 타입을 가진 문서를 조회합니다.
     *
     * @param referenceId 조회할 reference ID
     * @param entityType 조회할 엔티티 타입
     * @return 해당 조건을 만족하는 문서, 없으면 null
     */
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

    /**
     * 특정 reference ID를 가진 문서의 상태를 업데이트합니다.
     * updatedAt 필드도 함께 현재 시간으로 업데이트됩니다.
     *
     * @param referenceId 업데이트할 문서의 reference ID
     * @param status 새로운 상태
     * @return 업데이트된 문서, 실패 시 null
     */
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
