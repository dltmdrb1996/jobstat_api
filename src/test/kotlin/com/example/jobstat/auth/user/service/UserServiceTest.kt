package com.example.jobstat.auth.user.service

import com.example.jobstat.auth.user.fake.FakeRoleRepository
import com.example.jobstat.auth.user.fake.FakeUserRepository
import com.example.jobstat.auth.user.fake.UserFixture
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.*

@DisplayName("UserService 테스트")
class UserServiceTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var roleRepository: FakeRoleRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        roleRepository = FakeRoleRepository()
        userService = UserServiceImpl(userRepository, roleRepository)
    }

    //region 사용자 생성
    @Nested
    @DisplayName("사용자 생성")
    inner class CreateUser {
        @Test
        @DisplayName("유효한 정보로 사용자를 생성할 수 있다")
        fun createValidUser() {
            // given
            val user =
                UserFixture
                    .aUser()
                    .withUsername("testUser123")
                    .withEmail("test@example.com")
                    .withPassword("validPass123!")
                    .withBirthDate(LocalDate.now().minusYears(20))
                    .create()

            // when
            val createdUser =
                userService.createUser(
                    username = user.username,
                    email = user.email,
                    password = user.password,
                    birthDate = user.birthDate,
                )

            // then
            assertEquals(user.username, createdUser.username)
            assertEquals(user.email, createdUser.email)
            assertEquals(user.password, createdUser.password)
            assertEquals(user.birthDate, createdUser.birthDate)
            assertTrue(createdUser.isActive)
        }

        @Test
        @DisplayName("중복된 이메일로 사용자를 생성할 수 없다")
        fun cannotCreateUserWithDuplicateEmail() {
            // given
            val user =
                userService.createUser(
                    username = "uniqueUsername",
                    email = "dup@example.com",
                    password = "somePass123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when & then
            val ex =
                assertFailsWith<AppException> {
                    userService.createUser(
                        username = "otherUsername",
                        email = user.email, // 중복
                        password = "anotherPass123!",
                        birthDate = user.birthDate,
                    )
                }
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.errorCode)
        }

        @Test
        @DisplayName("중복된 사용자명으로 사용자를 생성할 수 없다")
        fun cannotCreateUserWithDuplicateUsername() {
            // given
            val user =
                userService.createUser(
                    username = "dupUsername",
                    email = "test@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when & then
            val ex =
                assertFailsWith<AppException> {
                    userService.createUser(
                        username = user.username, // 중복
                        email = "diff@example.com",
                        password = "otherPass123!",
                        birthDate = user.birthDate,
                    )
                }
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, ex.errorCode)
        }

        @Test
        @DisplayName("빈 패스워드로 사용자를 생성할 수 없다")
        fun cannotCreateUserWithEmptyPassword() {
            val ex =
                assertFailsWith<IllegalArgumentException> {
                    userService.createUser(
                        username = "testUser",
                        email = "test@example.com",
                        password = "",
                        birthDate = LocalDate.now().minusYears(20),
                    )
                }
            assertTrue(ex.message!!.contains("패스워드는 필수 값입니다"))
        }
    }
    //endregion

    //region 사용자 조회
    @Nested
    @DisplayName("사용자 조회")
    inner class GetUser {
        @Test
        @DisplayName("ID로 사용자를 조회할 수 있다")
        fun getUserById() {
            // given
            val savedUser =
                userService.createUser(
                    username = "getById123",
                    email = "getid@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            val savedUserId = savedUser.id

            // when
            val foundUser = userService.getUserById(savedUserId)

            // then
            assertEquals(savedUserId, foundUser.id)
            assertEquals(savedUser.username, foundUser.username)
        }

        @Test
        @DisplayName("삭제된 ID로 조회하면 EntityNotFoundException 발생")
        fun throwsExceptionWhenUserDeleted() {
            // given
            val user =
                userService.createUser(
                    username = "deleteCheck",
                    email = "deleteCheck@example.com",
                    password = "anypass123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            userService.deleteUser(user.id)

            // when & then
            assertFailsWith<EntityNotFoundException> {
                userService.getUserById(user.id)
            }
        }

        @Test
        @DisplayName("사용자명으로 사용자를 조회할 수 있다")
        fun getUserByUsername() {
            // given
            val savedUser =
                userService.createUser(
                    username = "nameCheck123",
                    email = "nameCheck@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when
            val foundUser = userService.getUserByUsername(savedUser.username)

            // then
            assertEquals(savedUser.id, foundUser.id)
            assertEquals(savedUser.username, foundUser.username)
        }

        @Test
        @DisplayName("이메일로 사용자를 조회할 수 있다")
        fun getUserByEmail() {
            // given
            val savedUser =
                userService.createUser(
                    username = "emailCheck123",
                    email = "email-check@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when
            val foundUser = userService.getUserByEmail(savedUser.email)

            // then
            assertEquals(savedUser.id, foundUser.id)
            assertEquals(savedUser.email, foundUser.email)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun throwsExceptionWhenUserNotFound() {
            assertFailsWith<EntityNotFoundException> {
                userService.getUserById(999L)
            }
        }

        @Test
        @DisplayName("모든 사용자를 조회할 수 있다")
        fun getAllUsers() {
            // given
            val userCount = 5
            repeat(userCount) { idx ->
                userService.createUser(
                    username = "someUser$idx",
                    email = "someUser$idx@example.com",
                    password = "somePass$idx!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            }

            // when
            val users = userService.getAllUsers()

            // then
            assertEquals(userCount, users.size)
        }
    }
    //endregion

    //region 사용자 삭제
    @Nested
    @DisplayName("사용자 삭제")
    inner class DeleteUserTest {
        @Test
        @DisplayName("사용자를 하면 다시 조회가 불가능하다")
        fun deleteRemovesUserFromFind() {
            // given
            val user =
                userService.createUser(
                    username = "deleteTest123",
                    email = "deleteTest@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            // when
            userService.deleteUser(user.id)

            // then
            assertFailsWith<EntityNotFoundException> {
                userService.getUserById(user.id)
            }
        }

        @Test
        @DisplayName("삭제된 사용자와 동일한 username/email로 새 사용자를 생성할 수 있다")
        fun createUserAfterDeletingSameUsernameAndEmail() {
            // given
            val oldUser =
                userService.createUser(
                    username = "duplicate123",
                    email = "dup@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            val oldUserId = oldUser.id
            userService.deleteUser(oldUserId)

            // when
            val newUser =
                userService.createUser(
                    username = "duplicate123",
                    email = "dup@example.com",
                    password = "newPassword123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // then
            // 새로 생성된 유저는 ID가 달라야 함
            assertNotEquals(oldUserId, newUser.id)

            // oldUser는 조회 안 됨
            assertFailsWith<EntityNotFoundException> {
                userService.getUserById(oldUserId)
            }
        }
    }
    //endregion

    //region 사용자 정보 수정
    @Nested
    @DisplayName("사용자 정보 수정")
    inner class UpdateUser {
        @Test
        @DisplayName("패스워드를 수정할 수 있다")
        fun updatePassword() {
            // given
            val user =
                userService.createUser(
                    username = "pwTest",
                    email = "pwTest@example.com",
                    password = "oldPass123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when
            val newPassword = "newPass123!"
            userService.updateUserPassword(user.id, newPassword)

            // then
            val updatedUser = userService.getUserById(user.id)
            assertEquals(newPassword, updatedUser.password)
        }

        @Test
        @DisplayName("사용자를 비활성화할 수 있다")
        fun disableAccountUser() {
            // given
            val user =
                userService.createUser(
                    username = "activeTest123",
                    email = "active@example.com",
                    password = "somePass!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            assertTrue(userService.getUserById(user.id).isActive)

            // when
            userService.disableUser(user.id)

            // then
            val deactivatedUser = userService.getUserById(user.id)
            assertFalse(deactivatedUser.isActive)
        }

        @Test
        @DisplayName("비활성화된 사용자를 활성화할 수 있다")
        fun activateUser() {
            // given
            val user =
                userService.createUser(
                    username = "inactiveUser123",
                    email = "inact@example.com",
                    password = "inactivePass!",
                    birthDate = LocalDate.now().minusYears(20),
                )
            // 먼저 비활성화
            userService.disableUser(user.id)

            // when
            userService.enableUser(user.id)

            // then
            val activatedUser = userService.getUserById(user.id)
            assertTrue(activatedUser.isActive)
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 업데이트 시 실패한다")
        fun updateNonExistentUserFails() {
            assertFailsWith<EntityNotFoundException> {
                userService.updateUser(
                    mapOf(
                        "id" to 9999L,
                        "email" to "newEmail@example.com",
                    ),
                )
            }
        }

        @Test
        @DisplayName("업데이트 시 지원되지 않는 키는 무시되지만, isActive 등은 반영된다")
        fun updateUserWithUnsupportedKeyIsIgnored() {
            // Given
            val savedUser =
                userService.createUser(
                    username = "someuser",
                    email = "some@example.com",
                    password = "Pass123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // When
            userService.updateUser(
                mapOf(
                    "id" to savedUser.id,
                    "unknownField" to "SomeValue",
                    "isActive" to false,
                ),
            )

            // Then
            val updated = userService.getUserById(savedUser.id)
            // unknownField는 무시, isActive=false는 적용
            assertFalse(updated.isActive)
        }

        @Test
        @DisplayName("업데이트 시 비밀번호가 빈 문자열이면 예외 발생")
        fun updateUserWithEmptyPasswordFails() {
            // Given
            val savedUser =
                userService.createUser(
                    username = "pwuser",
                    email = "pwuser@example.com",
                    password = "OldPass123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // When & Then
            val ex =
                assertFailsWith<IllegalArgumentException> {
                    userService.updateUser(
                        mapOf(
                            "id" to savedUser.id,
                            "password" to "", // 빈 비밀번호
                        ),
                    )
                }
            assertTrue(ex.message!!.contains("패스워드는 필수 값입니다"))
        }
    }
    //endregion

    //region 사용자 정보 검증 (가용성, 활성화 상태 등)
    @Nested
    @DisplayName("사용자 정보 검증")
    inner class ValidateUserInfo {
        @Test
        @DisplayName("사용 가능한 사용자명인지 확인할 수 있다")
        fun checkUsernameAvailability() {
            // given
            val user =
                userService.createUser(
                    username = "existingName123",
                    email = "existing@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when & then
            assertTrue(userService.validateUsername("newName123"))
            assertFalse(userService.validateUsername("existingName123"))
        }

        @Test
        @DisplayName("사용 가능한 이메일인지 확인할 수 있다")
        fun checkEmailAvailability() {
            // given
            val user =
                userService.createUser(
                    username = "existingUserX",
                    email = "existing@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when & then
            assertTrue(userService.validateEmail("new@example.com"))
            assertFalse(userService.validateEmail("existing@example.com"))
        }

        @Test
        @DisplayName("사용자 활성화 상태를 확인할 수 있다")
        fun checkUserActivation() {
            // given
            val user =
                userService.createUser(
                    username = "checkActiveUser",
                    email = "active@example.com",
                    password = "password123!",
                    birthDate = LocalDate.now().minusYears(20),
                )

            // when & then
            assertTrue(userService.isAccountEnabled(user.id))
        }
    }
    //endregion

    //region 사용자 활성화 관리
    @Nested
    @DisplayName("사용자 활성화/비활성화 관리")
    inner class UserActivation {
        @Test
        @DisplayName("사용자를 비활성화할 수 있다")
        fun disableAccountUser() {
            // given
            val user =
                userService.createUser(
                    username = "activeUser",
                    email = "active@ex.com",
                    password = "somepass123",
                    birthDate = LocalDate.now().minusYears(20),
                )
            assertTrue(userService.isAccountEnabled(user.id))

            // when
            userService.disableUser(user.id)

            // then
            assertFalse(userService.isAccountEnabled(user.id))
        }

        @Test
        @DisplayName("비활성화된 사용자를 활성화할 수 있다")
        fun activateUser() {
            // given
            val user =
                userService.createUser(
                    username = "inactiveUser2",
                    email = "inact2@ex.com",
                    password = "pass1234",
                    birthDate = LocalDate.now().minusYears(20),
                )
            userService.disableUser(user.id)
            assertFalse(userService.isAccountEnabled(user.id))

            // when
            userService.enableUser(user.id)

            // then
            assertTrue(userService.isAccountEnabled(user.id))
        }
    }

    //region 유효성 검사
    @Nested
    @DisplayName("엔티티 유효성 검사")
    inner class Validation {
        @Test
        @DisplayName("패스워드가 비어있으면 예외")
        fun validatePassword() {
            assertFailsWith<IllegalArgumentException> {
                UserFixture
                    .aUser()
                    .withPassword("")
                    .create()
            }

            assertFailsWith<IllegalArgumentException> {
                UserFixture
                    .aUser()
                    .withPassword("   ")
                    .create()
            }
        }

        @Test
        @DisplayName("미래 날짜 생년월일이면 예외")
        fun validateBirthDate() {
            assertFailsWith<IllegalArgumentException> {
                UserFixture
                    .aUser()
                    .withBirthDate(LocalDate.now().plusDays(1))
                    .create()
            }
        }

        @Test
        @DisplayName("이메일 형식이 아니면 예외")
        fun validateEmail() {
            assertFailsWith<IllegalArgumentException> {
                UserFixture
                    .aUser()
                    .withEmail("invalid-email")
                    .create()
            }
        }

        @Test
        @DisplayName("사용자명 유효성 검사 (3~15자 한글/영문/숫자만 허용)")
        fun validateUsername() {
            // 3자 미만
            assertFailsWith<IllegalArgumentException> {
                UserFixture
                    .aUser()
                    .withUsername("ab")
                    .create()
            }

            // 16자 초과
            assertFailsWith<IllegalArgumentException> {
                UserFixture
                    .aUser()
                    .withUsername("1234567890123456") // 16자
                    .create()
            }

            // 특수문자 포함
            assertFailsWith<IllegalArgumentException> {
                UserFixture
                    .aUser()
                    .withUsername("테스트!@#")
                    .create()
            }
        }
    }
    //endregion

    //region 사용자 조회(대량 생성 / 필터링)
    @Nested
    @DisplayName("사용자 조회 성능 및 필터링 테스트")
    inner class UserSearchPerformance {
        @Test
        @DisplayName("대량의 사용자를 생성하고 조회할 수 있다")
        fun bulkCreateAndSearch() {
            // given
            val userCount = 1000
            userRepository.bulkInsert(userCount) { index ->
                withUsername("bulk$index")
                withEmail("bulk$index@example.com")
                withPassword("password$index!")
            }

            // when
            val foundUsers = userService.getAllUsers()

            // then
            assertEquals(userCount, foundUsers.size)
            assertTrue(foundUsers.any { it.username == "bulk1" })
            assertTrue(foundUsers.any { it.email == "bulk1@example.com" })
        }
    }
    //endregion
}
