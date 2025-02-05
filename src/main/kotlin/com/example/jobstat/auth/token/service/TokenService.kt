package com.example.jobstat.auth.token.service

interface TokenService {
    fun storeRefreshToken(
        refreshToken: String,
        userId: Long,
        expirationInSeconds: Long,
    )

    fun validateRefreshTokenAndReturnUserId(refreshToken: String): Long

    fun removeToken(userId: Long)
}
