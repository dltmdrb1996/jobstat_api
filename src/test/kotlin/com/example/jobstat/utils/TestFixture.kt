package com.example.jobstat.utils

import com.example.jobstat.core.base.BaseAutoIncEntity

interface TestFixture<T> {
    fun create(): T
}

abstract class IdFixture<T> : TestFixture<T> {
    fun setIdByReflection(
        entity: T,
        newId: Long,
    ) {
        // 'Role'이 아니라 'BaseEntity' 에서 id 필드를 찾는다
        val idField = BaseAutoIncEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, newId)
    }
}
