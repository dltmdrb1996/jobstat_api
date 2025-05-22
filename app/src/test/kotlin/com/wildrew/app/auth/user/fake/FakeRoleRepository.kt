package com.wildrew.app.auth.user.fake

import com.wildrew.jobstat.auth.user.entity.Role
import com.wildrew.jobstat.auth.user.repository.RoleRepository
import com.wildrew.jobstat.utils.IndexManager
import com.wildrew.jobstat.utils.base.BaseFakeRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException

internal class FakeRoleRepository : RoleRepository {
    private val baseRepo =
        object : BaseFakeRepository<Role, RoleFixture>() {
            override fun fixture() = RoleFixture.aRole()

            override fun createNewEntity(entity: Role): Role =
                fixture()
                    .withId(nextId())
                    .withName(entity.name)
                    .create()

            override fun updateEntity(entity: Role): Role = entity

            override fun clearAdditionalState() {
                nameIndex.clear()
            }
        }

    private val nameIndex = IndexManager<String, Long>()

    init {
        // 기본 역할들 초기화
        RoleFixture.defaultRoles().forEach { save(it) }
    }

    override fun save(role: Role): Role {
        checkUniqueConstraints(role)
        val savedRole = baseRepo.save(role)
        nameIndex.put(savedRole.name, savedRole.id)
        return savedRole
    }

    override fun findById(id: Long): Role = baseRepo.findById(id)

    override fun findByName(name: String): Role {
        val roleId =
            nameIndex.get(name)
                ?: throw EntityNotFoundException("해당 이름의 역할을 찾을 수 없습니다: $name")
        return findById(roleId)
    }

    override fun findAll(): List<Role> = baseRepo.findAll()

    override fun deleteById(id: Long) {
        val role = findById(id)
        delete(role)
    }

    override fun delete(role: Role) {
        baseRepo.delete(role)
        nameIndex.remove(role.name)
    }

    override fun existsById(id: Long): Boolean = baseRepo.existsById(id)

    override fun existsByName(name: String): Boolean = nameIndex.containsKey(name)

    override fun deleteAll() {
        baseRepo.deleteAll()
        nameIndex.clear()
    }

    private fun checkUniqueConstraints(role: Role) {
        nameIndex.get(role.name)?.let { existingId ->
            if (existingId != role.id) {
                throw DuplicateKeyException("이미 존재하는 역할 이름입니다: ${role.name}")
            }
        }
    }

    fun clear() {
        baseRepo.clear()
        nameIndex.clear()
    }

    fun saveAll(
        count: Int,
        customizer: RoleFixture.(Int) -> Unit,
    ): List<Role> =
        (1..count).map { index ->
            val role = RoleFixture.aRole().apply { customizer(index) }.create()
            save(role)
        }

    fun bulkInsert(
        count: Int,
        customizer: RoleFixture.(Int) -> Unit = {},
    ): List<Role> {
        val roles =
            (1..count).map { index ->
                val role =
                    RoleFixture
                        .aRole()
                        .withId(baseRepo.nextId())
                        .apply { customizer(index) }
                        .create()
                nameIndex.put(role.name, role.id)
                role
            }
        roles.forEach { baseRepo.save(it) }
        return roles
    }
}
