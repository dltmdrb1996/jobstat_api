package com.wildrew.jobstat.statistics_read.utils

import com.wildrew.jobstat.statistics_read.common.BaseEntity
import com.wildrew.jobstat.statistics_read.utils.TestFixture
import java.lang.reflect.Field

abstract class IdFixture<T : BaseEntity> : TestFixture<T> {
    fun setIdByReflection(
        entity: T,
        newId: Long,
    ) {
        val idField = findFieldRecursive(entity::class.java, "id")

        if (idField != null) {
            try {
                idField.isAccessible = true
                idField.set(entity, newId)
            } catch (e: IllegalAccessException) {
                System.err.println("Error setting ID via reflection (Illegal Access) for ${entity::class.java.name}: ${e.message}")
                throw RuntimeException("Failed to set ID for ${entity::class.java.name} due to access restrictions", e)
            } catch (e: Exception) {
                System.err.println("Error setting ID via reflection for ${entity::class.java.name}: ${e.message}")
                throw RuntimeException("Failed to set ID for ${entity::class.java.name} via reflection", e)
            }
        } else {
            throw NoSuchFieldException("Field 'id' not found in the class hierarchy of ${entity::class.java.name}")
        }
    }

    fun findFieldRecursive(
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
}
