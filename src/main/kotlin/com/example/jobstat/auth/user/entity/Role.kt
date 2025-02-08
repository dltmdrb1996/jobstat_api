package com.example.jobstat.auth.user.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*

interface ReadRole {
    val id: Long
    val name: String
    val users: Set<ReadUser>
}

@Entity
@Table(
    name = "roles",
    indexes = [
        Index(name = "idx_roles_name", columnList = "name"),
    ],
)
internal class Role private constructor(
    name: String,
) : BaseEntity(),
    ReadRole {
    @Column(nullable = false, unique = true, length = 50)
    override var name: String = name
        protected set

    @OneToMany(
        mappedBy = "role",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    private val _userRoles: MutableSet<UserRole> = mutableSetOf()

    override val users: Set<User>
        get() = _userRoles.mapNotNull { it.user }.toSet()

    fun getUserRole(user: User): UserRole? = _userRoles.find { it.user.id == user.id }

    fun addUserRole(userRole: UserRole) {
        require(userRole.role == this) { "UserRole은 현재 역할에 속해있어야 합니다" }
        if (getUserRole(userRole.user) != null) return

        _userRoles.add(userRole)
    }

    fun removeUserRole(
        user: User,
        removeFromUser: Boolean = true,
    ) {
        val userRole = getUserRole(user)
        userRole?.let {
            _userRoles.remove(it)
            if (removeFromUser) {
                user.removeRole(this, false)
            }
        }
    }

    fun clearUserRoles() {
        val users = _userRoles.map { it.user }.toSet()
        users.forEach { user ->
            removeUserRole(user)
        }
    }

    companion object {
        fun create(
            name: String,
            id: Long = 0L,
        ): Role {
            require(name.isNotBlank()) { "역할 이름은 비어있을 수 없습니다" }
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
