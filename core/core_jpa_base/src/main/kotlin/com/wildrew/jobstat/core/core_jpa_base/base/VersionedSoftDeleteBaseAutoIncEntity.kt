package com.wildrew.jobstat.core.core_jpa_base.base

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime

@MappedSuperclass
abstract class VersionedSoftDeleteBaseAutoIncEntity : VersionedBaseAutoIncEntity() {
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
