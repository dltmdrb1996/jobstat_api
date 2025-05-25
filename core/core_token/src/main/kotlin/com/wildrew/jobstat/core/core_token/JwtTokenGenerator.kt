package com.wildrew.jobstat.core.core_token

import com.wildrew.jobstat.core.core_token.model.AccessPayload
import com.wildrew.jobstat.core.core_token.model.RefreshPayload
import com.wildrew.jobstat.core.core_token.model.TokenType
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

class JwtTokenGenerator(
    private val secret: String,
    private val accessTokenExpiration: Int,
    private val refreshTokenExpiration: Int,
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun createAccessToken(payload: AccessPayload): String = createToken(payload.id, payload.roles, payload.tokenType, accessTokenExpiration)

    fun createRefreshToken(payload: RefreshPayload): String = createToken(payload.id, payload.roles, payload.tokenType, refreshTokenExpiration)

    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration.toLong()

    init {
        log.info("JWT Token Generator initialized with secret key length: ${secret} characters")
    }

    private fun createToken(
        id: Long,
        roles: List<String>,
        tokenType: TokenType,
        expirationInSeconds: Int,
    ): String {
        val now: Instant = Instant.now() // 현재 시간을 Instant로 가져옴
        val expirationTime: Instant = now.plusSeconds(expirationInSeconds.toLong())

        return Jwts
            .builder()
            .apply {
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
