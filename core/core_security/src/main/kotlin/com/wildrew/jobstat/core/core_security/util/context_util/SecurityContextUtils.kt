package com.wildrew.jobstat.core.core_security.util.context_util

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

class SecurityContextUtils : TheadContextUtils {

    private object Constants {
        const val ROLE_PREFIX = "ROLE_"
        const val ANONYMOUS_USER = "anonymousUser"
    }

    private fun getAuthentication(): Authentication? {
        return SecurityContextHolder.getContext().authentication
    }

    override fun isAuthenticated(): Boolean {
        val auth = getAuthentication()
        return auth != null &&
            auth.isAuthenticated &&
            auth.principal != null &&
            auth.principal != Constants.ANONYMOUS_USER &&
            auth.principal is Long
    }

    override fun getCurrentUserIdOrFail(): Long {
        return getCurrentUserIdOrNull() ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)
    }

    override fun getCurrentUserIdOrNull(): Long? {
        if (!isAuthenticated()) {
            return null
        }
        return getAuthentication()?.principal as? Long
    }

    override fun getCurrentUserRoles(): List<String> {
        if (!isAuthenticated()) {
            return emptyList()
        }
        return getAuthentication()?.authorities
            ?.map { it.authority.removePrefix(Constants.ROLE_PREFIX) }
            ?: emptyList()
    }

    override fun hasRole(role: String): Boolean {
        if (!isAuthenticated()) return false
        val roleWithPrefix = if (role.startsWith(Constants.ROLE_PREFIX)) role else "${Constants.ROLE_PREFIX}$role"
        return getAuthentication()?.authorities?.any { it.authority == roleWithPrefix } ?: false
    }

    override fun hasAnyRole(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false
        val authorities = getAuthentication()?.authorities?.map { it.authority } ?: return false
        return roles.any { role ->
            val roleWithPrefix = if (role.startsWith(Constants.ROLE_PREFIX)) role else "${Constants.ROLE_PREFIX}$role"
            authorities.contains(roleWithPrefix)
        }
    }

    override fun hasAllRoles(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false
        val authorities = getAuthentication()?.authorities?.map { it.authority } ?: return false
        return roles.all { role ->
            val roleWithPrefix = if (role.startsWith(Constants.ROLE_PREFIX)) role else "${Constants.ROLE_PREFIX}$role"
            authorities.contains(roleWithPrefix)
        }
    }

    override fun isAdmin(): Boolean = hasRole("ADMIN")

    override fun canAccess(resourceUserId: Long): Boolean {
        if (!isAuthenticated()) return false
        return isAdmin() || getCurrentUserIdOrNull() == resourceUserId
    }

    override fun getHighestRole(): String? {
        if (!isAuthenticated()) return null
        val roles = getCurrentUserRoles()
        return when {
            roles.contains("ADMIN") -> "ADMIN"
            roles.contains("MANAGER") -> "MANAGER"
            roles.contains("USER") -> "USER"
            else -> roles.firstOrNull() // 또는 null
        }
    }
}