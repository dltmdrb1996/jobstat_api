package com.example.jobstat.user.entity

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
    val isActive: Boolean
    val roles: Set<ReadOnlyRole>

    fun hasRole(roleName: String): Boolean
    fun isAdmin(): Boolean
}

@Entity
@Table(name = "users")
internal class User(
    username: String,
    email: String,
    birthDate: LocalDate,
    override val id: Long = 0L,
) : SoftDeleteBaseEntity(), ReadUser {
    @Column(nullable = false, length = 10, unique = true)
    override var username: String = username
        protected set

    @Column(nullable = false, length = 100, unique = true)
    override var email: String = email
        protected set

    @Column(nullable = false)
    override var birthDate: LocalDate = birthDate
        protected set

    @Embedded
    override var address: Address? = null
        protected set

    @Column(nullable = false, name = "is_active")
    override var isActive: Boolean = true
        protected set

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _userRoles: MutableList<UserRole> = mutableListOf()

    // 기본적인 유저권한을
    override val roles: Set<Role>
        get() = _userRoles.map { it.role }.toSet()

    override fun hasRole(roleName: String): Boolean {
        return roles.any { it.name == roleName }
    }

    override fun isAdmin(): Boolean {
        return hasRole("ADMIN")
    }

    fun addRole(role: UserRole) {
        _userRoles.add(role)
    }

    fun removeRole(roleToRemove: Role) {
        _userRoles.removeIf { it.role.id == roleToRemove.id }
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

    companion object {
        fun create(
            username: String,
            email: String,
            birthDate: LocalDate,
        ): User {
            require(username.matches(RegexPatterns.USERNAME)) { "유효하지 않은 사용자 이름입니다." }
            require(email.matches(RegexPatterns.EMAIL)) { "유효하지 않은 이메일 주소입니다." }
            require(birthDate.isBefore(LocalDate.now().minusDays(1))) { "유효하지 않은 생년월일입니다." }
            return User(username, email, birthDate)
        }
    }
}
