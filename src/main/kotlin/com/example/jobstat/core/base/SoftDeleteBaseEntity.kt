package com.example.jobstat.core.base

import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
abstract class SoftDeleteBaseEntity : BaseEntity() {
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
