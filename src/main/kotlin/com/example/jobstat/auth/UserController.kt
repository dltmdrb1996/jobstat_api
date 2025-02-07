package com.example.jobstat.auth

import com.example.jobstat.auth.user.usecase.Register
import com.example.jobstat.core.constants.RestConstants
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/${RestConstants.Versions.V1}/user")
internal class UserController(
    private val register: Register,
) {
    // 로그아웃 엔드포인트 추가 예정
    // @PostMapping("/signout")
    // fun signOut(@RequestBody signOutRequest: SignOutRequest): ResponseEntity<ApiResponse<SignOutResponse>> {
    //     authService.signOut(SignOutRequest.refreshToken)
    //     return ResponseEntity.ok().build()
    // }
}
