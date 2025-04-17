package com.example.jobstat.auth.user.usecase

import com.example.jobstat.auth.user.service.UserService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class UpdateUserStatus(
    private val userService: UserService,
    validator: Validator,
) : ValidUseCase<UpdateUserStatus.Request, Unit>(validator) {
    @Transactional
    override fun invoke(request: Request) {
        super.invoke(request)
    }

    override fun execute(request: Request): Unit =
        with(request) {
            userService.updateUser(
                mapOf(
                    "id" to userId,
                    "isActive" to active,
                ),
            )
        }

    @Schema(
        name = "UpdateUserStatusRequest",
        description = "사용자 계정 상태 업데이트 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "사용자 ID",
            example = "1",
            required = true,
        )
        @field:Positive(message = "사용자 ID는 양수여야 합니다")
        @field:NotNull(message = "사용자 ID는 필수 값입니다")
        val userId: Long,
        @field:Schema(
            description = "계정 활성화 상태",
            example = "true",
            required = true,
        )
        @field:NotNull(message = "활성화 상태는 필수 값입니다")
        val active: Boolean,
    )
}
