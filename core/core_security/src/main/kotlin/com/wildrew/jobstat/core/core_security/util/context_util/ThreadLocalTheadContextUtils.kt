package com.wildrew.jobstat.core.core_security.util.context_util

import org.springframework.security.core.context.SecurityContextHolder

class ThreadLocalTheadContextUtils : TheadContextUtils {

    private object Constants {
        const val ROLE_PREFIX = "ROLE_"
        const val ANONYMOUS_USER = "anonymousUser"
    }

    override fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal != null &&
            authentication.principal != Constants.ANONYMOUS_USER &&
            authentication.principal is Long
    }

    override fun getCurrentUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is Long -> principal
            else -> null
        }
    }

    override fun getCurrentUserRoles(): List<String> {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.principal != null && authentication.principal != Constants.ANONYMOUS_USER && authentication.principal is Long) {
            authentication.authorities?.map { it.authority.removePrefix(Constants.ROLE_PREFIX) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    override fun hasRole(role: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        // isAuthenticated()를 먼저 체크하여 중복 로직 줄이기 가능
        return isAuthenticated() && // 인증된 경우에만 역할 검사
            (authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false)
    }

    override fun hasAnyRole(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false // 인증되지 않았으면 바로 false
        val authentication = SecurityContextHolder.getContext().authentication
        return roles.any { role ->
            authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
        }
    }

    override fun hasAllRoles(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false // 인증되지 않았으면 바로 false
        val authentication = SecurityContextHolder.getContext().authentication
        return roles.all { role ->
            authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
        }
    }

    override fun isAdmin(): Boolean {
        // RoleData.ADMIN.name이 정확한 ADMIN 역할 문자열을 반환한다고 가정
        return hasRole("ADMIN")
    }

    override fun canAccess(resourceUserId: Long): Boolean {
        if (!isAuthenticated()) return false // getCurrentUserId()가 null을 반환할 것이므로 이 검사는 유효

        // isAdmin() 내부에서도 isAuthenticated()가 호출될 수 있으므로,
        // 최적화를 위해 isAdmin()을 먼저 호출하고, 그 다음 ID 비교를 할 수 있음.
        // 또는, 현재 사용자가 인증되었다는 것을 이미 알고 있으므로 바로 역할/ID 비교.
        return isAdmin() || getCurrentUserId() == resourceUserId
    }

    override fun getHighestRole(): String? {
        if (!isAuthenticated()) return null
        val roles = getCurrentUserRoles()
        return when {
            roles.contains("ADMIN") -> "ADMIN"
            roles.contains("MANAGER") -> "MANAGER"
            roles.contains("USER") -> "USER"
            else -> null
        }
    }
}