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

    fun getUserById(id: Long): ReadUser

    fun getUserByUsername(username: String): ReadUser

    fun getUserByEmail(email: String): ReadUser

    fun getAllUsers(): List<ReadUser>

    fun updateUser(command: Map<String, Any>): ReadUser

    fun deleteUser(id: Long)

    fun isUsernameAvailable(username: String): Boolean

    fun isEmailAvailable(email: String): Boolean

    fun isActivated(id: Long): Boolean

    fun getUserWithRoles(id: Long): ReadUser

    fun getUserRoles(id: Long): List<String>
}
