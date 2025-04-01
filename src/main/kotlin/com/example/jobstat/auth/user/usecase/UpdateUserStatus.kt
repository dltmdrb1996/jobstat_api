package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.transaction.Transactional
import jakarta.validation.Validator
import org.springframework.stereotype.Service

@Service
internal class UpdateUserStatus(
    private val userService: UserService,
    validator: Validator,
) : ValidUseCase<UpdateUserStatus.Request, Unit>(validator) {
    @Transactional
    override fun execute(request: Request): Unit = with(request) {
        // 사용자 상태 업데이트
        userService.updateUser(
            mapOf(
                "id" to userId,
                "isActive" to active,
            )
        )
    }

    data class Request(
        val userId: Long,
        val active: Boolean,
    )
}
