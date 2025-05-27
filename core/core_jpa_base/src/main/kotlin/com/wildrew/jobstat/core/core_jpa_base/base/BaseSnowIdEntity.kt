package com.wildrew.jobstat.core.core_jpa_base.base

import com.wildrew.jobstat.core.core_jpa_base.id_generator.SnowflakeId
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.proxy.HibernateProxy
import java.util.*

@MappedSuperclass
abstract class BaseSnowIdEntity : BaseEntity {
    @Id
    @SnowflakeId
    override val id: Long = 0L

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
