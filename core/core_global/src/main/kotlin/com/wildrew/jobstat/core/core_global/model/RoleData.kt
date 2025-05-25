package com.wildrew.jobstat.core.core_global.model

enum class RoleData(
    val id: Long,
    private val roleName: String,
    val description: String,
) {
    USER(1L, "USER", "일반 사용자 역할"),
    ADMIN(2L, "ADMIN", "관리자 역할"),
    MANAGER(3L, "MANAGER", "매니저 역할"),
    ;
}
