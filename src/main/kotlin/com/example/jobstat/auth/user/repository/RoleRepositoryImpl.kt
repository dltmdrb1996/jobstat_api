package com.example.jobstat.auth.user.repository

import com.example.jobstat.auth.user.entity.Role
import com.example.jobstat.core.extension.orThrowNotFound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

internal interface RoleJpaRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Optional<Role>

    fun existsByName(name: String): Boolean
}

// Repository 구현체
@Repository
internal class RoleRepositoryImpl(
    private val roleJpaRepository: RoleJpaRepository,
) : RoleRepository {
    override fun save(role: Role): Role = roleJpaRepository.save(role)

    override fun findById(id: Long): Role = roleJpaRepository.findById(id).orThrowNotFound("id", id)

    override fun findByName(name: String): Role = roleJpaRepository.findByName(name).orThrowNotFound("name", name)

    override fun findAll(): List<Role> = roleJpaRepository.findAll()

    override fun deleteById(id: Long) = roleJpaRepository.deleteById(id)

    override fun delete(role: Role) = roleJpaRepository.delete(role)

    override fun existsById(id: Long): Boolean = roleJpaRepository.existsById(id)

    override fun existsByName(name: String): Boolean = roleJpaRepository.existsByName(name)
}
