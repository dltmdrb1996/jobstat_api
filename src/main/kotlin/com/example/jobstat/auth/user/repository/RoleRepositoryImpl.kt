package com.example.jobstat.auth.user.repository

import com.example.jobstat.auth.user.entity.Role
import com.example.jobstat.core.core_jpa_base.orThrowNotFound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
internal interface RoleJpaRepository : JpaRepository<Role, Long> {
    @Query(
        """
        SELECT DISTINCT r FROM Role r 
        LEFT JOIN FETCH r._userRoles ur 
        LEFT JOIN FETCH ur.user 
        WHERE r.name = :name
    """,
    )
    fun findByName(name: String): Optional<Role>

    @Query(
        """
        SELECT DISTINCT r FROM Role r 
        LEFT JOIN FETCH r._userRoles ur 
        LEFT JOIN FETCH ur.user 
        WHERE r.id = :id
    """,
    )
    fun findByIdWithUsers(id: Long): Optional<Role>

    fun existsByName(name: String): Boolean
}

@Repository
internal class RoleRepositoryImpl(
    private val roleJpaRepository: RoleJpaRepository,
) : RoleRepository {
    override fun save(role: Role): Role = roleJpaRepository.save(role)

    override fun findById(id: Long): Role = roleJpaRepository.findByIdWithUsers(id).orThrowNotFound("Role", id)

    override fun findByName(name: String): Role = roleJpaRepository.findByName(name).orThrowNotFound("Role", name)

    override fun findAll(): List<Role> = roleJpaRepository.findAll()

    override fun deleteById(id: Long) = roleJpaRepository.deleteById(id)

    override fun delete(role: Role) = roleJpaRepository.delete(role)

    override fun existsById(id: Long): Boolean = roleJpaRepository.existsById(id)

    override fun existsByName(name: String): Boolean = roleJpaRepository.existsByName(name)

    override fun deleteAll() = roleJpaRepository.deleteAll()
}
