package com.example.jobstat.core.security

data class AccessPayload(
    val id: Long,
    val roles: List<String>,
    val tokenType: TokenType = TokenType.ACCESS_TOKEN,
)

data class RefreshPayload(
    val id: Long,
    val roles: List<String>,
    val tokenType: TokenType = TokenType.REFRESH_TOKEN,
)

enum class TokenType(
    val value: Int,
) {
    ACCESS_TOKEN(0),
    REFRESH_TOKEN(1),
    ;

    companion object {
        fun fromValue(value: Int): TokenType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("유효하지 않은 토큰 타입입니다: $value")
    }
}
