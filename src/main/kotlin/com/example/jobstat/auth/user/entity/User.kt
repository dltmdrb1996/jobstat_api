package com.example.jobstat.auth.user.entity

import com.example.jobstat.core.base.Address
import com.example.jobstat.core.base.SoftDeleteBaseEntity
import com.example.jobstat.core.utils.RegexPatterns
import jakarta.persistence.*
import java.time.LocalDate

interface ReadUser {
    val id: Long
    val username: String
    val email: String
    val birthDate: LocalDate
    val address: Address?
    val password: String
    val isActive: Boolean
    val roles: Set<ReadOnlyRole>

    fun getRolesString(): List<String> = roles.map { it.name }

    fun hasRole(roleName: String): Boolean

    fun isAdmin(): Boolean
}

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email"),
    ],
)
internal class User private constructor(
    username: String,
    email: String,
    password: String,
    birthDate: LocalDate,
) : SoftDeleteBaseEntity(),
    ReadUser {
    @Column(name = "username", nullable = false, unique = true, length = 15)
    override var username: String = username
        protected set

    @Column(name = "birth_date", nullable = false)
    override var birthDate: LocalDate = birthDate
        protected set

    @Column(name = "email", nullable = false, unique = true)
    override var email: String = email
        protected set

    @Column(name = "password", nullable = false)
    override var password: String = password
        protected set

    @Embedded
    override var address: Address? = null
        protected set

    @Column(name = "is_active", nullable = false)
    override var isActive: Boolean = true
        protected set

    @OneToMany(
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    private val _userRoles: MutableSet<UserRole> = mutableSetOf()

    override val roles: Set<Role>
        get() = _userRoles.mapNotNull { it.role }.toSet()

    override fun hasRole(roleName: String): Boolean = roles.any { it.name.equals(roleName, ignoreCase = true) }

    override fun isAdmin(): Boolean = hasRole("ADMIN")

    fun hasRole(role: Role): Boolean = _userRoles.any { it.role.id == role.id }

    fun getUserRole(role: Role): UserRole? = _userRoles.find { it.role.id == role.id }

    fun addRole(userRole: UserRole) {
        require(userRole.user == this) { "UserRole은 현재 사용자에 속해있어야 합니다" }
        if (hasRole(userRole.role)) return

        _userRoles.add(userRole)
    }

    fun removeRole(
        role: Role,
        removeFromRole: Boolean = true,
    ) {
        val userRole = getUserRole(role)
        userRole?.let {
            _userRoles.remove(it)
            if (removeFromRole) {
                role.removeUserRole(this, false)
            }
        }
    }

    fun clearRoles() {
        val currentRoles = roles.toSet() // 복사본 생성
        currentRoles.forEach { role ->
            removeRole(role)
        }
    }

    fun updatePassword(newPassword: String) {
        require(newPassword.isNotBlank()) { "패스워드는 필수 값입니다." }
        this.password = newPassword
    }

    fun updateEmail(newEmail: String) {
        require(newEmail.matches(RegexPatterns.EMAIL)) { "유효하지 않은 이메일 주소입니다." }
        this.email = newEmail
    }

    fun updateAddress(newAddress: Address?) {
        this.address = newAddress
    }

    fun activate() {
        isActive = true
    }

    fun deactivate() {
        isActive = false
    }

    override fun restore() {
        super.restore()
        activate()
    }

    companion object {
        fun create(
            username: String,
            email: String,
            password: String,
            birthDate: LocalDate,
        ): User {
            require(username.matches(RegexPatterns.USERNAME)) { "유효하지 않은 사용자 이름입니다" }
            require(email.matches(RegexPatterns.EMAIL)) { "유효하지 않은 이메일 주소입니다" }
            require(birthDate.isBefore(LocalDate.now().minusDays(1))) { "유효하지 않은 생년월일입니다" }
            require(password.isNotBlank()) { "패스워드는 필수 값입니다" }

            return User(username, email, password, birthDate)
        }
    }
}
