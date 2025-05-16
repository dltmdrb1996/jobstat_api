package com.example.jobstat.core.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenGenerator(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.accessTokenExpiration}") private val accessTokenExpiration: Int,
    @Value("\${jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Int,
) {
    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun createAccessToken(payload: AccessPayload): String = createToken(payload.id, payload.roles, payload.tokenType, accessTokenExpiration)

    fun createRefreshToken(payload: RefreshPayload): String = createToken(payload.id, payload.roles, payload.tokenType, refreshTokenExpiration)

    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration.toLong()

    private fun createToken(
        id: Long,
        roles: List<String>,
        tokenType: TokenType,
        expirationInSeconds: Int,
    ): String {
        val now: Instant = Instant.now() // 현재 시간을 Instant로 가져옴
        val expirationTime: Instant = now.plusSeconds(expirationInSeconds.toLong())

        return Jwts.builder().apply {
            this.header().add("typ", "JWT")
            this.claim("userId", id)
            this.claim("tokenType", tokenType.value)
            this.claim("roles", roles)
            this.issuedAt(Date.from(now))
            this.expiration(Date.from(expirationTime))
            this.signWith(key)
        }.compact()
    }
}
