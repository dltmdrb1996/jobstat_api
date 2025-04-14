package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.BaseDocument
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model.*
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository
import org.springframework.data.repository.NoRepositoryBean
import java.time.Instant

@NoRepositoryBean
interface BaseMongoRepository<T : BaseDocument, ID : Any> : MongoRepository<T, ID> {
    /**
     * createdAt 인덱스를 활용한 범위 쿼리를 수행합니다
     * 최적의 성능을 위해 배치 사이즈를 적용합니다
     */
    fun findByCreatedAtBetween(
        start: Instant,
        end: Instant,
    ): List<T>

    fun findAllByQuery(query: Query): List<T>

    /**
     * _id 인덱스를 활용하여 여러 문서를 효율적으로 조회합니다
     */
    fun bulkFindByIds(ids: List<ID>): List<T>

    // Insert Operations
    fun bulkInsert(entities: List<T>): List<T>

    // Update Operations
    fun bulkUpdate(entities: List<T>): List<T>

    // Delete Operations
    fun bulkDelete(ids: List<ID>): Int

    // Upsert Operations
    fun bulkUpsert(entities: List<T>): BulkWriteResult
}

abstract class BaseMongoRepositoryImpl<T : BaseDocument, ID : Any>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : SimpleMongoRepository<T, ID>(entityInformation, mongoOperations),
    com.example.jobstat.core.base.repository.BaseMongoRepository<T, ID> {
    companion object {
        const val OPTIMAL_BATCH_SIZE = 2000
        private val UNORDERED_BULK_OPTIONS =
            BulkWriteOptions()
                .ordered(false)
                .bypassDocumentValidation(true)
    }

    /**
     * createdAt 인덱스를 활용한 범위 쿼리를 수행합니다
     * 최적의 성능을 위해 배치 사이즈를 적용합니다
     */
    override fun findByCreatedAtBetween(
        start: Instant,
        end: Instant,
    ): List<T> {
        // createdAt 인덱스를 활용한 범위 쿼리 (이미 @Indexed)
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.gte("createdAt", start),
                    Filters.lte("createdAt", end),
                ),
            ).hintString("createdAt") // createdAt 필드에 대해 {createdAt:1} 인덱스가 있다고 가정
            .batchSize(com.example.jobstat.core.base.repository.BaseMongoRepositoryImpl.Companion.OPTIMAL_BATCH_SIZE)
            // noCursorTimeout는 특별한 이유가 없으면 제거하여 리소스 점유 줄임
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * _id 인덱스를 활용하여 여러 문서를 효율적으로 조회합니다
     */
    override fun bulkFindByIds(ids: List<ID>): List<T> {
        if (ids.isEmpty()) return emptyList()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)
        val objectIds = ids.map { ObjectId(it.toString()) }

        // _id 인덱스를 활용한 정렬
        return collection
            .find(Filters.`in`("_id", objectIds))
            .sort(Sorts.ascending("_id"))
            .hintString("_id_") // _id 인덱스 사용 명시
            .batchSize(com.example.jobstat.core.base.repository.BaseMongoRepositoryImpl.Companion.OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 정렬이 요청된 경우 정렬을 적용하고
     * 그렇지 않은 경우 _id 기준으로 정렬하여 인덱스 스캔을 최적화합니다
     */
    override fun findAllByQuery(query: Query): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)
        val filter = queryToFilter(query)

        val findIterable =
            collection
                .find(filter)
                .batchSize(com.example.jobstat.core.base.repository.BaseMongoRepositoryImpl.Companion.OPTIMAL_BATCH_SIZE)

        // 정렬이 요청된 경우 정렬 적용
        val sortBson = queryToSort(query)
        if (sortBson != null) {
            findIterable.sort(sortBson)
            // 정렬 필드에 인덱스가 존재한다면 hint 지정 가능 (예: createdAt_1 등)
            // 예: findIterable.hintString("createdAt_1")
        } else {
            // 정렬 요청이 없을 경우 _id 기준으로 정렬하여 인덱스 스캔 최적화
            findIterable.sort(Sorts.ascending("_id"))
            findIterable.hintString("_id_")
        }

        return findIterable.map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }.toList()
    }

    fun findAllFast(): List<T> {
        // _id 인덱스를 활용한 순차 스캔
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.ascending("_id"))
            .hintString("_id_")
            .batchSize(com.example.jobstat.core.base.repository.BaseMongoRepositoryImpl.Companion.OPTIMAL_BATCH_SIZE)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun bulkInsert(entities: List<T>): List<T> {
        if (entities.isEmpty()) return emptyList()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)
        val converter = mongoOperations.converter

        val documents =
            entities.map { entity ->
                if (entity.id == null) {
                    entity.id = ObjectId().toString()
                }
                converter.convertToMongoType(entity) as Document
            }

        collection.insertMany(
            documents,
            InsertManyOptions()
                .ordered(false)
                .bypassDocumentValidation(true),
        )

        return entities
    }

    override fun bulkUpdate(entities: List<T>): List<T> {
        if (entities.isEmpty()) return emptyList()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)
        val converter = mongoOperations.converter

        val writes =
            entities.map { entity ->
                val id = entity.id ?: throw IllegalArgumentException("Entity ID cannot be null for update")
                val doc = converter.convertToMongoType(entity) as Document
                ReplaceOneModel<Document>(
                    Filters.eq("_id", ObjectId(id)),
                    doc,
                    ReplaceOptions().upsert(false).bypassDocumentValidation(true),
                )
            }

        collection.bulkWrite(
            writes,
            com.example.jobstat.core.base.repository.BaseMongoRepositoryImpl.Companion.UNORDERED_BULK_OPTIONS,
        )
        return entities
    }

    @Suppress("UNCHECKED_CAST")
    override fun bulkUpsert(entities: List<T>): BulkWriteResult {
        if (entities.isEmpty()) return BulkWriteResult.unacknowledged()

        val collection = mongoOperations.getCollection(entityInformation.collectionName)
        val converter = mongoOperations.converter

        val writes =
            entities.map { entity ->
                val id = entityInformation.getId(entity) as String
                val doc = Document(converter.convertToMongoType(entity) as Map<String, *>)
                UpdateOneModel<Document>(
                    Filters.eq("_id", ObjectId(id)),
                    Document("\$set", doc),
                    UpdateOptions().upsert(true).bypassDocumentValidation(true),
                )
            }

        return collection.bulkWrite(
            writes,
            com.example.jobstat.core.base.repository.BaseMongoRepositoryImpl.Companion.UNORDERED_BULK_OPTIONS,
        )
    }

    override fun bulkDelete(ids: List<ID>): Int {
        if (ids.isEmpty()) return 0

        val collection = mongoOperations.getCollection(entityInformation.collectionName)
        val objectIds = ids.map { ObjectId(it.toString()) }

        return collection
            .deleteMany(
                Filters.`in`("_id", objectIds),
                DeleteOptions().hint(Document("_id", 1)),
            ).deletedCount
            .toInt()
    }

    private fun queryToFilter(query: Query): Bson {
        if (query.queryObject.isEmpty()) {
            return Filters.empty()
        }

        val filters = mutableListOf<Bson>()

        query.queryObject.forEach { (key, value) ->
            when {
                key == "_id" -> {
                    when (value) {
                        is Document -> {
                            value.forEach { (operator, operand) ->
                                when (operator) {
                                    "\$in" ->
                                        filters.add(
                                            Filters.`in`(
                                                "_id",
                                                (operand as List<*>).map { ObjectId(it.toString()) },
                                            ),
                                        )

                                    "\$gt" -> filters.add(Filters.gt("_id", ObjectId(operand.toString())))
                                    "\$gte" -> filters.add(Filters.gte("_id", ObjectId(operand.toString())))
                                    "\$lt" -> filters.add(Filters.lt("_id", ObjectId(operand.toString())))
                                    "\$lte" -> filters.add(Filters.lte("_id", ObjectId(operand.toString())))
                                    "\$ne" -> filters.add(Filters.ne("_id", ObjectId(operand.toString())))
                                }
                            }
                        }

                        else -> filters.add(Filters.eq("_id", ObjectId(value.toString())))
                    }
                }

                key.endsWith("At") && value is Document -> {
                    value.forEach { (operator, operand) ->
                        when (operator) {
                            "\$gt" -> filters.add(Filters.gt(key, operand))
                            "\$gte" -> filters.add(Filters.gte(key, operand))
                            "\$lt" -> filters.add(Filters.lt(key, operand))
                            "\$lte" -> filters.add(Filters.lte(key, operand))
                            "\$in" -> filters.add(Filters.`in`(key, operand as List<*>))
                            "\$nin" -> filters.add(Filters.nin(key, operand as List<*>))
                            "\$ne" -> filters.add(Filters.ne(key, operand))
                        }
                    }
                }

                value is Document -> {
                    value.forEach { (operator, operand) ->
                        when (operator) {
                            "\$regex" -> {
                                val options = value["\$options"] as? String
                                filters.add(
                                    if (options != null) {
                                        Filters.regex(key, operand.toString(), options)
                                    } else {
                                        Filters.regex(key, operand.toString())
                                    },
                                )
                            }

                            "\$in" -> filters.add(Filters.`in`(key, operand as List<*>))
                            "\$nin" -> filters.add(Filters.nin(key, operand as List<*>))
                            "\$gt" -> filters.add(Filters.gt(key, operand))
                            "\$gte" -> filters.add(Filters.gte(key, operand))
                            "\$lt" -> filters.add(Filters.lt(key, operand))
                            "\$lte" -> filters.add(Filters.lte(key, operand))
                            "\$ne" -> filters.add(Filters.ne(key, operand))
                            "\$exists" -> filters.add(Filters.exists(key, operand as Boolean))
                            "\$type" -> filters.add(Filters.type(key, operand.toString()))
                            "\$all" -> filters.add(Filters.all(key, operand as List<*>))
                            "\$size" -> filters.add(Filters.size(key, operand as Int))
                            "\$elemMatch" -> {
                                val elemMatchDoc = operand as Document
                                val elemMatchFilters =
                                    elemMatchDoc.map { (elemKey, elemValue) ->
                                        when (elemKey) {
                                            "\$gt" -> Filters.gt(key, elemValue)
                                            "\$gte" -> Filters.gte(key, elemValue)
                                            "\$lt" -> Filters.lt(key, elemValue)
                                            "\$lte" -> Filters.lte(key, elemValue)
                                            "\$eq" -> Filters.eq(key, elemValue)
                                            "\$ne" -> Filters.ne(key, elemValue)
                                            else -> Filters.eq("$key.$elemKey", elemValue)
                                        }
                                    }
                                filters.add(Filters.elemMatch(key, Filters.and(elemMatchFilters)))
                            }
                        }
                    }
                }

                key.contains(".") -> {
                    // 필드 경로 쿼리에 대해서 인덱스가 있다면 활용 가능. 인덱스 패턴에 맞춰서 필드명 지정 필요.
                    filters.add(Filters.eq(key, value))
                }

                else -> filters.add(Filters.eq(key, value))
            }
        }

        return if (filters.size == 1) filters.first() else Filters.and(filters)
    }

    private fun queryToSort(query: Query): Bson? =
        if (query.sortObject.isEmpty()) {
            null
        } else {
            val sortFields =
                query.sortObject.entries.map { (field, direction) ->
                    if (direction == 1) {
                        Sorts.ascending(field)
                    } else {
                        Sorts.descending(field)
                    }
                }
            Sorts.orderBy(sortFields)
        }
}
