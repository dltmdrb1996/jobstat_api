package com.wildrew.jobstat.auth.user.fake

import com.wildrew.jobstat.auth.user.entity.Role
import com.wildrew.jobstat.auth.utils.IdFixture
import com.wildrew.jobstat.core.core_global.model.RoleData

class RoleFixture private constructor(
    private var id: Long = 0L,
    private var name: String = "USER",
) : IdFixture<Role>() {
    override fun create(): Role {
        val role = Role.create(name = name)
        if (id > 0L) {
            setIdByReflection(role, id)
        }
        return role
    }

    fun withId(id: Long) = apply { this.id = id }

    fun withName(name: String) = apply { this.name = name }

    companion object {
        fun aRole() = RoleFixture()

        // 기존 예시들...
        fun aUserRole() =
            aRole()
                .withId(RoleData.USER.id)
                .withName(RoleData.USER.name)
                .create()

        fun anAdminRole() =
            aRole()
                .withId(RoleData.ADMIN.id)
                .withName(RoleData.ADMIN.name)
                .create()

        fun aManagerRole() =
            aRole()
                .withId(RoleData.MANAGER.id)
                .withName(RoleData.MANAGER.name)
                .create()

        fun defaultRoles() =
            listOf(
                aUserRole(),
                anAdminRole(),
                aManagerRole(),
            )
    }
}
