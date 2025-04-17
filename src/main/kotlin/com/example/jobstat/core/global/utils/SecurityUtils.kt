package com.example.jobstat.core.global.utils

import com.example.jobstat.auth.user.entity.RoleData
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityUtils {
    private object Constants {
        const val ROLE_PREFIX = "ROLE_"
        const val ANONYMOUS_USER = "anonymousUser"
    }

    /**
     * 현재 사용자가 인증되었는지 확인
     */
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal != null &&
            authentication.principal != Constants.ANONYMOUS_USER &&
            authentication.principal is Long
    }

    /**
     * 인증된 사용자의 ID 반환 (미인증 시 null)
     */
    fun getCurrentUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is Long -> principal
            else -> null
        }
    }

    /**
     * 현재 사용자의 모든 권한 목록 반환
     */
    fun getCurrentUserRoles(): List<String> {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.principal != Constants.ANONYMOUS_USER) {
            authentication?.authorities?.map { it.authority.removePrefix(Constants.ROLE_PREFIX) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 특정 역할을 가지고 있는지 확인
     */
    fun hasRole(role: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal != Constants.ANONYMOUS_USER &&
            (authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false)
    }

    /**
     * 여러 역할 중 하나라도 가지고 있는지 확인 (Short circuit)
     */
    fun hasAnyRole(vararg roles: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal != Constants.ANONYMOUS_USER &&
            roles.any { role ->
                authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
            }
    }

    /**
     * 모든 역할을 가지고 있는지 확인
     */
    fun hasAllRoles(vararg roles: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal != Constants.ANONYMOUS_USER &&
            roles.all { role ->
                authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
            }
    }

    /**
     * 관리자 권한을 가지고 있는지 확인 (Short circuit)
     */
    fun isAdmin(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal != Constants.ANONYMOUS_USER &&
            hasRole(RoleData.ADMIN.name)
    }

    /**
     * 현재 사용자가 특정 리소스에 접근 가능한지 확인 (Short circuit)
     */
    fun canAccess(resourceUserId: Long): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication?.principal == Constants.ANONYMOUS_USER) return false

        return isAdmin() || getCurrentUserId() == resourceUserId
    }

    /**
     * 현재 사용자의 최상위 권한 반환
     */
    fun getHighestRole(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication?.principal == Constants.ANONYMOUS_USER) return null

        val roles = getCurrentUserRoles()
        return when {
            roles.contains(RoleData.ADMIN.name) -> RoleData.ADMIN.name
            roles.contains(RoleData.MANAGER.name) -> RoleData.MANAGER.name
            roles.contains(RoleData.USER.name) -> RoleData.USER.name
            else -> null
        }
    }
}
