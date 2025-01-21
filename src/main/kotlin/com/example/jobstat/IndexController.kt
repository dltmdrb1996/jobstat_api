package com.example.jobstat

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
internal class IndexController {
    @Suppress("SameReturnValue", "SameReturnValue")
    @GetMapping(value = ["", "/", "/test"])
    fun helloWorld() = "Pong!"

    @GetMapping("/error")
    fun error(): Unit = throw AppException.fromErrorCode(ErrorCode.INTERNAL_ERROR)

    @GetMapping(value = ["/required"])
    fun helloRequiredWorld(
        @RequestParam(value = "msg", required = true) msg: String,
    ) = "Echo \"$msg\"!"
    
}
