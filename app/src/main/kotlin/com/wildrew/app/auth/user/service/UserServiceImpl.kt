package com.wildrew.app.auth.user.service

import com.wildrew.app.auth.user.UserConstants
import com.wildrew.app.auth.user.entity.RoleData
import com.wildrew.app.auth.user.entity.User
import com.wildrew.app.auth.user.entity.UserRole
import com.wildrew.app.auth.user.repository.RoleRepository
import com.wildrew.app.auth.user.repository.UserRepository
import com.wildrew.app.eacheach.trueOrThrow
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
) : UserService {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun createUser(
        username: String,
        email: String,
        password: String,
        birthDate: LocalDate,
    ): User =
        User.create(username, email, password, birthDate).also { user ->
            validateEmail(user.email).trueOrThrow {
                AppException.fromErrorCode(
                    ErrorCode.DUPLICATE_RESOURCE,
                    UserConstants.ErrorMessages.DUPLICATE_EMAIL,
                )
            }
            validateUsername(user.username).trueOrThrow {
                AppException.fromErrorCode(
                    ErrorCode.DUPLICATE_RESOURCE,
                    UserConstants.ErrorMessages.DUPLICATE_USERNAME,
                )
            }

            val role = roleRepository.findById(RoleData.USER.id)
            UserRole.create(user, role)
            userRepository.save(user)
        }

    override fun getUserById(id: Long): User = userRepository.findById(id)

    override fun getUserByUsername(username: String): User = userRepository.findByUsername(username)

    override fun getUserByEmail(email: String): User = userRepository.findByEmail(email)

    override fun getAllUsers(): List<User> = userRepository.findAll()

    override fun deleteUser(id: Long) = userRepository.deleteById(id)

    override fun validateUsername(username: String): Boolean {
        if (!username.matches(Regex(UserConstants.Patterns.USERNAME_PATTERN))) {
            throw AppException.fromErrorCode(
                ErrorCode.INVALID_ARGUMENT,
                UserConstants.ErrorMessages.INVALID_USERNAME,
            )
        }
        return !userRepository.existsByUsername(username)
    }

    override fun validateEmail(email: String): Boolean {
        if (!email.matches(Regex(UserConstants.Patterns.EMAIL_PATTERN))) {
            throw AppException.fromErrorCode(
                ErrorCode.INVALID_ARGUMENT,
                UserConstants.ErrorMessages.INVALID_EMAIL,
            )
        }
        return !userRepository.existsByEmail(email)
    }

    override fun isAccountEnabled(id: Long): Boolean = userRepository.findById(id).isActive

    override fun getUserWithRoles(id: Long): User = userRepository.findByIdWithRoles(id)

    override fun getUserRoles(id: Long): List<String> = userRepository.findByIdWithRoles(id).getRolesString()

    override fun updateUser(command: Map<String, Any>): User {
        val userId = command["id"] as Long
        return userRepository
            .findById(userId)
            .apply {
                command.forEach { (key, value) ->
                    when (key) {
                        "password" -> updatePassword(value as String)
                        "email" -> updateEmail(value as String)
                        "isActive" -> if (value as Boolean) enableAccount() else disableAccount()
                        // 추가 필드가 있다면 여기에 추가
                    }
                }
            }.let(userRepository::save)
    }

    override fun enableUser(id: Long) {
        updateUser(
            mapOf(
                "id" to id,
                "isActive" to true,
            ),
        )
    }

    override fun disableUser(id: Long) {
        updateUser(
            mapOf(
                "id" to id,
                "isActive" to false,
            ),
        )
    }

    override fun updateUserPassword(
        userId: Long,
        newPassword: String,
    ) {
        require(newPassword.matches(Regex(UserConstants.Patterns.PASSWORD_PATTERN))) {
            UserConstants.ErrorMessages.INVALID_PASSWORD
        }
        updateUser(
            mapOf(
                "id" to userId,
                "password" to newPassword,
            ),
        )
    }

    override fun updateUserEmail(
        userId: Long,
        newEmail: String,
    ) {
        updateUser(
            mapOf(
                "id" to userId,
                "email" to newEmail,
            ),
        )
    }
}
