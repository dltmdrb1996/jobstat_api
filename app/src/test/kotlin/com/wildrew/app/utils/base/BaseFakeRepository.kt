package com.wildrew.app.utils.base

import com.wildrew.app.utils.TestFixture
import com.wildrew.jobstat.core.core_jpa_base.base.BaseEntity
import jakarta.persistence.EntityNotFoundException
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

abstract class BaseFakeRepository<T : BaseEntity, F : TestFixture<T>> {
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
                createNewEntity(entity)
            } else {
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

    open fun isValidId(id: Long): Boolean = (id > 0)

    protected abstract fun createNewEntity(entity: T): T

    protected abstract fun updateEntity(entity: T): T

    open fun clear() {
        store.clear()
        sequence.set(0)
        clearAdditionalState()
    }

    protected open fun clearAdditionalState() {}

    fun setEntityId(
        entity: T,
        newId: Long,
    ) {
        setEntityField(entity, "id", newId)
    }

    fun setCreatedAt(
        entity: T,
        newCreatedAt: LocalDateTime,
    ) {
        setEntityField(entity, "createdAt", newCreatedAt)
    }

    private fun findFieldRecursive(
        clazz: Class<*>,
        fieldName: String,
    ): Field? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            try {
                return currentClass.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }

    fun <T : Any, V> setEntityField(
        entity: T,
        fieldName: String,
        newValue: V,
    ) {
        val field = findFieldRecursive(entity::class.java, fieldName)

        if (field != null) {
            try {
                field.isAccessible = true
                field.set(entity, newValue)
            } catch (e: IllegalAccessException) {
                System.err.println("Error setting field '$fieldName' via reflection (Illegal Access) for ${entity::class.java.name}: ${e.message}")
                throw RuntimeException("Failed to set field '$fieldName' for ${entity::class.java.name} due to access restrictions", e)
            } catch (e: IllegalArgumentException) {
                System.err.println("Error setting field '$fieldName' via reflection (Illegal Argument - type mismatch?) for ${entity::class.java.name}: ${e.message}")
                throw RuntimeException("Failed to set field '$fieldName' for ${entity::class.java.name} due to type mismatch or other argument issue", e)
            } catch (e: Exception) {
                System.err.println("Error setting field '$fieldName' via reflection for ${entity::class.java.name}: ${e.message}")
                throw RuntimeException("Failed to set field '$fieldName' for ${entity::class.java.name} via reflection", e)
            }
        } else {
            // 필드를 찾지 못한 경우 명시적인 예외 발생
            throw NoSuchFieldException("Field '$fieldName' not found in the class hierarchy of ${entity::class.java.name}")
        }
    }
}
