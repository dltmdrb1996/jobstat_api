package com.example.jobstat.auth.user.fake

import com.example.jobstat.auth.user.entity.Role
import com.example.jobstat.auth.user.entity.RoleData
import com.example.jobstat.auth.user.entity.User
import com.example.jobstat.auth.user.entity.UserRole
import com.example.jobstat.auth.user.model.Address
import com.example.jobstat.utils.TestFixture
import java.time.LocalDate
import java.time.LocalDateTime

internal class UserFixture private constructor(
    private var id: Long = 0L,
    private var username: String = "테스트사용자1",
    private var email: String = "test@example.com",
    private var password: String = "testpassword123!",
    private var birthDate: LocalDate = LocalDate.now().minusYears(20),
    private var address: Address? = null,
    private var isActive: Boolean = true,
    private var roles: Set<Role> = emptySet(),
    private var createdAt: LocalDateTime = LocalDateTime.now(),
    private var updatedAt: LocalDateTime = LocalDateTime.now(),
) : TestFixture<User> {
    override fun create(): User =
        User
            .create(
                username = username,
                email = email,
                password = password,
                birthDate = birthDate,
            ).apply {
                address?.let { updateAddress(it) }
                if (!isActive) disableAccount()
                roles.forEach { role ->
                    val userRole = UserRole.create(this, role)
                    assignRole(userRole)
                    role.assignRole(userRole)
                }
            }

    fun withId(id: Long) = apply { this.id = id }

    fun withUsername(username: String) = apply { this.username = username }

    fun withEmail(email: String) = apply { this.email = email }

    fun withPassword(password: String) = apply { this.password = password }

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
                .withUsername("activeUser1") // 알파벳+숫자, 길이 3~10
                .withPassword("activePass123!")
                .withActive(true)
                .create()

        fun anInactiveUser() =
            aUser()
                .withUsername("inactive2")
                .withPassword("inactivePass123!")
                .withActive(false)
                .create()

        fun anAdminUser() =
            aUser()
                .withUsername("adminUserX")
                .withPassword("adminPass123!")
                .withRoles(setOf(RoleData.ADMIN.toEntity()))
                .create()

        fun aUserWithAddress() =
            aUser()
                .withUsername("addrUser1")
                .withPassword("addressPass123!")
                .withAddress(Address("12345", "Seoul", "SomeDetail"))
                .create()

        fun aDeletedUser() =
            aUser()
                .withUsername("deletedUser99")
                .withPassword("deletedPass123!")
                .withActive(false)
                .create()
                .apply { delete() }
    }
}
