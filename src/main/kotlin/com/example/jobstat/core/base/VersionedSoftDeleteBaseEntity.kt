package com.example.jobstat.core.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime

@MappedSuperclass
abstract class VersionedSoftDeleteBaseEntity : VersionedBaseEntity() {
    @Column(nullable = false)
    var isDeleted: Boolean = false
        protected set

    @Column
    var deletedAt: LocalDateTime? = null
        protected set

    fun delete() {
        isDeleted = true
        deletedAt = LocalDateTime.now()
    }
}