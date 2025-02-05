package com.example.jobstat.core.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordUtil(
    private val passwordEncoder: PasswordEncoder
) {
    fun encode(password: String): String = passwordEncoder.encode(password)

    fun matches(rawPassword: String, encodedPassword: String): Boolean =
        passwordEncoder.matches(rawPassword, encodedPassword)
}