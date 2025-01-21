package com.example.jobstat.user.service

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.extension.trueOrThrow
import com.example.jobstat.user.entity.*
import com.example.jobstat.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
internal class UserServiceImpl(
    private val userRepository: UserRepository,
) : UserService {

    override fun createUser(username: String, email: String, birthDate: LocalDate): User {
        val user = User.create(username, email, birthDate)
        isEmailAvailable(user.email).trueOrThrow { AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 이메일입니다.") }
        isUsernameAvailable(user.username).trueOrThrow { AppException.fromErrorCode(ErrorCode.DUPLICATE_RESOURCE, "이미 사용중인 아이디입니다.") }
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

    override fun isEmailAvailable(email: String) : Boolean = !userRepository.existsByEmail(email)

    override fun isActivated(id: Long): Boolean = userRepository.findById(id).isActive
}
