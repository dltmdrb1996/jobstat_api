package com.wildrew.jobstat.core.core_security.util.context_util

interface TheadContextUtils {
    fun isAuthenticated(): Boolean

    fun getCurrentUserId(): Long?

    fun getCurrentUserRoles(): List<String>

    fun hasRole(role: String): Boolean

    fun hasAnyRole(vararg roles: String): Boolean

    fun hasAllRoles(vararg roles: String): Boolean

    fun isAdmin(): Boolean

    fun canAccess(resourceUserId: Long): Boolean

    fun getHighestRole(): String?
}
