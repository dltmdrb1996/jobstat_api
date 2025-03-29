package com.example.jobstat.core.base

import jakarta.persistence.*
import org.hibernate.proxy.HibernateProxy
import java.util.Objects

@MappedSuperclass
abstract class BaseIdEntity {
    @Id
    open val id: Long = 0L

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        val otherId: Long? =
            when (other) {
                is BaseIdEntity -> other.id
                is HibernateProxy -> (other.hibernateLazyInitializer.identifier as? Long)
                else -> return false
            }

        return this.id == otherId
    }

    override fun hashCode() = Objects.hashCode(id)
}