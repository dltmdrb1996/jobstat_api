package com.example.jobstat.core.security

import com.example.jobstat.core.security.PasswordUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 테스트용 FakePasswordUtil
 * 실제 암호화 대신 간단한 접두사를 추가하는 방식으로 동작
 */
class FakePasswordUtil : PasswordUtil {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun encode(password: String): String = "encoded:$password"

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean = encode(rawPassword) == encodedPassword
}
