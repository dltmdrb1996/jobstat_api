package com.example.jobstat.utils

import com.example.jobstat.core.security.PasswordUtil

/**
 * 테스트용 FakePasswordUtil
 * 실제 암호화 대신 간단한 접두사를 추가하는 방식으로 동작
 */
class FakePasswordUtil : PasswordUtil {
    override fun encode(password: String): String = "encoded:$password"

    override fun matches(
        rawPassword: String,
        encodedPassword: String,
    ): Boolean = encodedPassword == "encoded:$rawPassword"
}
