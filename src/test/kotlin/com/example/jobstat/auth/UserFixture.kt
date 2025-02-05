package com.example.jobstat.auth

import com.example.jobstat.core.base.Address
import com.example.jobstat.auth.user.entity.Role
import com.example.jobstat.auth.user.entity.RoleData
import com.example.jobstat.auth.user.entity.User
import com.example.jobstat.auth.user.entity.UserRole
import com.example.jobstat.utils.TestFixture
import java.time.LocalDate
import java.time.LocalDateTime

internal class UserFixture private constructor(
    private var id: Long = 0L,
    // 한글+영문+숫자 3-10자 규칙을 만족하는 기본값
    private var username: String = "테스트123",
    private var email: String = "test@example.com",
    private var birthDate: LocalDate = LocalDate.now().minusYears(20),
    private var address: Address? = null,
    private var isActive: Boolean = true,
    private var roles: Set<Role> = emptySet(),
    private var createdAt: LocalDateTime = LocalDateTime.now(),
    private var updatedAt: LocalDateTime = LocalDateTime.now(),
) : TestFixture<User> {
    override fun create(): User =
        User(
            username = username,
            email = email,
            birthDate = birthDate,
            id = id,
        ).apply {
            address?.let { updateAddress(it) }
            if (!isActive) deactivate()
            roles.forEach { role ->
                addRole(UserRole(user = this, role = role))
            }
        }

    fun withId(id: Long) = apply { this.id = id }

    fun withUsername(username: String) = apply { this.username = username }

    fun withEmail(email: String) = apply { this.email = email }

    fun withBirthDate(birthDate: LocalDate) = apply { this.birthDate = birthDate }

    fun withAddress(address: Address?) = apply { this.address = address }

    fun withActive(isActive: Boolean) = apply { this.isActive = isActive }

    fun withRoles(roles: Set<Role>) = apply { this.roles = roles }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

    companion object {
        fun aUser() = UserFixture()

        fun aDefaultUser() = aUser().create()

        fun anActiveUser() =
            aUser()
                .withUsername("활성123")
                .withActive(true)
                .create()

        fun anInactiveUser() =
            aUser()
                .withUsername("비활성123")
                .withActive(false)
                .create()

        fun anAdminUser() =
            aUser()
                .withUsername("관리자123")
                .withRoles(setOf(RoleData.ADMIN.toEntity()))
                .create()

        fun aUserWithAddress() =
            aUser()
                .withUsername("주소123")
                .withAddress(Address("12345", "서울시", "상세주소"))
                .create()

        fun aDeletedUser() =
            aUser()
                .withUsername("삭제123")
                .withActive(false)
                .create()
                .apply { delete() }
    }
}
