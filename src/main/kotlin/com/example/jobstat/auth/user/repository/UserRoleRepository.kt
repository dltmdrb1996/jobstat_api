package com.example.jobstat.auth.user.repository

import com.example.jobstat.auth.user.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
internal interface UserRoleRepository : JpaRepository<UserRole, Long> {
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.role.id = :roleId")
    fun deleteAllByRoleId(
        @Param("roleId") roleId: Long,
    )
}
