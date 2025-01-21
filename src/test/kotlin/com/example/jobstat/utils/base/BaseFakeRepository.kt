package com.example.jobstat.utils.base

import com.example.jobstat.core.base.BaseEntity
import com.example.jobstat.utils.TestFixture
import jakarta.persistence.EntityNotFoundException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

abstract class BaseFakeRepository<T : BaseEntity, F : TestFixture<T>> {
    protected val store = ConcurrentHashMap<Long, T>()
    private val sequence = AtomicLong(0)

    // Fixture 생성 메서드
    protected abstract fun fixture(): F

    // 여러 엔티티 생성 및 저장을 위한 편의 메서드
    fun saveAll(count: Int, customizer: F.(Int) -> Unit = {}): List<T> {
        return (1..count).map { index ->
            fixture()
                .apply { customizer(index) }
                .create()
                .let { save(it) }
        }
    }

    open fun save(entity: T): T {
        val finalEntity = if (!isValidId(entity.id)) {
            createNewEntity(entity)
        } else {
            updateEntity(entity)
        }

        store[finalEntity.id] = finalEntity
        return finalEntity
    }

    open fun findById(id: Long): T {
        return findByIdOrNull(id) ?: throw EntityNotFoundException("Entity not found with id: $id")
    }

    open fun findByIdOrNull(id: Long): T? = store[id]

    open fun findAll(): List<T> = store.values.toList()

    open fun deleteById(id: Long) {
        store.remove(id)
    }

    open fun delete(entity: T) {
        store.remove(entity.id)
    }

    open fun deleteAll() {
        store.clear()
    }

    open fun existsById(id: Long): Boolean = store.containsKey(id)

    protected fun nextId(): Long = sequence.incrementAndGet()

    protected open fun isValidId(id: Long): Boolean = id > 0

    protected abstract fun createNewEntity(entity: T): T
    protected abstract fun updateEntity(entity: T): T

    fun clear() {
        store.clear()
        sequence.set(0)
        clearAdditionalState()
    }

    protected open fun clearAdditionalState() {}
}
