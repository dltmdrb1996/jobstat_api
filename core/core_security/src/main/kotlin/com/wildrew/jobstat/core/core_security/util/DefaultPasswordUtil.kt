package com.wildrew.jobstat.core.core_security.util

import org.springframework.security.crypto.password.PasswordEncoder

interface PasswordUtil {
    fun encode(password: String): String

    fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean
}

class DefaultPasswordUtil(
    private val passwordEncoder: PasswordEncoder,
) : PasswordUtil {
    override fun encode(password: String): String = passwordEncoder.encode(password)

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean = passwordEncoder.matches(rawPassword, encodedPassword)
}
