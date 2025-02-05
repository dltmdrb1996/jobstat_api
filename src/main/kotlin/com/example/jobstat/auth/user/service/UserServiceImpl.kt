package com.example.jobstat.auth.user.service

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.extension.trueOrThrow
import com.example.jobstat.core.security.PasswordUtil
import com.example.jobstat.auth.user.entity.ReadUser
import com.example.jobstat.auth.user.entity.RoleData
import com.example.jobstat.auth.user.entity.User
import com.example.jobstat.auth.user.entity.UserRole
import com.example.jobstat.auth.user.repository.RoleRepository
import com.example.jobstat.auth.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
internal class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordUtil: PasswordUtil,
) : UserService {
    override fun createUser(
        username: String,
        email: String,
        password: String,
        birthDate: LocalDate,
    ): User {
        val encodedPassword = passwordUtil.encode(password)
        val user = User.create(username, email, encodedPassword, birthDate)

        isEmailAvailable(user.email).trueOrThrow {
            AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 이메일입니다.")
        }
        isUsernameAvailable(user.username).trueOrThrow {
            AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 아이디입니다.")
        }

        // 기본 USER 역할 부여
        val userRole = roleRepository.findByName(RoleData.USER.name)
        val userRoleEntity = UserRole(user, userRole)
        user.addRole(userRoleEntity)

        return userRepository.save(user)
    }

    override fun getUserById(id: Long): User = userRepository.findById(id)

    override fun getUserByUsername(username: String): User = userRepository.findByUsername(username)

    override fun getUserByEmail(email: String): User = userRepository.findByEmail(email)

    override fun getAllUsers(): List<User> = userRepository.findAll()

    override fun updateUser(command: Map<String, Any>): ReadUser {
        TODO("Not yet implemented")
    }

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
}
