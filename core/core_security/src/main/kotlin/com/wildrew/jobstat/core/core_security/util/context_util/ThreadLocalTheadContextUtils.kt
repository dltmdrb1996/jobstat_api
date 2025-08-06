package com.wildrew.jobstat.core.core_security.util.context_util

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
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

    override fun getCurrentUserIdOrFail(): Long = getCurrentUserIdOrNull() ?: throw AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE)

    override fun getCurrentUserIdOrNull(): Long? {
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
        return isAuthenticated() &&
            (authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false)
    }

    override fun hasAnyRole(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false
        val authentication = SecurityContextHolder.getContext().authentication
        return roles.any { role ->
            authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
        }
    }

    override fun hasAllRoles(vararg roles: String): Boolean {
        if (!isAuthenticated()) return false
        val authentication = SecurityContextHolder.getContext().authentication
        return roles.all { role ->
            authentication?.authorities?.any { it.authority == "${Constants.ROLE_PREFIX}$role" } ?: false
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
            else -> null
        }
    }
}
