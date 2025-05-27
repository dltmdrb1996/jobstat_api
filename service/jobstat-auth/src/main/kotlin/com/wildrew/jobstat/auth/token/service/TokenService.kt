package com.wildrew.jobstat.auth.token.service

interface TokenService {
    fun saveToken(
        refreshToken: String,
        userId: Long,
        expirationInSeconds: Long,
    )

    fun getUserIdFromToken(refreshToken: String): Long

    fun removeToken(userId: Long)

    fun invalidateRefreshToken(refreshToken: String)
}
