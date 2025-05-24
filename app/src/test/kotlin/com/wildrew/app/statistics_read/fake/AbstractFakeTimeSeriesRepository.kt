package com.wildrew.app.statistics_read.fake

import com.mongodb.bulk.BulkWriteResult
import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.app.statistics_read.core.core_mongo_base.model.BaseTimeSeriesDocument
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.BaseTimeSeriesRepository
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import java.util.*

abstract class AbstractFakeTimeSeriesRepository<T : BaseTimeSeriesDocument>(
    protected val pageSize: Int = 100,
) : BaseTimeSeriesRepository<T, String> {
    protected val documents = mutableMapOf<String, T>()

    // BaseTimeSeriesRepository 구현
    override fun findByBaseDate(baseDate: BaseDate): T? = documents[baseDate.toString()]

    override fun findByBaseDateBetween(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<T> =
        documents.values
            .filter { doc ->
                val date = doc.baseDate
                date >= startDate.toString() && date <= endDate.toString()
            }.sortedBy { it.baseDate }

    override fun findLatest(): T? = documents.values.maxByOrNull { it.baseDate }

    override fun findLatestN(n: Int): List<T> =
        documents.values
            .sortedByDescending { it.baseDate }
            .take(n)

    // BaseMongoRepository 구현
    override fun findByCreatedAtBetween(
        start: Instant,
        end: Instant,
    ): List<T> =
        documents.values
            .filter { doc ->
                val createdAt = doc.createdAt
                createdAt != null && createdAt >= start && createdAt <= end
            }.sortedBy { it.createdAt }

    override fun findAllByQuery(query: Query): List<T> = documents.values.toList()

    override fun bulkFindByIds(ids: List<String>): List<T> =
        documents.values
            .filter { doc -> doc.id in ids }
            .sortedBy { it.id }

    override fun bulkInsert(entities: List<T>): List<T> {
        entities.forEach { doc ->
            if (doc.id == null) {
                doc.id = ObjectId().toString()
            }
            documents[doc.baseDate] = doc
        }
        return entities
    }

    override fun bulkUpdate(entities: List<T>): List<T> {
        entities.forEach { doc ->
            doc.id?.let { documents[doc.baseDate] = doc }
        }
        return entities
    }

    override fun bulkDelete(ids: List<String>): Int {
        val count = documents.values.count { it.id in ids }
        documents.values.removeAll { it.id in ids }
        return count
    }

    override fun bulkUpsert(entities: List<T>): BulkWriteResult {
        entities.forEach { doc ->
            documents[doc.baseDate] = doc
        }

        return BulkWriteResult.acknowledged(
            0,
            entities.size,
            0,
            entities.size,
            emptyList(),
            emptyList(),
        )
    }

    // MongoRepository의 기본 메서드들
    override fun <S : T> save(entity: S): S {
        documents[entity.baseDate] = entity
        return entity
    }

    override fun <S : T> saveAll(entities: Iterable<S>): List<S> {
        entities.forEach { save(it) }
        return entities.toList()
    }

    override fun findById(id: String): Optional<T> = Optional.ofNullable(documents.values.find { it.id == id })

    override fun existsById(id: String): Boolean = documents.values.any { it.id == id }

    override fun findAll(): List<T> = documents.values.toList()

    override fun findAllById(ids: Iterable<String>): List<T> = documents.values.filter { it.id in ids }

    override fun count(): Long = documents.size.toLong()

    override fun deleteById(id: String) {
        documents.values.removeAll { it.id == id }
    }

    override fun delete(entity: T) {
        documents.remove(entity.baseDate)
    }

    override fun deleteAllById(ids: Iterable<String>) {
        documents.values.removeAll { it.id in ids }
    }

    override fun deleteAll(entities: Iterable<T>) {
        entities.forEach { delete(it) }
    }

    override fun deleteAll() {
        documents.clear()
    }

    // Helper methods for testing
    fun addDocument(document: T) {
        documents[document.baseDate] = document
    }

    fun clear() {
        documents.clear()
    }
}
