package com.example.jobstat.core.security

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

interface PasswordUtil {
    fun encode(password: String): String

    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean
}

@Component
class BCryptPasswordUtil(
    private val passwordEncoder: PasswordEncoder,
) : PasswordUtil {
    override fun encode(password: String): String = passwordEncoder.encode(password)

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean = passwordEncoder.matches(rawPassword, encodedPassword)
}
