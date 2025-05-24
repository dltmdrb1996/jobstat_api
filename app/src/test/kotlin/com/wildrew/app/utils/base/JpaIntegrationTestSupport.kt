package com.wildrew.app.utils.base

import com.wildrew.app.utils.config.BatchOperationTestSupport
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional

@Transactional
@Rollback
abstract class JpaIntegrationTestSupport : BatchOperationTestSupport() {
    @PersistenceContext
    protected lateinit var entityManager: EntityManager

    protected fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    protected fun <T : Any> saveAndFlush(
        entity: T,
        save: (T) -> T,
    ): T {
        val savedEntity = save(entity)
        entityManager.flush()
        return savedEntity
    }

    protected fun <T : Any> saveAndGetAfterCommit(
        entity: T,
        save: (T) -> T,
    ): T {
        val savedEntity = save(entity)
        flushAndClear()
        return savedEntity
    }
}
