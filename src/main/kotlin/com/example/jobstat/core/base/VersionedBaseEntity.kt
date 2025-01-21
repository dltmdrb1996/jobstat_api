package com.example.jobstat.core.base

import jakarta.persistence.*

@MappedSuperclass
abstract class VersionedBaseEntity : BaseEntity() {
    @Version
    var version: Long = 0L
        protected set
}