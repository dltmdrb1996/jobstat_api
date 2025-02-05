package com.example.jobstat.auth

import com.example.jobstat.core.constants.RestConstants
import com.example.jobstat.auth.user.usecase.SignUp
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/user")
internal class UserController(
    private val signUp: SignUp,
) {


    // com.example.jobstat.user.controller.AuthController.kt (add this endpoint)
//    @PostMapping("/signout")
//    fun signOut(@RequestBody signOutRequest: SignOutRequest): ResponseEntity<ApiResponse<SignOutResponse>> {
//        authService.signOut(SignOutRequest.refreshToken)
//        return ResponseEntity.ok().build()
//    }
}
