package com.wildrew.jobstat.core.core_security.util.context_util

import com.wildrew.jobstat.core.core_security.util.ScopedSecurityContextHolder

class ScopedValueTheadContextUtils : TheadContextUtils { // 인터페이스 구현

    private object Constants {
        const val ROLE_PREFIX = "ROLE_"
    }

    override fun isAuthenticated(): Boolean {
        val authentication = ScopedSecurityContextHolder.getContext().authentication
        return authentication?.principal != null &&
            authentication.principal is Long
    }

    override fun getCurrentUserId(): Long? {
        val authentication = ScopedSecurityContextHolder.getContext().authentication
        return when (val principal = authentication?.principal) {
            is Long -> principal
            else -> null // 인증되지 않았거나 principal이 Long 타입이 아닌 경우
        }
    }

    override fun getCurrentUserRoles(): List<String> {
        val authentication = ScopedSecurityContextHolder.getContext().authentication
        // isAuthenticated() 와 유사한 조건 사용
        return if (authentication?.principal != null && authentication.principal is Long) {
            authentication.authorities?.map { it.authority.removePrefix(Constants.ROLE_PREFIX) } ?: emptyList()
        } else {
            emptyList()
        }
    }

    override fun hasRole(role: String): Boolean {
        if (!isAuthenticated()) return false
        val authentication = ScopedSecurityContextHolder.getContext().authentication
        return authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
    }

    override fun hasAnyRole(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false
        val authentication = ScopedSecurityContextHolder.getContext().authentication
        return roles.any { role ->
            authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
        }
    }

    override fun hasAllRoles(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false
        val authentication = ScopedSecurityContextHolder.getContext().authentication
        return roles.all { role ->
            authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
        }
    }

    override fun isAdmin(): Boolean = hasRole("ADMIN")

    override fun canAccess(resourceUserId: Long): Boolean {
        if (!isAuthenticated()) return false
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
