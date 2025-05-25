package com.wildrew.app.auth.user.entity

import com.wildrew.app.auth.user.UserConstants
import com.wildrew.jobstat.core.core_jpa_base.base.AuditableEntitySnow
import jakarta.persistence.*

@Entity
@Table(
    name = "roles",
    indexes = [
        Index(name = "idx_roles_name", columnList = "name"),
    ],
)
class Role private constructor(
    name: String,
) : AuditableEntitySnow() {
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
        get() = _userRoles.map { it.user }.toSet()

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
        ): Role {
            require(name.isNotBlank()) { UserConstants.ErrorMessages.INVALID_ROLE }
            return Role(name)
        }
    }
}