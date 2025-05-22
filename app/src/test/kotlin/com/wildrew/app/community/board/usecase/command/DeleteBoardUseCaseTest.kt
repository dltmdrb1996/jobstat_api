package com.wildrew.app.community.board.usecase.command

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.board.service.BoardServiceImpl
import com.wildrew.jobstat.community.counting.CounterService
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.wildrew.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*

@DisplayName("DeleteBoard UseCase 테스트")
class DeleteBoardUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var passwordUtil: FakePasswordUtil

    private lateinit var boardService: BoardService

    private lateinit var theadContextUtils: TheadContextUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher
    private lateinit var counterService: CounterService // Mock 추가

    private lateinit var deleteBoard: DeleteBoard

    private lateinit var testCategory: BoardCategory
    private val ownerUserId = 1L
    private val otherUserId = 2L
    private val adminUserId = 99L
    private val guestUserId: Long? = null
    private val correctPassword = "password123"
    private val wrongPassword = "wrong_password"

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        passwordUtil = FakePasswordUtil()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        theadContextUtils = mock()
        eventPublisher = mock()
        counterService = mock()

        deleteBoard =
            DeleteBoard(
                boardService = boardService,
                counterService = counterService,
                passwordUtil = passwordUtil,
                theadContextUtils = theadContextUtils,
                communityCommandEventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory =
            categoryRepository.save(
                CategoryFixture.aCategory().withName("TEST_CAT").create(),
            )
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
    }

    private fun createTestBoard(
        userId: Long?,
        password: String?,
    ): Board {
        val encodedPassword = password?.let { passwordUtil.encode(it) }
        return boardService.createBoard(
            title = "테스트 게시글",
            content = "삭제 테스트용 내용입니다.",
            author = if (userId != null) "user$userId" else "guest",
            categoryId = testCategory.id,
            password = encodedPassword,
            userId = userId,
        )
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("로그인 사용자가 자신의 게시글 삭제 성공")
        fun `given owner user, when delete own board, then success and publish event`() {
            // Given
            val board = createTestBoard(ownerUserId, null) // 사용자 ID로 생성된 게시글
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(ownerUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(false)

            val request = DeleteBoard.ExecuteRequest(board.id, null)

            // When
            val response = deleteBoard(request)

            // Then
            assertTrue(response.success)
            assertFalse(boardRepository.existsById(board.id))

            // Verify
            verify(counterService).cleanupBoardCounters(board.id)
            verify(eventPublisher).publishBoardDeleted(
                eq(board.id),
                eq(board.userId),
                eq(testCategory.id),
                anyLong(),
            )
            verify(theadContextUtils).getCurrentUserId()
            verify(theadContextUtils).isAdmin()
        }

        @Test
        @DisplayName("관리자가 다른 사용자의 게시글 삭제 성공")
        fun `given admin user, when delete other user board, then success and publish event`() {
            // Given
            val board = createTestBoard(otherUserId, null) // 다른 사용자 게시글
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(adminUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(true) // 관리자 Mock

            val request = DeleteBoard.ExecuteRequest(board.id, null)

            // When
            val response = deleteBoard(request)

            // Then
            assertTrue(response.success)
            assertFalse(boardRepository.existsById(board.id))
            verify(counterService).cleanupBoardCounters(board.id)
            verify(eventPublisher).publishBoardDeleted(
                eq(board.id),
                eq(board.userId),
                eq(testCategory.id),
                anyLong(),
            )
            verify(theadContextUtils).getCurrentUserId()
            verify(theadContextUtils).isAdmin()
        }

        @Test
        @DisplayName("비회원 사용자가 올바른 비밀번호로 게시글 삭제 성공")
        fun `given guest user with correct password, when delete guest board, then success and publish event`() {
            // Given
            val board = createTestBoard(guestUserId, correctPassword)
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(guestUserId)

            val request = DeleteBoard.ExecuteRequest(board.id, correctPassword)

            // When
            val response = deleteBoard(request)

            // Then
            assertTrue(response.success)
            assertFalse(boardRepository.existsById(board.id))
            assertTrue(passwordUtil.matches(correctPassword, passwordUtil.encode(correctPassword)))

            // Verify
            verify(counterService).cleanupBoardCounters(board.id)
            verify(eventPublisher).publishBoardDeleted(
                eq(board.id),
                eq(board.userId),
                eq(testCategory.id),
                anyLong(),
            )
            verifyNoInteractions(theadContextUtils)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 권한 없음")
    inner class PermissionFailCases {
        @Test
        @DisplayName("로그인 사용자가 다른 사용자의 게시글 삭제 시 AppException(INSUFFICIENT_PERMISSION) 발생")
        fun `given non-owner user, when delete other user board, then throw AppException`() {
            // Given
            val board = createTestBoard(ownerUserId, null)
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(otherUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(false)

            val request = DeleteBoard.ExecuteRequest(board.id, null)

            // When & Then
            val exception = assertThrows<AppException> { deleteBoard(request) }
            assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.errorCode)
            assertTrue(boardRepository.existsById(board.id))
            verifyNoInteractions(counterService, eventPublisher)
        }

        @Test
        @DisplayName("비회원 사용자가 비밀번호 틀리면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user with wrong password, when delete guest board, then throw AppException`() {
            // Given
            val board = createTestBoard(guestUserId, correctPassword)
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(guestUserId)

            val request = DeleteBoard.ExecuteRequest(board.id, wrongPassword)

            // When & Then
            val exception = assertThrows<AppException> { deleteBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호가 일치하지 않습니다"))
            assertTrue(boardRepository.existsById(board.id))
            verifyNoInteractions(counterService, eventPublisher)
        }

        @Test
        @DisplayName("비회원 게시글 삭제 시 비밀번호 미입력하면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user without password, when delete guest board, then throw AppException`() {
            // Given
            val board = createTestBoard(guestUserId, correctPassword)
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(guestUserId)

            val request = DeleteBoard.ExecuteRequest(board.id, null)

            // When & Then
            val exception = assertThrows<AppException> { deleteBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호가 필요합니다"))
            assertTrue(boardRepository.existsById(board.id))
            verifyNoInteractions(counterService, eventPublisher)
        }

        @Test
        @DisplayName("로그인 필요한 게시글 삭제 시 로그인 안되어 있으면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user, when delete member board, then throw AppException`() {
            // Given
            val board = createTestBoard(ownerUserId, null)
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(guestUserId)

            val request = DeleteBoard.ExecuteRequest(board.id, null)

            // When & Then
            val exception = assertThrows<AppException> { deleteBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("로그인이 필요합니다"))
            assertTrue(boardRepository.existsById(board.id))
            verifyNoInteractions(counterService, eventPublisher)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 기타")
    inner class OtherFailCases {
        @Test
        @DisplayName("존재하지 않는 게시글 삭제 시 EntityNotFoundException 발생 (BoardService 레벨)")
        fun `given non-existent boardId, when delete board, then throw EntityNotFoundException`() {
            // Given
            val nonExistentBoardId = 999L
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(ownerUserId)

            val request = DeleteBoard.ExecuteRequest(nonExistentBoardId, null)

            // When & Then
            assertThrows<EntityNotFoundException> { deleteBoard(request) }
            verifyNoInteractions(counterService, eventPublisher)
        }
    }
}
