package com.example.jobstat.user.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*

interface ReadOnlyRole {
    val id: Long
    val name: String
    val users: Set<ReadUser>
}

@Entity
@Table(name = "roles")
internal class Role(
    override val id: Long = 0,
    @Column(nullable = false, unique = true)
    override val name: String,
) : BaseEntity(),
    ReadOnlyRole {
    @OneToMany(mappedBy = "role")
    private val _userRoles: MutableSet<UserRole> = mutableSetOf()

    override val users: Set<User>
        get() = _userRoles.map { it.user }.toSet()
}

internal enum class RoleData(
    val id: Long,
    private val roleName: String,
    val description: String,
) {
    USER(1L, "USER", "USER ROLE"),
    ADMIN(2L, "ADMIN", "ADMIN ROLE"),
    MANAGER(3L, "MANAGER", "MANAGER ROLE"),
    ;

    fun toEntity() = Role(id, roleName)
}
