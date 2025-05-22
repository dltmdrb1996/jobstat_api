package com.wildrew.app.auth.user.repository

import com.wildrew.jobstat.auth.user.entity.*
import com.wildrew.jobstat.utils.base.JpaIntegrationTestSupport
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import java.time.LocalDate
import kotlin.test.*

@DisplayName("RoleRepository 통합 테스트")
class RoleRepositoryTest : JpaIntegrationTestSupport() {
    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testRole: Role

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        testRole = Role.create("TEST_ROLE")
    }

    /**
     * 테스트 시작 전 공통적으로 데이터를 청소하는 로직.
     * flushAndClear()는 마지막에 한 번만 호출해서 DB와 영속성 컨텍스트를 완전히 비웁니다.
     */
    override fun cleanupTestData() {
        executeInTransaction {
            userRepository.findAll().forEach { user ->
                user.clearRoles()
                userRepository.save(user)
            }
            userRepository.deleteAll()

            roleRepository.findAll().forEach { role ->
                roleRepository.save(role)
            }
            roleRepository.deleteAll()
        }
        flushAndClear()
    }

    @Nested
    @DisplayName("역할 생성 테스트")
    inner class CreateRoleTest {
        @Test
        @DisplayName("새로운 역할을 생성할 수 있다")
        fun createRoleSuccess() {
            // When
            val savedRole = roleRepository.save(testRole)

            // Then
            assertEquals(testRole.name, savedRole.name)
            assertTrue(savedRole.id > 0)
        }

        @Test
        @DisplayName("중복된 이름으로 역할을 생성할 수 없다 (Unique 제약 확인)")
        fun createRoleWithDuplicateNameFails() {
            // Given: 첫 번째 역할 저장 및 DB 반영 확인
            // testRole 변수가 미리 정의되어 있다고 가정
            val existingRoleName = testRole.name
            saveAndGetAfterCommit(Role.create(existingRoleName)) { roleRepository.save(it) }

            // When & Then: 동일한 이름으로 두 번째 역할 저장을 시도하고 즉시 flush
            assertFailsWith<ConstraintViolationException> {
                // 동일한 이름으로 새 Role 객체 생성 후 저장 시도
                roleRepository.save(Role.create(existingRoleName))
                // save 호출 후 즉시 flush하여 DB에 INSERT 시도 -> Unique 제약 위반 유도
                flushAndClear() // 또는 entityManager.flush() 만 호출해도 됨
            }
        }
    }

    @Nested
    @DisplayName("역할 조회 테스트")
    inner class FindRoleTest {
        @Test
        @DisplayName("ID로 역할을 조회할 수 있다")
        fun findRoleByIdSuccess() {
            // Given
            val savedRole = roleRepository.save(testRole)

            // When
            val foundRole = roleRepository.findById(savedRole.id)

            // Then
            assertEquals(savedRole.id, foundRole.id)
            assertEquals(savedRole.name, foundRole.name)
        }

        @Test
        @DisplayName("이름으로 역할을 조회할 수 있다")
        fun findRoleByNameSuccess() {
            // Given
            val savedRole = roleRepository.save(testRole)

            // When
            val foundRole = roleRepository.findByName(savedRole.name)

            // Then
            assertEquals(savedRole.id, foundRole.id)
            assertEquals(savedRole.name, foundRole.name)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun findRoleByNonExistentIdFails() {
            // userRepository에 없는 ID(999L) 조회 -> 예외 발생 기대
            assertFailsWith<JpaObjectRetrievalFailureException> {
                userRepository.findById(999L)
            }
        }

        @Test
        @DisplayName("존재하지 않는 이름으로 조회시 예외가 발생한다")
        fun findRoleByNonExistentNameFails() {
            assertFailsWith<JpaObjectRetrievalFailureException> {
                roleRepository.findByName("존재하지_않는_역할")
            }
        }
    }

    @Nested
    @DisplayName("역할 삭제 테스트")
    inner class DeleteRoleTest {
        @Test
        @DisplayName("역할을 삭제할 수 있다")
        fun deleteRoleSuccess() {
            // Given
            val savedRole = saveAndGetAfterCommit(testRole) { roleRepository.save(it) }

            // When
            roleRepository.delete(savedRole)
            flushAndClear() // 실제 삭제 DB 반영

            // Then
            assertFalse(roleRepository.existsById(savedRole.id))
        }

        @Test
        @DisplayName("사용자에게 할당된 역할도 삭제할 수 있다")
        fun deleteAssignedRoleSuccess() {
            // Given
            val savedRole = saveAndGetAfterCommit(testRole) { roleRepository.save(it) }
            val user =
                User.create(
                    username = "테스트123",
                    email = "test@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            val savedUser = saveAndGetAfterCommit(user) { userRepository.save(it) }

            // UserRole 생성 (양방향 연관관계)
            val useRole = UserRole.create(user = savedUser, role = savedRole)
            savedUser.assignRole(useRole)
            savedRole.assignRole(useRole)
            flushAndClear()

            // When
            roleRepository.delete(savedRole)
            flushAndClear() // DB에서 실제 삭제

            // Then
            assertFalse(roleRepository.existsById(savedRole.id))
        }
    }

    @Nested
    @DisplayName("역할 존재 여부 확인 테스트")
    inner class ExistsRoleTest {
        @Test
        @DisplayName("ID로 역할 존재 여부를 확인할 수 있다")
        fun checkRoleExistsByIdSuccess() {
            // Given
            val savedRole = roleRepository.save(testRole)

            // When & Then
            assertTrue(roleRepository.existsById(savedRole.id))
            assertFalse(roleRepository.existsById(999L))
        }

        @Test
        @DisplayName("이름으로 역할 존재 여부를 확인할 수 있다")
        fun checkRoleExistsByNameSuccess() {
            // 준비
            val savedRole = roleRepository.save(testRole)

            // 실행 & 검증
            assertTrue(roleRepository.existsByName(savedRole.name))
            assertFalse(roleRepository.existsByName("존재하지_않는_역할"))
        }
    }
}
