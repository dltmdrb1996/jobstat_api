package com.example.jobstat.auth.user.service

import com.example.jobstat.auth.user.entity.ReadUser
import com.example.jobstat.auth.user.entity.RoleData
import com.example.jobstat.auth.user.entity.User
import com.example.jobstat.auth.user.entity.UserRole
import com.example.jobstat.auth.user.repository.RoleRepository
import com.example.jobstat.auth.user.repository.UserRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.extension.trueOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
internal class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
) : UserService {
    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    override fun createUser(
        username: String,
        email: String,
        password: String,
        birthDate: LocalDate,
    ): User {
        val user = User.create(username, email, password, birthDate)

        isEmailAvailable(user.email).trueOrThrow {
            AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 이메일입니다.")
        }
        isUsernameAvailable(user.username).trueOrThrow {
            AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 아이디입니다.")
        }

        val role = roleRepository.findById(RoleData.USER.id)
        logger.info("userRole: ${role.name}")
        logger.info("userRole: ${role.id}")
        val userRole = UserRole.create(user, role)
        user.addRole(userRole)
        role.addUserRole(userRole)
        return userRepository.save(user)
    }

    override fun getUserById(id: Long): User = userRepository.findById(id)

    override fun getUserByUsername(username: String): User = userRepository.findByUsername(username)

    override fun getUserByEmail(email: String): User = userRepository.findByEmail(email)

    override fun getAllUsers(): List<User> = userRepository.findAll()

    override fun deleteUser(id: Long) = userRepository.deleteById(id)

    override fun isUsernameAvailable(username: String): Boolean = !userRepository.existsByUsername(username)

    override fun isEmailAvailable(email: String): Boolean = !userRepository.existsByEmail(email)

    override fun isActivated(id: Long): Boolean = userRepository.findById(id).isActive

    override fun getUserWithRoles(id: Long): ReadUser {
        val user = userRepository.findByIdWithRoles(id)
        return user
    }

    override fun getUserRoles(id: Long): List<String> {
        val user = userRepository.findByIdWithRoles(id)
        return user.getRolesString()
    }

    override fun updateUser(command: Map<String, Any>): ReadUser {
        val userId = command["id"] as Long
        val user = userRepository.findById(userId)

        command.forEach { (key, value) ->
            when (key) {
                "password" -> user.updatePassword(value as String)
                "email" -> user.updateEmail(value as String)
                "isActive" -> if (value as Boolean) user.activate() else user.deactivate()
                // 추가 필드가 있다면 여기에 추가
            }
        }

        return userRepository.save(user)
    }

    override fun activateUser(id: Long) {
        updateUser(
            mapOf(
                "id" to id,
                "isActive" to true,
            ),
        )
    }

    override fun deactivateUser(id: Long) {
        updateUser(
            mapOf(
                "id" to id,
                "isActive" to false,
            ),
        )
    }

    override fun updatePassword(
        userId: Long,
        newPassword: String,
    ) {
        updateUser(
            mapOf(
                "id" to userId,
                "password" to newPassword,
            ),
        )
    }

    override fun updateEmail(
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
