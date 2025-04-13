package com.example.jobstat.core.base

import jakarta.persistence.*

@MappedSuperclass
abstract class VersionedBaseAutoIncEntity : BaseAutoIncEntity() {
    @Version
    var version: Long = 0L
        protected set
}
