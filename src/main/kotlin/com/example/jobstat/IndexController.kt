package com.example.jobstat

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.SecurityUtils
import com.example.jobstat.core.security.annotation.AdminAuth
import com.example.jobstat.core.security.annotation.Public
import com.example.jobstat.core.security.annotation.PublicWithTokenCheck
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("dev") // dev 프로파일에서만 활성화
internal class IndexController(
    private val securityUtils: SecurityUtils,
) {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @Public
    @GetMapping(value = ["/", "/test"])
    fun helloWorld() = "Pong!"

    @Public
    @GetMapping("/err")
    fun error() {
        log.error("Test error")
        throw AppException.fromErrorCode(ErrorCode.INTERNAL_ERROR, "Test error", "Test error detail")
    }

    @PublicWithTokenCheck
    @GetMapping("/api/test/public/check")
    fun apiTestTokenCheck(): String {
        val isAuthenticated = securityUtils.isAuthenticated()
        val userId = securityUtils.getCurrentUserId()
        val role = securityUtils.getCurrentUserRoles()
        val highRole = securityUtils.getHighestRole()
        return "apiTestTokenCheck: isAuthenticated=$isAuthenticated userId=$userId, role=$role, highRole=$highRole"
    }

    @AdminAuth
    @GetMapping("/api/test/auth")
    fun apiTestAuth() = "apiTestAuth"

    @GetMapping("/api/test/token")
    fun apiTestToken() = "apiTest"

    @Public
    @GetMapping("api/test/public")
    fun apiTest() = "apiTest!"

    @Public
    @GetMapping(value = ["/required"])
    fun helloRequiredWorld(
        @RequestParam(value = "msg", required = true) msg: String,
    ) = "Echo \"$msg\"!"
}
