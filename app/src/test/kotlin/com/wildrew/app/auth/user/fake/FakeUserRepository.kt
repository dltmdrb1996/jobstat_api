package com.wildrew.app.auth.user.fake

import com.wildrew.jobstat.auth.user.entity.User
import com.wildrew.jobstat.auth.user.repository.UserRepository
import com.wildrew.jobstat.utils.IndexManager
import com.wildrew.jobstat.utils.base.BaseFakeRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException
import java.time.LocalDate

/**
 * User 전용 FakeRepository 구현
 * - 소프트 삭제, 활성화 상태, 사용자명/이메일 중복 인덱싱 등 처리
 */
internal class FakeUserRepository : UserRepository {
    private val baseRepo =
        object : BaseFakeRepository<User, UserFixture>() {
            override fun fixture() = UserFixture.aUser()

            /**
             * 새로 저장하는 경우: ID가 없으면 nextId()로 부여.
             * 이후 엔티티는 그대로 반환하여, 기존 설정(roles, isActive 등)이 유지되도록 함
             */
            override fun createNewEntity(entity: User): User {
                if (!isValidId(entity.id)) {
                    setEntityId(entity, nextId())
                }
                return entity
            }

            /**
             * 이미 ID가 있는 엔티티는 그대로 업데이트
             * (엔티티의 변경사항을 보존)
             */
            override fun updateEntity(entity: User): User = entity

            /**
             * 추가적으로 관리하던 인덱스를 여기서 초기화할 수 있음
             */
            override fun clearAdditionalState() {
                usernameIndex.clear()
                emailIndex.clear()
            }
        }

    private val usernameIndex = IndexManager<String, Long>()
    private val emailIndex = IndexManager<String, Long>()

    /**
     * 저장 시 유니크 제약(중복 username/email) 검사 후 baseRepo에 저장
     * - softDelete된 엔티티라면 인덱스에서 제거
     * - active 상태라면 인덱스에 등록
     */
    override fun save(user: User): User {
        checkUniqueConstraints(user)
        val savedUser = baseRepo.save(user)

        if (savedUser.isDeleted) {
            // soft-delete된 유저는 인덱스에서 제거
            usernameIndex.remove(savedUser.username)
            emailIndex.remove(savedUser.email)
        } else {
            // 중복 아님이 확인되었으니 인덱스 등록
            usernameIndex.put(savedUser.username, savedUser.id)
            emailIndex.put(savedUser.email, savedUser.id)
        }

        return savedUser
    }

    override fun findById(id: Long): User {
        val user =
            baseRepo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("사용자를 찾을 수 없습니다 (id=$id)")

        // 소프트 삭제된 사용자는 찾을 수 없다고 간주
        if (user.isDeleted) {
            throw EntityNotFoundException("사용자가 삭제되었습니다 (id=$id)")
        }

        return user
    }

    override fun findByUsername(username: String): User {
        val userId =
            usernameIndex.get(username)
                ?: throw EntityNotFoundException("해당 사용자명의 사용자를 찾을 수 없습니다: $username")
        return findById(userId)
    }

    override fun findByEmail(email: String): User {
        val userId =
            emailIndex.get(email)
                ?: throw EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: $email")
        return findById(userId)
    }

    override fun findByIdWithRoles(id: Long): User {
        // 여기서는 단순히 findById 재활용
        return findById(id)
    }

    /**
     * Soft-delete가 아닌 유저만 반환
     */
    override fun findAll(): List<User> = baseRepo.findAll().filter { !it.isDeleted }

    override fun deleteById(id: Long) {
        val user = baseRepo.findByIdOrNull(id) ?: return
        delete(user)
    }

    /**
     * soft-delete 로직:
     * - user.delete()로 isDeleted=true
     * - 테스트가 기대하는 "삭제 시 isActive=false"도 함께 처리
     * - 인덱스에서 제거
     * - store에는 남겨두어 findAll()에서 제외되도록 (isDeleted=true 이므로)
     */
    override fun delete(user: User) {
        user.delete()
        user.disableAccount()

        usernameIndex.remove(user.username)
        emailIndex.remove(user.email)

        // 다시 store에 저장(상태 갱신)
        baseRepo.save(user)
    }

    override fun existsById(id: Long): Boolean {
        val user = baseRepo.findByIdOrNull(id) ?: return false
        return !user.isDeleted
    }

    override fun existsByUsername(username: String): Boolean {
        val userId = usernameIndex.get(username) ?: return false
        return existsById(userId)
    }

    override fun existsByEmail(email: String): Boolean {
        val userId = emailIndex.get(email) ?: return false
        return existsById(userId)
    }

    override fun deleteAll() {
        baseRepo.deleteAll()
        usernameIndex.clear()
        emailIndex.clear()
    }

    /**
     * username/email 중복 검사
     * - 이미 다른 유저(softDelete=false)가 동일한 username/email을 쓰고 있으면 예외
     */
    private fun checkUniqueConstraints(user: User) {
        usernameIndex.get(user.username)?.let { existingId ->
            if (existingId != user.id) {
                val existingUser = baseRepo.findByIdOrNull(existingId)
                if (existingUser != null && !existingUser.isDeleted) {
                    throw DuplicateKeyException("이미 존재하는 사용자명입니다: ${user.username}")
                }
            }
        }

        emailIndex.get(user.email)?.let { existingId ->
            if (existingId != user.id) {
                val existingUser = baseRepo.findByIdOrNull(existingId)
                if (existingUser != null && !existingUser.isDeleted) {
                    throw DuplicateKeyException("이미 존재하는 이메일입니다: ${user.email}")
                }
            }
        }
    }

    /**
     * 테스트 환경 리셋용
     */
    fun clear() {
        baseRepo.clear()
        usernameIndex.clear()
        emailIndex.clear()
    }

    /**
     * 대량 생성 편의 메서드 (직접 커스터마이징 가능)
     */
    fun saveAll(
        count: Int,
        customizer: UserFixture.(Int) -> Unit,
    ): List<User> = baseRepo.saveAll(count, customizer)

    fun bulkInsert(
        count: Int,
        customizer: UserFixture.(Int) -> Unit = {},
    ): List<User> {
        val users =
            (1..count).map { index ->
                val user =
                    UserFixture
                        .aUser()
                        .apply { customizer(index) }
                        .create()
                user
            }
        users.forEach { baseRepo.save(it) }

        // 인덱스 갱신 (soft-delete=false 가정)
        users.forEach {
            usernameIndex.put(it.username, it.id)
            emailIndex.put(it.email, it.id)
        }
        return users
    }

    /**
     * 조건부 필터 조회
     */
    fun findAllByCondition(predicate: (User) -> Boolean): List<User> = baseRepo.findAll().filter(predicate)

    fun findByBirthDateBefore(date: LocalDate): List<User> = findAllByCondition { !it.isDeleted && it.birthDate.isBefore(date) }

    fun findByIsActive(isActive: Boolean): List<User> = findAllByCondition { !it.isDeleted && it.isActive == isActive }

    fun findByAddressIsNotNull(): List<User> = findAllByCondition { !it.isDeleted && it.address != null }

    fun findAllWithRole(roleName: String): List<User> = findAllByCondition { !it.isDeleted && it.hasRole(roleName) }
}
