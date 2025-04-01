package com.example.jobstat.auth.user.entity

import com.example.jobstat.auth.user.UserConstants
import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "roles",
    indexes = [
        Index(name = "idx_roles_name", columnList = "name"),
    ],
)
internal class Role private constructor(
    name: String,
) : BaseEntity() {
    @Column(nullable = false, unique = true, length = UserConstants.MAX_ROLE_NAME_LENGTH)
    var name: String = name
        protected set

    @OneToMany(
        mappedBy = "role",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    private val _userRoles: MutableSet<UserRole> = mutableSetOf()

    val users: Set<User>
        get() = _userRoles.mapNotNull { it.user }.toSet()

    fun getUserRole(user: User): UserRole? = _userRoles.find { it.user.id == user.id }

    fun assignRole(userRole: UserRole) {
        require(userRole.role == this) { UserConstants.ErrorMessages.INVALID_ROLE }
        if (getUserRole(userRole.user) != null) return
        _userRoles.add(userRole)
    }

    fun revokeRole(
        user: User,
        removeFromUser: Boolean = true,
    ) {
        val userRole = getUserRole(user)
        userRole?.let {
            _userRoles.remove(it)
            if (removeFromUser) {
                user.revokeRole(this, false)
            }
        }
    }

    companion object {
        fun create(
            name: String,
            id: Long = 0L,
        ): Role {
            require(name.isNotBlank()) { UserConstants.ErrorMessages.INVALID_ROLE }
            return Role(name)
        }
    }
}

internal enum class RoleData(
    val id: Long,
    private val roleName: String,
    val description: String,
) {
    USER(1L, "USER", "일반 사용자 역할"),
    ADMIN(2L, "ADMIN", "관리자 역할"),
    MANAGER(3L, "MANAGER", "매니저 역할"),
    ;

    fun toEntity() = Role.create(roleName)
}
