package com.example.jobstat.auth.user.service

import org.springframework.cache.Cache

interface LoginAttemptService {
    fun recordFailedAttempt(username: String)
    fun isBlocked(username: String): Boolean
    fun clearAttempts(username: String)
}

