package com.example.jobstat.auth.user.repository

import com.example.jobstat.auth.user.entity.User
import com.example.jobstat.core.core_jpa_base.orThrowNotFound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

internal interface UserJpaRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id")
    fun findByIdWithRoles(id: Long): Optional<User>
}

@Repository
internal class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User = userJpaRepository.save(user)

    override fun findById(id: Long): User = userJpaRepository.findById(id).orThrowNotFound("User", id)

    override fun findByUsername(username: String): User = userJpaRepository.findByUsername(username).orThrowNotFound("User", username)

    override fun findByEmail(email: String): User = userJpaRepository.findByEmail(email).orThrowNotFound("User", email)

    override fun findAll(): List<User> = userJpaRepository.findAll()

    override fun deleteById(id: Long) = userJpaRepository.deleteById(id)

    override fun delete(user: User) = userJpaRepository.delete(user)

    override fun existsById(id: Long): Boolean = userJpaRepository.existsById(id)

    override fun existsByUsername(username: String): Boolean = userJpaRepository.existsByUsername(username)

    override fun existsByEmail(email: String): Boolean = userJpaRepository.existsByEmail(email)

    override fun findByIdWithRoles(id: Long): User = userJpaRepository.findByIdWithRoles(id).orThrowNotFound("User", id)

    override fun deleteAll() = userJpaRepository.deleteAll()
}
