package com.example.jobstat.auth.user.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "user_roles",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "role_id"]),
    ],
)
internal class UserRole(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    val role: Role,
) : BaseEntity()
