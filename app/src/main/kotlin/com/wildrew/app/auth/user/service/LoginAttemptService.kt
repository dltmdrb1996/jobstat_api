package com.wildrew.app.auth.user.service

interface LoginAttemptService {
    fun incrementFailedAttempts(username: String)

    fun isAccountLocked(username: String): Boolean

    fun resetAttempts(username: String)
}
