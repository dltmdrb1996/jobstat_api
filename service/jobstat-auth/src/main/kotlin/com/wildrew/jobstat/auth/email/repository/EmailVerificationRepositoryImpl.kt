package com.wildrew.jobstat.auth.email.repository

import com.wildrew.jobstat.auth.email.entity.EmailVerification
import com.wildrew.jobstat.core.core_jpa_base.orThrowNotFound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

interface EmailVerificationJpaRepository : JpaRepository<EmailVerification, Long> {
    fun findTopByEmailOrderByIdDesc(email: String): EmailVerification?

    fun existsByEmailAndCode(
        email: String,
        code: String,
    ): Boolean

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    fun deleteAllExpired(now: LocalDateTime)
}

@Repository
class EmailVerificationRepositoryImpl(
    private val emailVerificationJpaRepository: EmailVerificationJpaRepository,
) : EmailVerificationRepository {
    override fun save(emailVerification: EmailVerification): EmailVerification = emailVerificationJpaRepository.save(emailVerification)

    override fun findById(id: Long): EmailVerification = emailVerificationJpaRepository.findById(id).orThrowNotFound("id", id)

    override fun findLatestByEmail(email: String): EmailVerification? = emailVerificationJpaRepository.findTopByEmailOrderByIdDesc(email)

    override fun existsByEmailAndCode(
        email: String,
        code: String,
    ): Boolean = emailVerificationJpaRepository.existsByEmailAndCode(email, code)

    override fun delete(emailVerification: EmailVerification) = emailVerificationJpaRepository.delete(emailVerification)

    override fun deleteExpired() = emailVerificationJpaRepository.deleteAllExpired(LocalDateTime.now())
}
