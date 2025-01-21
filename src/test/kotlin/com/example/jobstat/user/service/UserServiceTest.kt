package com.example.jobstat.user.service

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.user.FakeUserRepository
import com.example.jobstat.user.UserFixture
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.*

@DisplayName("UserService 테스트")
class UserServiceTest {
    private lateinit var userRepository: FakeUserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        // 매 테스트마다 FakeUserRepository를 초기화하고
        // 그 위에 UserService를 생성만 해 둡니다.
        userRepository = FakeUserRepository()
        userService = UserServiceImpl(userRepository)
    }

    @Nested
    @DisplayName("사용자 생성")
    inner class CreateUser {

        @Test
        @DisplayName("유효한 정보로 사용자를 생성할 수 있다")
        fun createValidUser() {
            // given
            val user = UserFixture.aUser()
                .withUsername("테스트123")
                .withEmail("test@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()

            // when
            val createdUser = userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )

            // then
            assertEquals(user.username, createdUser.username)
            assertEquals(user.email, createdUser.email)
            assertEquals(user.birthDate, createdUser.birthDate)
            assertTrue(createdUser.isActive)
        }

        @Test
        @DisplayName("중복된 이메일로 사용자를 생성할 수 없다")
        fun cannotCreateUserWithDuplicateEmail() {
            // given
            val user = UserFixture.aUser()
                .withUsername("중복유저")
                .withEmail("dup@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()

            // 이미 존재하는 유저를 하나 만들어둔다
            userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )

            // when & then
            val exception = assertFailsWith<AppException> {
                userService.createUser(
                    username = "신규123",    // 다른 username
                    email = user.email, // 이미 존재하는 email
                    birthDate = user.birthDate
                )
            }
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.errorCode)
        }

        @Test
        @DisplayName("중복된 사용자명으로 사용자를 생성할 수 없다")
        fun cannotCreateUserWithDuplicateUsername() {
            // given
            val user = UserFixture.aUser()
                .withUsername("테스트123")
                .withEmail("test@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()

            // 이미 존재하는 유저를 하나 만들어둔다
            userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )

            // when & then
            val exception = assertFailsWith<AppException> {
                userService.createUser(
                    username = user.username, // 이미 존재하는 username
                    email = "different@example.com",
                    birthDate = user.birthDate
                )
            }
            assertEquals(ErrorCode.DUPLICATE_RESOURCE, exception.errorCode)
        }
    }

    @Nested
    @DisplayName("사용자 조회")
    inner class GetUser {

        @Test
        @DisplayName("ID로 사용자를 조회할 수 있다")
        fun getUserById() {
            // given
            val user = UserFixture.aUser()
                .withUsername("조회123")
                .withEmail("test@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            val savedUser = userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )
            val savedUserId = savedUser.id

            // when
            val foundUser = userService.getUserById(savedUserId)

            // then
            assertEquals(savedUserId, foundUser.id)
            assertEquals(savedUser.username, foundUser.username)
        }

        @Test
        @DisplayName("사용자명으로 사용자를 조회할 수 있다")
        fun getUserByUsername() {
            // given
            val user = UserFixture.aUser()
                .withUsername("조회123")
                .withEmail("test2@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            val savedUser = userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
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
            val user = UserFixture.aUser()
                .withUsername("이메일조회")
                .withEmail("email-check@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            val savedUser = userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
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
            // given
            val nonExistentId = 999L

            // when & then
            assertFailsWith<EntityNotFoundException> {
                userService.getUserById(nonExistentId)
            }
        }

        @Test
        @DisplayName("모든 사용자를 조회할 수 있다")
        fun getAllUsers() {
            // given
            // 1) 먼저 하나의 사용자 생성
            val firstFixture = UserFixture.aUser()
                .withUsername("조회123")
                .withEmail("test@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            userService.createUser(
                username = firstFixture.username,
                email = firstFixture.email,
                birthDate = firstFixture.birthDate
            )

            // 2) 나머지 5명의 사용자 생성
            val userCount = 5
            repeat(userCount) { index ->
                val user = UserFixture.aUser()
                    .withUsername("유저${index}번")
                    .withEmail("user${index}@example.com")
                    .withBirthDate(LocalDate.now().minusYears(20))
                    .create()

                userService.createUser(
                    username = user.username,
                    email = user.email,
                    birthDate = user.birthDate
                )
            }

            // when
            val users = userService.getAllUsers()

            // then
            // 처음 1명 + 새로 만든 5명 = 6
            assertEquals(userCount + 1, users.size)
        }
    }

    @Nested
    @DisplayName("사용자 삭제")
    inner class DeleteUser {

        @Test
        @DisplayName("사용자를 삭제할 수 있다")
        fun deleteUser() {
            // given
            val user = UserFixture.aUser()
                .withUsername("삭제123")
                .withEmail("test@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            val savedUser = userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )
            val savedUserId = savedUser.id

            // when
            userService.deleteUser(savedUserId)

            // then
            assertFailsWith<EntityNotFoundException> {
                userService.getUserById(savedUserId)
            }
        }
    }

    @Nested
    @DisplayName("사용자 정보 검증")
    inner class ValidateUserInfo {

        @Test
        @DisplayName("사용 가능한 사용자명인지 확인할 수 있다")
        fun checkUsernameAvailability() {
            // given
            val user = UserFixture.aUser()
                .withUsername("가입123")
                .withEmail("existing@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )

            // when & then
            assertTrue(userService.isUsernameAvailable("신규123"))
            assertFalse(userService.isUsernameAvailable("가입123"))
        }

        @Test
        @DisplayName("사용 가능한 이메일인지 확인할 수 있다")
        fun checkEmailAvailability() {
            // given
            val user = UserFixture.aUser()
                .withUsername("가입456")
                .withEmail("existing@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )

            // when & then
            assertTrue(userService.isEmailAvailable("new@example.com"))
            assertFalse(userService.isEmailAvailable("existing@example.com"))
        }

        @Test
        @DisplayName("사용자 활성화 상태를 확인할 수 있다")
        fun checkUserActivation() {
            // given
            val user = UserFixture.aUser()
                .withUsername("활성화체크")
                .withEmail("active@example.com")
                .withBirthDate(LocalDate.now().minusYears(20))
                .create()
            val savedUser = userService.createUser(
                username = user.username,
                email = user.email,
                birthDate = user.birthDate
            )

            // when & then
            assertTrue(userService.isActivated(savedUser.id))
        }
    }
}
