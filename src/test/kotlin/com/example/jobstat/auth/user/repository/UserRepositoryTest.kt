package com.example.jobstat.auth.user.repository

import com.example.jobstat.auth.user.entity.User
import com.example.jobstat.core.core_model.Address
import com.example.jobstat.utils.base.JpaIntegrationTestSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import java.time.LocalDate
import kotlin.test.*

@DisplayName("UserRepository 통합 테스트")
class UserRepositoryTest : JpaIntegrationTestSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        testUser =
            User.create(
                username = "테스트123",
                email = "test@example.com",
                password = "testPassword123!",
                birthDate = LocalDate.now().minusYears(20),
            )
    }

    override fun cleanupTestData() {
        executeInTransaction {
            userRepository.deleteAll()
        }
        flushAndClear()
    }

    @Nested
    @DisplayName("사용자 생성 테스트")
    inner class CreateUserTest {
        @Test
        @DisplayName("올바른 정보로 사용자를 생성할 수 있다")
        fun createUserWithValidInfoSuccess() {
            // Given
            val address = Address("12345", "서울시", "상세주소")
            testUser.updateAddress(address)

            // When
            // DB 반영 + 영속성 컨텍스트 초기화 후 detached 엔티티 반환
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // Then
            // 새로 DB에서 조회해서 검증
            val foundUser = userRepository.findById(savedUser.id)
            assertEquals("테스트123", foundUser.username)
            assertEquals("test@example.com", foundUser.email)
            assertEquals("testPassword123!", foundUser.password)
            assertEquals(address, foundUser.address)
            assertTrue(foundUser.isActive)
            assertTrue(foundUser.id > 0)
        }

        @Test
        @DisplayName("잘못된 username으로 사용자 생성시 실패한다")
        fun createUserWithInvalidUsernameFails() {
            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "te", // 3자 미만
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            }

            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "테스트!@#$", // 특수문자 포함
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            }

            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "너무긴사용자이름너무긴사용자이름너무긴사용자이름", // 10자 초과
                    email = "test@example.com",
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            }
        }

        @Test
        @DisplayName("잘못된 email로 사용자 생성시 실패한다")
        fun createUserWithInvalidEmailFails() {
            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "테스트사용자123",
                    email = "잘못된-이메일", // 잘못된 이메일
                    password = "testpassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            }
        }

        @Test
        @DisplayName("미래 날짜의 생년월일로 사용자 생성시 실패한다")
        fun createUserWithFutureBirthDateFails() {
            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "testuser123",
                    email = "test@example.com",
                    password = "testPassword123!",
                    birthDate = LocalDate.now().plusDays(1), // 미래 날짜
                )
            }
        }

        @Test
        @DisplayName("빈 패스워드로 사용자 생성시 실패한다")
        fun createUserWithEmptyPasswordFails() {
            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "testuser123",
                    email = "test@example.com",
                    password = "", // 빈 패스워드
                    birthDate = LocalDate.now().minusYears(20),
                )
            }

            assertFailsWith<IllegalArgumentException> {
                User.create(
                    username = "testuser123",
                    email = "test@example.com",
                    password = "   ", // 공백 패스워드
                    birthDate = LocalDate.now().minusYears(20),
                )
            }
        }
    }

    @Nested
    @DisplayName("사용자 정보 수정 테스트")
    inner class UpdateUserTest {
        @Test
        @DisplayName("이메일을 수정할 수 있다")
        fun updateEmailSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findById(savedUser.id)
            foundUser.updateEmail("new@example.com")
            val updatedUser = saveAndGetAfterCommit(foundUser) { userRepository.save(it) }

            // Then
            val reloadedUser = userRepository.findById(updatedUser.id)
            assertEquals("new@example.com", reloadedUser.email)
        }

        @Test
        @DisplayName("주소를 수정할 수 있다")
        fun updateAddressSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findById(savedUser.id)
            val newAddress = Address("54321", "부산시", "새로운 상세주소")
            foundUser.updateAddress(newAddress)
            val updatedUser = saveAndGetAfterCommit(foundUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(updatedUser.id)
            assertEquals(newAddress, reloaded.address)
        }

        @Test
        @DisplayName("패스워드를 수정할 수 있다")
        fun updatePasswordSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findById(savedUser.id)
            val newPassword = "newPassword123!"
            foundUser.updatePassword(newPassword)
            val updatedUser = saveAndGetAfterCommit(foundUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(updatedUser.id)
            assertEquals(newPassword, reloaded.password)
        }

        @Test
        @DisplayName("사용자를 비활성화할 수 있다")
        fun disableAccountUserSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findById(savedUser.id)
            foundUser.disableAccount()
            val updatedUser = saveAndGetAfterCommit(foundUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(updatedUser.id)
            assertFalse(reloaded.isActive)
        }

        @Test
        @DisplayName("비활성화된 사용자를 다시 활성화할 수 있다")
        fun activateInactiveUserSuccess() {
            // Given
            testUser.disableAccount()
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findById(savedUser.id)
            foundUser.enableAccount()
            val updatedUser = saveAndGetAfterCommit(foundUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(updatedUser.id)
            assertTrue(reloaded.isActive)
        }

        @Test
        @DisplayName("잘못된 이메일 형식으로 수정시 실패한다")
        fun updateWithInvalidEmailFormatFails() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When & Then
            val foundUser = userRepository.findById(savedUser.id)
            assertFailsWith<IllegalArgumentException> {
                foundUser.updateEmail("invalid-email")
            }
        }
    }

    @Nested
    @DisplayName("사용자 삭제 테스트")
    inner class DeleteUserTest {
        @Test
        @DisplayName("사용자를 soft delete할 수 있다")
        fun softDeleteUserSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findById(savedUser.id)
            foundUser.delete()
            val deletedUser = saveAndGetAfterCommit(foundUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(deletedUser.id)
            assertTrue(reloaded.isDeleted)
        }

        @Test
        @DisplayName("삭제된 사용자를 복구할 수 있다")
        fun restoreDeletedUserSuccess() {
            // Given
            testUser.delete()
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findById(savedUser.id)
            foundUser.restore()
            val restoredUser = saveAndGetAfterCommit(foundUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(restoredUser.id)
            assertFalse(reloaded.isDeleted)
            assertNull(reloaded.deletedAt)
        }
    }

    @Nested
    @DisplayName("사용자 조회 테스트")
    inner class FindUserTest {
        @Test
        @DisplayName("Username으로 사용자를 조회할 수 있다")
        fun findUserByUsernameSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findByUsername(savedUser.username)

            // Then
            assertEquals(savedUser.id, foundUser.id)
            assertEquals(savedUser.username, foundUser.username)
        }

        @Test
        @DisplayName("이메일로 사용자를 조회할 수 있다")
        fun findUserByEmailSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findByEmail(savedUser.email)

            // Then
            assertEquals(savedUser.id, foundUser.id)
            assertEquals(savedUser.email, foundUser.email)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun findUserByNonExistentIdFails() {
            // 예외가 flush 시점이 아니라, findById 내부(`getReference` 등)에서 발생할 수 있음
            assertFailsWith<JpaObjectRetrievalFailureException> {
                userRepository.findById(999L)
            }
        }
    }

    @Nested
    @DisplayName("사용자 존재 여부 확인 테스트")
    inner class ExistsUserTest {
        @Test
        @DisplayName("ID로 사용자 존재 여부를 확인할 수 있다")
        fun checkUserExistsByIdSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When & Then
            assertTrue(userRepository.existsById(savedUser.id))
            assertFalse(userRepository.existsById(999L))
        }

        @Test
        @DisplayName("Username으로 사용자 존재 여부를 확인할 수 있다")
        fun checkUserExistsByUsernameSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When & Then
            assertTrue(userRepository.existsByUsername(savedUser.username))
            assertFalse(userRepository.existsByUsername("존재하지않음"))
        }

        @Test
        @DisplayName("이메일로 사용자 존재 여부를 확인할 수 있다")
        fun checkUserExistsByEmailSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When & Then
            assertTrue(userRepository.existsByEmail(savedUser.email))
            assertFalse(userRepository.existsByEmail("존재하지않음@example.com"))
        }
    }

    @Nested
    @DisplayName("사용자 이메일 조회 테스트")
    inner class FindUserByEmailTest {
        @Test
        @DisplayName("이메일로 사용자를 조회할 수 있다")
        fun findUserByEmailSuccess() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            val foundUser = userRepository.findByEmail(savedUser.email)

            // Then
            assertEquals(savedUser.id, foundUser.id)
            assertEquals(savedUser.email, foundUser.email)
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회시 예외가 발생한다")
        fun findUserByNonExistentEmailFails() {
            assertFailsWith<JpaObjectRetrievalFailureException> {
                userRepository.findByEmail("nonexistent@example.com")
            }
        }
    }

    @Nested
    @DisplayName("사용자 패스워드 테스트")
    inner class UserPasswordTest {
        @Test
        @DisplayName("빈 패스워드로 업데이트시 실패한다")
        fun updateWithEmptyPasswordFails() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When & Then
            assertFailsWith<IllegalArgumentException> {
                savedUser.updatePassword("")
            }
            assertFailsWith<IllegalArgumentException> {
                savedUser.updatePassword("   ")
            }
        }
    }

    @Nested
    @DisplayName("사용자 Soft Delete 테스트")
    inner class UserSoftDeleteTest {
        @Test
        @DisplayName("삭제된 사용자는 isDeleted가 true이다")
        fun checkDeletedUserIsDeletedTrue() {
            // Given
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            savedUser.delete()
            val deletedUser = saveAndGetAfterCommit(savedUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(deletedUser.id)
            assertTrue(reloaded.isDeleted)
        }

        @Test
        @DisplayName("복구된 사용자는 isDeleted가 false이다")
        fun checkRestoredUserIsDeletedFalse() {
            // Given
            testUser.delete()
            val savedUser = saveAndGetAfterCommit(testUser) { userRepository.save(it) }

            // When
            savedUser.restore()
            val restoredUser = saveAndGetAfterCommit(savedUser) { userRepository.save(it) }

            // Then
            val reloaded = userRepository.findById(restoredUser.id)
            assertFalse(reloaded.isDeleted)
            assertNull(reloaded.deletedAt)
        }
    }
}
