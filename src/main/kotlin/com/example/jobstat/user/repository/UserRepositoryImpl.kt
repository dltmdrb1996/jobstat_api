package com.example.jobstat.user.repository

import com.example.jobstat.core.extension.orThrowNotFound
import com.example.jobstat.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

internal interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean
}

@Repository
internal class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User = userJpaRepository.save(user)

    override fun findById(id: Long): User = userJpaRepository.findById(id).orThrowNotFound("id", id)

    override fun findByUsername(username: String): User = userJpaRepository.findByUsername(username).orThrowNotFound("username", username)

    override fun findByEmail(email: String): User = userJpaRepository.findByEmail(email).orThrowNotFound("email", email)

    override fun findAll(): List<User> = userJpaRepository.findAll()

    override fun deleteById(id: Long) {
        userJpaRepository.deleteById(id)
    }

    override fun delete(user: User) {
        userJpaRepository.delete(user)
    }

    override fun existsById(id: Long): Boolean = userJpaRepository.existsById(id)

    override fun existsByUsername(username: String): Boolean = userJpaRepository.existsByUsername(username)

    override fun existsByEmail(email: String): Boolean = userJpaRepository.existsByEmail(email)
}
