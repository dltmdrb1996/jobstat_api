package com.example.jobstat

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.security.annotation.Public
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Public
@RestController
internal class IndexController {
    @GetMapping(value = ["/", "/test"])
    fun helloWorld() = "Pong!"

    @GetMapping("/err")
    fun error(): Unit = throw AppException.fromErrorCode(ErrorCode.INTERNAL_ERROR, "Test error", "Test error detail")

    @GetMapping("/api/v1/test")
    fun apiTest() = "apiTest!"

    @GetMapping(value = ["/required"])
    fun helloRequiredWorld(
        @RequestParam(value = "msg", required = true) msg: String,
    ) = "Echo \"$msg\"!"
}
