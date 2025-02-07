package com.example.jobstat.auth.user.repository

import com.example.jobstat.auth.user.entity.Role

internal interface RoleRepository {
    fun save(role: Role): Role

    fun findById(id: Long): Role

    fun findByName(name: String): Role

    fun findAll(): List<Role>

    fun deleteById(id: Long)

    fun delete(role: Role)

    fun existsById(id: Long): Boolean

    fun existsByName(name: String): Boolean

    fun deleteAll()
}
