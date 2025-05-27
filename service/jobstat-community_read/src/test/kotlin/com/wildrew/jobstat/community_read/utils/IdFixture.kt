package com.wildrew.jobstat.community_read.utils

import com.wildrew.jobstat.core.core_jpa_base.base.BaseEntity
import java.lang.reflect.Field

abstract class IdFixture<T : BaseEntity> : TestFixture<T> {
    fun setIdByReflection(
        entity: T,
        newId: Long,
    ) {
        // entity의 실제 클래스에서 시작하여 상위 클래스로 올라가며 'id' 필드를 찾음
        val idField = findFieldRecursive(entity::class.java, "id")

        if (idField != null) {
            try {
                idField.isAccessible = true // 접근 제한자 무시 설정
                idField.set(entity, newId) // entity 객체의 필드 값 변경
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
