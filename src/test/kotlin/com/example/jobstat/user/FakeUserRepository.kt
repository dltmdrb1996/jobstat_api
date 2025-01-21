package com.example.jobstat.user

import com.example.jobstat.user.entity.User
import com.example.jobstat.user.repository.UserRepository
import com.example.jobstat.utils.base.BaseFakeRepository
import com.example.jobstat.utils.IndexManager
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException

internal class FakeUserRepository : UserRepository {
    private val baseRepo = object : BaseFakeRepository<User, UserFixture>() {
        override fun fixture() = UserFixture.aUser()

        override fun createNewEntity(entity: User): User {
            return fixture()
                .withId(nextId())
                .withUsername(entity.username)
                .withEmail(entity.email)
                .withBirthDate(entity.birthDate)
                .withAddress(entity.address)
                .withActive(entity.isActive)
                .withRoles(entity.roles)
                .create()
        }

        override fun updateEntity(entity: User): User = entity

        override fun clearAdditionalState() {
            usernameIndex.clear()
            emailIndex.clear()
        }
    }

    private val usernameIndex = IndexManager<String, Long>()
    private val emailIndex = IndexManager<String, Long>()

    fun init() {
        save(UserFixture.aDefaultUser())
        save(UserFixture.anActiveUser())
        save(UserFixture.anInactiveUser())
    }

    override fun save(user: User): User {
        checkUniqueConstraints(user)
        val savedUser = baseRepo.save(user)

        usernameIndex.put(savedUser.username, savedUser.id)
        emailIndex.put(savedUser.email, savedUser.id)

        return savedUser
    }

    override fun findById(id: Long): User = baseRepo.findById(id)

    override fun findByUsername(username: String): User {
        val userId = usernameIndex.get(username)
            ?: throw EntityNotFoundException("User not found with username: $username")
        return findById(userId)
    }

    override fun findByEmail(email: String): User {
        val userId = emailIndex.get(email)
            ?: throw EntityNotFoundException("User not found with email: $email")
        return findById(userId)
    }

    override fun findAll(): List<User> = baseRepo.findAll()

    override fun deleteById(id: Long) {
        val user = findById(id)
        delete(user)
    }

    override fun delete(user: User) {
        baseRepo.delete(user)
        usernameIndex.remove(user.username)
        emailIndex.remove(user.email)
    }

    override fun existsById(id: Long): Boolean = baseRepo.existsById(id)

    override fun existsByUsername(username: String): Boolean =
        usernameIndex.containsKey(username)

    override fun existsByEmail(email: String): Boolean =
        emailIndex.containsKey(email)

    private fun checkUniqueConstraints(user: User) {
        usernameIndex.get(user.username)?.let { existingId ->
            if (existingId != user.id) {
                throw DuplicateKeyException("Username already exists: ${user.username}")
            }
        }

        emailIndex.get(user.email)?.let { existingId ->
            if (existingId != user.id) {
                throw DuplicateKeyException("Email already exists: ${user.email}")
            }
        }
    }

    fun clear() {
        baseRepo.clear()
    }

    // 테스트 데이터 생성을 위한 편의 메서드
    fun saveAll(count: Int, customizer: UserFixture.(Int) -> Unit) =
        baseRepo.saveAll(count, customizer)
}