package com.example.jobstat.auth.user.entity

import com.example.jobstat.core.base.AuditableEntitySnow
import jakarta.persistence.*

@Entity
@Table(
    name = "user_roles",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_role",
            columnNames = ["user_id", "role_id"],
        ),
    ],
)
internal class UserRole private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    val role: Role,
) : AuditableEntitySnow() {
    companion object {
        fun create(
            user: User,
            role: Role,
        ): UserRole {
            val existingUserRole = user.getUserRole(role) ?: role.getUserRole(user)
            if (existingUserRole != null) {
                return existingUserRole
            }

            val userRole = UserRole(user, role)
            user.assignRole(userRole)
            role.assignRole(userRole)
            return userRole
        }
    }
}
