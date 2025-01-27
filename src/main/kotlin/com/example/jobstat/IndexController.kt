package com.example.jobstat

import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.annotation.Public
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Public
@RestController
@Profile("dev") // dev 프로파일에서만 활성화
internal class IndexController {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping(value = ["/", "/test"])
    fun helloWorld() = "Pong!"

    @GetMapping("/err")
    fun error() {
        logger.error("Test error")
        throw AppException.fromErrorCode(ErrorCode.INTERNAL_ERROR, "Test error", "Test error detail")
    }

    @GetMapping("/api/${RestConstants.Versions.V1}/test")
    fun apiTest() = "apiTest!"

    @GetMapping(value = ["/required"])
    fun helloRequiredWorld(
        @RequestParam(value = "msg", required = true) msg: String,
    ) = "Echo \"$msg\"!"
}
