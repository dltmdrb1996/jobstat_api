package com.example.jobstat.auth.user

import com.example.jobstat.auth.user.usecase.Register
import com.example.jobstat.core.constants.RestConstants
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/user")
internal class UserController(
    private val register: Register,
)
