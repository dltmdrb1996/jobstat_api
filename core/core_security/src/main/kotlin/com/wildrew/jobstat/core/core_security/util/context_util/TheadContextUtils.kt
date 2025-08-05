package com.wildrew.jobstat.core.core_security.util.context_util

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode

interface TheadContextUtils {
    fun isAuthenticated(): Boolean

    fun getCurrentUserIdOrFail(): Long

    fun getCurrentUserIdOrNull(): Long?

    fun getCurrentUserRoles(): List<String>

    fun hasRole(role: String): Boolean

    fun hasAnyRole(vararg roles: String): Boolean



    fun hasAllRoles(vararg roles: String): Boolean

    fun isAdmin(): Boolean

    fun canAccess(resourceUserId: Long): Boolean

    fun getHighestRole(): String?
}