package com.example.jobstat.user.repository

import com.example.jobstat.core.base.Address
import com.example.jobstat.user.entity.Role
import com.example.jobstat.user.entity.RoleData
import com.example.jobstat.user.entity.User
import com.example.jobstat.user.entity.UserRole
import com.example.jobstat.utils.base.BatchOperationTestSupport
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("UserRepository 통합 테스트")
@Transactional  // 추가
class UserRepositoryIntegrationTest : BatchOperationTestSupport() {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        testUser = User.create(
            username = "테스트123",  // 한글+숫자로 된 3-10자 username
            email = "test@example.com",
            birthDate = LocalDate.now().minusYears(20)
        )
    }

    override fun cleanupTestData() {
        executeInTransaction {
            userRepository.findAll().forEach { user ->
                userRepository.delete(user)
            }
        }
    }

    @Nested
    @DisplayName("사용자 생성 테스트")
    inner class CreateUserTest {

        @Test
        @DisplayName("올바른 정보로 사용자를 생성할 수 있다")
        fun `유효한 사용자 정보로 생성시 성공한다`() {
            // Given
            val address = Address("12345", "서울시", "상세주소")
            testUser.updateAddress(address)

            // When
            val savedUser = userRepository.save(testUser)

            // Then
            assertEquals(testUser.username, savedUser.username)
            assertEquals(testUser.email, savedUser.email)
            assertEquals(address, savedUser.address)
            assertTrue(savedUser.isActive)
            assertTrue(savedUser.id > 0)
        }

        @Test
        @DisplayName("잘못된 username으로 사용자 생성시 실패한다")
        fun `잘못된 username으로 생성시 실패한다`() {
            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "te",  // 3자 미만
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20)
                )
            }

            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "test!@#$",  // 특수문자 포함
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20)
                )
            }

            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "verylongusername",  // 10자 초과
                    email = "test@example.com",
                    birthDate = LocalDate.now().minusYears(20)
                )
            }
        }
        @Test
        @DisplayName("잘못된 email로 사용자 생성시 실패한다")
        fun `잘못된 email로 생성시 실패한다`() {
            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "testuser123",
                    email = "invalid-email", // 잘못된 이메일
                    birthDate = LocalDate.now().minusYears(20)
                )
            }
        }

        @Test
        @DisplayName("미래 날짜의 생년월일로 사용자 생성시 실패한다")
        fun `미래 날짜의 생년월일로 생성시 실패한다`() {
            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "testuser123",
                    email = "test@example.com",
                    birthDate = LocalDate.now().plusDays(1) // 미래 날짜
                )
            }
        }
    }

    @Nested
    @DisplayName("사용자 정보 수정 테스트")
    inner class UpdateUserTest {

        @Test
        @DisplayName("이메일을 수정할 수 있다")
        fun `이메일 수정시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)
            val newEmail = "new@example.com"

            // When
            savedUser.updateEmail(newEmail)
            val updatedUser = userRepository.save(savedUser)

            // Then
            assertEquals(newEmail, updatedUser.email)
        }

        @Test
        @DisplayName("주소를 수정할 수 있다")
        fun `주소 수정시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)
            val newAddress = Address("54321", "부산시", "새로운 상세주소")

            // When
            savedUser.updateAddress(newAddress)
            val updatedUser = userRepository.save(savedUser)

            // Then
            assertEquals(newAddress, updatedUser.address)
        }

        @Test
        @DisplayName("사용자를 비활성화할 수 있다")
        fun `사용자 비활성화시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)

            // When
            savedUser.deactivate()
            val updatedUser = userRepository.save(savedUser)

            // Then
            assertFalse(updatedUser.isActive)
        }

        @Test
        @DisplayName("비활성화된 사용자를 다시 활성화할 수 있다")
        fun `비활성 사용자 활성화시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)
            savedUser.deactivate()
            userRepository.save(savedUser)

            // When
            savedUser.activate()
            val updatedUser = userRepository.save(savedUser)

            // Then
            assertTrue(updatedUser.isActive)
        }

        @Test
        @DisplayName("잘못된 이메일 형식으로 수정시 실패한다")
        fun `잘못된 이메일 형식으로 수정시 실패한다`() {
            // Given
            val savedUser = userRepository.save(testUser)

            // When & Then
            assertFailsWith<IllegalArgumentException> {
                savedUser.updateEmail("invalid-email")
            }
        }
    }

    @Nested
    @DisplayName("사용자 조회 테스트")
    inner class FindUserTest {

        @Test
        @DisplayName("ID로 사용자를 조회할 수 있다")
        fun `ID로 사용자 조회시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)

            // When
            val foundUser = userRepository.findById(savedUser.id)

            // Then
            assertEquals(savedUser.id, foundUser.id)
            assertEquals(savedUser.username, foundUser.username)
            assertEquals(savedUser.email, foundUser.email)
            assertEquals(savedUser.birthDate, foundUser.birthDate)
        }

        @Test
        @DisplayName("Username으로 사용자를 조회할 수 있다")
        fun `Username으로 사용자 조회시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)

            // When
            val foundUser = userRepository.findByUsername(savedUser.username)

            // Then
            assertEquals(savedUser.id, foundUser.id)
            assertEquals(savedUser.username, foundUser.username)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun `존재하지 않는 ID로 조회시 실패한다`() {
            // Given
            val nonExistentId = 999L

            // When & Then
            assertFailsWith<JpaObjectRetrievalFailureException> {
                userRepository.findById(nonExistentId)
            }
        }
    }

    @Nested
    @DisplayName("사용자 권한 테스트")
    inner class UserRoleTest {


        private lateinit var userRole: Role
        private lateinit var adminRole: Role
        private lateinit var managerRole: Role

        @BeforeEach
        fun setUpRoles() {
            userRole = roleRepository.save(RoleData.USER.toEntity())
            adminRole = roleRepository.save(RoleData.ADMIN.toEntity())
            managerRole = roleRepository.save(RoleData.MANAGER.toEntity())
        }

        @Test
        @DisplayName("사용자 생성시 기본 권한이 부여된다")
        fun `사용자 생성시 기본 권한 부여가 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)
            val userRole = UserRole(user = savedUser, role = roleRepository.findByName("USER"))

            // When
            savedUser.addRole(userRole)
            val updatedUser = userRepository.save(savedUser)

            // Then
            assertFalse(updatedUser.isAdmin())
            assertTrue(updatedUser.hasRole("USER"))
        }

        @Test
        @DisplayName("사용자에게 관리자 권한을 부여할 수 있다")
        fun `관리자 권한 부여시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)
            val adminRole = UserRole(user = savedUser, role = roleRepository.findByName("ADMIN"))

            // When
            savedUser.addRole(adminRole)
            val updatedUser = userRepository.save(savedUser)

            // Then
            assertTrue(updatedUser.isAdmin())
            assertTrue(updatedUser.hasRole("ADMIN"))
        }

        @Test
        @DisplayName("사용자의 권한을 삭제할 수 있다")
        fun `사용자 권한 삭제시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)
            val adminRole = roleRepository.findByName("ADMIN")
            val userRole = UserRole(user = savedUser, role = adminRole)

            // When
            savedUser.addRole(userRole)
            val userWithAdminRole = userRepository.save(savedUser)
            userWithAdminRole.removeRole(adminRole)  // UserRole 대신 Role을 전달
            val updatedUser = userRepository.save(userWithAdminRole)

            // Then
            assertFalse(updatedUser.isAdmin())
            assertFalse(updatedUser.hasRole("ADMIN"))
        }

        @Test
        @DisplayName("사용자에게 여러 권한을 부여할 수 있다")
        fun `다중 권한 부여시 성공한다`() {
            // Given
            val savedUser = userRepository.save(testUser)
            val adminRole = roleRepository.findByName("ADMIN")
            val managerRole = roleRepository.findByName("MANAGER")
            val adminUserRole = UserRole(user = savedUser, role = adminRole)
            val managerUserRole = UserRole(user = savedUser, role = managerRole)

            // When
            savedUser.addRole(adminUserRole)
            savedUser.addRole(managerUserRole)
            val updatedUser = userRepository.save(savedUser)

            // Then
            assertTrue(updatedUser.hasRole("ADMIN"))
            assertTrue(updatedUser.hasRole("MANAGER"))
            assertEquals(2, updatedUser.roles.size)
        }
    }
}