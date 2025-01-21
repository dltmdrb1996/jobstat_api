package com.example.jobstat.user

import com.example.jobstat.user.usecase.SignUp
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
internal class UserController(
    private val signUp: SignUp,
) {
    // com.example.jobstat.user.AuthController.kt (add this endpoint)
//    @PostMapping("/signout")
//    fun signOut(@RequestBody signOutRequest: SignOutRequest): ResponseEntity<ApiResponse<SignOutResponse>> {
//        authService.signOut(SignOutRequest.refreshToken)
//        return ResponseEntity.ok().build()
//    }
}
