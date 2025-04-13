package com.example.jobstat.core.base

import jakarta.persistence.*
import org.hibernate.proxy.HibernateProxy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.Objects

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseAutoIncEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        val otherId: Long? =
            when (other) {
                is BaseAutoIncEntity -> other.id
                is HibernateProxy -> (other.hibernateLazyInitializer.identifier as? Long)
                else -> return false
            }

        return this.id == otherId
    }

    override fun hashCode() = Objects.hashCode(id)
}
