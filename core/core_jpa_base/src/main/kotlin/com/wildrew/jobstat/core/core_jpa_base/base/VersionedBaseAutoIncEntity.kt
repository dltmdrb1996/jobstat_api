package com.wildrew.jobstat.core.core_jpa_base.base

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version

@MappedSuperclass
abstract class VersionedBaseAutoIncEntity : BaseAutoIncEntity() {
    @Version
    var version: Long = 0L
        protected set
}
