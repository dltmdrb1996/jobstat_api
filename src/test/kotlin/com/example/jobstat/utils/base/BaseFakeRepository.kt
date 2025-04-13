package com.example.jobstat.utils.base

import com.example.jobstat.core.base.BaseAutoIncEntity
import com.example.jobstat.utils.TestFixture
import jakarta.persistence.EntityNotFoundException
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * FakeRepository의 기본 뼈대 클래스.
 * - ID 시퀀스(sequence) 관리
 * - store(메모리 맵)에 엔티티 보관
 */
abstract class BaseFakeRepository<T : BaseAutoIncEntity, F : TestFixture<T>> {
    protected val store = ConcurrentHashMap<Long, T>()
    private val sequence = AtomicLong(0)

    protected abstract fun fixture(): F

    fun saveAll(
        count: Int,
        customizer: F.(Int) -> Unit = {},
    ): List<T> =
        (1..count).map { index ->
            fixture().apply { customizer(index) }.create().let { save(it) }
        }

    open fun save(entity: T): T {
        val finalEntity =
            if (!isValidId(entity.id)) {
                // 신규 엔티티 처리: ID를 할당
                createNewEntity(entity)
            } else {
                // 업데이트 로직: 원본 객체를 그대로 반영
                updateEntity(entity)
            }
        store[finalEntity.id] = finalEntity
        return finalEntity
    }

    open fun findById(id: Long): T = findByIdOrNull(id) ?: throw EntityNotFoundException("Entity not found with id: $id")

    open fun findByIdOrNull(id: Long): T? = store[id]

    open fun deleteAll() {
        store.clear()
        sequence.set(0)
        clearAdditionalState()
    }

    open fun findAll(): List<T> = store.values.toList()

    open fun deleteById(id: Long) {
        store.remove(id)
    }

    open fun delete(entity: T) {
        store.remove(entity.id)
    }

    open fun existsById(id: Long): Boolean = store.containsKey(id)

    fun nextId(): Long = sequence.incrementAndGet()

    protected open fun isValidId(id: Long): Boolean = (id > 0)

    protected abstract fun createNewEntity(entity: T): T

    protected abstract fun updateEntity(entity: T): T

    open fun clear() {
        store.clear()
        sequence.set(0)
        clearAdditionalState()
    }

    protected open fun clearAdditionalState() {}

    protected fun setEntityId(
        entity: T,
        newId: Long,
    ) {
        try {
            val idField: Field = BaseAutoIncEntity::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(entity, newId)
        } catch (e: Exception) {
            throw RuntimeException("Failed to set ID via reflection: ${e.message}", e)
        }
    }
}
