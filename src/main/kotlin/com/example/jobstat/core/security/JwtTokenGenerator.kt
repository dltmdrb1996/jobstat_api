package com.example.jobstat.core.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenGenerator(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.accessTokenExpiration}") private val accessTokenExpiration: Int,
    @Value("\${jwt.refreshTokenExpiration}") private val refreshTokenExpiration: Int,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun createAccessToken(payload: AccessPayload): String =
        createToken(payload.id, payload.tokenType, accessTokenExpiration)

    fun createRefreshToken(payload: RefreshPayload): String =
        createToken(payload.id, payload.tokenType, refreshTokenExpiration)

    fun getRefreshTokenExpiration(): Long {
        return refreshTokenExpiration.toLong()
    }

    private fun createToken(id : Long, tokenType: TokenType, expirationInSeconds : Int): String {
        val headerMap: MutableMap<String, Any> = HashMap()
        headerMap["typ"] = "JWT"
        headerMap["alg"] = "HS256"

        val claims: MutableMap<String, Any?> = HashMap()
        claims["userId"] = id
        claims["tokenType"] = tokenType.value

        val now = Date()
        val expiration = Date(now.time + expirationInSeconds * 1000)

        return Jwts.builder()
            .setHeader(headerMap)
            .setClaims(claims)
            .setExpiration(expiration)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }
}
