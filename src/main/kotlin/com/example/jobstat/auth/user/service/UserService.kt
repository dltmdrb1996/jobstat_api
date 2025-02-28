package com.example.jobstat.auth.user.service

import com.example.jobstat.auth.user.entity.ReadUser
import java.time.LocalDate

interface UserService {
    fun createUser(
        username: String,
        email: String,
        password: String,
        birthDate: LocalDate,
    ): ReadUser

    fun deleteUser(id: Long)

    // 유저 정보 조회
    fun getUserById(id: Long): ReadUser

    fun getUserByUsername(username: String): ReadUser

    fun getUserByEmail(email: String): ReadUser

    fun getAllUsers(): List<ReadUser>

    fun getUserWithRoles(id: Long): ReadUser

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

    fun updateUser(command: Map<String, Any>): ReadUser
}
