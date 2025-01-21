package com.example.jobstat.user.service

interface TokenService {
    fun storeRefreshToken(
        refreshToken: String,
        userId: Long,
        expirationInSeconds: Long,
    )

    fun validateRefreshTokenAndReturnUserId(refreshToken: String): Long

    fun removeToken(userId: Long)
}
