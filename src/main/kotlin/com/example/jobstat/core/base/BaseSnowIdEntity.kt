package com.example.jobstat.core.base

import com.example.jobstat.core.global.utils.id_generator.SnowflakeId
import jakarta.persistence.*
import org.hibernate.proxy.HibernateProxy
import java.util.Objects

@MappedSuperclass
abstract class BaseSnowIdEntity {
    @Id
    @SnowflakeId
    @Column(name = "id", nullable = false, updatable = false)
    val id: Long = 0L

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass && other !is HibernateProxy) return false
        val currentId = this.id ?: return false
        val otherId =
            when (other) {
                is BaseSnowIdEntity -> other.id
                is HibernateProxy -> (other.hibernateLazyInitializer.identifier as? Long)
                else -> return false
            } ?: return false
        return currentId == otherId
    }

    override fun hashCode(): Int = Objects.hashCode(id)
}
