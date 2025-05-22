package com.wildrew.app.auth.user.service

import com.wildrew.app.auth.user.entity.User
import java.time.LocalDate

interface UserService {
    fun createUser(
        username: String,
        email: String,
        password: String,
        birthDate: LocalDate,
    ): User

    fun deleteUser(id: Long)

    // 유저 정보 조회
    fun getUserById(id: Long): User

    fun getUserByUsername(username: String): User

    fun getUserByEmail(email: String): User

    fun getAllUsers(): List<User>

    fun getUserWithRoles(id: Long): User

    fun getUserRoles(id: Long): List<String>

    // 정보 검증
    fun validateUsername(username: String): Boolean

    fun validateEmail(email: String): Boolean

    fun isAccountEnabled(id: Long): Boolean

    // 업데이트
    fun enableUser(id: Long)

    fun disableUser(id: Long)

    fun updateUserPassword(
        userId: Long,
        newPassword: String,
    )

    fun updateUserEmail(
        userId: Long,
        newEmail: String,
    )

    fun updateUser(command: Map<String, Any>): User
}
