package com.wildrew.jobstat.community.board.usecase.command

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.board.service.BoardServiceImpl
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.community.utils.FakePasswordUtil
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*

@DisplayName("UpdateBoard UseCase 테스트")
class UpdateBoardUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var passwordUtil: FakePasswordUtil

    private lateinit var boardService: BoardService

    private lateinit var theadContextUtils: TheadContextUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher

    private lateinit var updateBoard: UpdateBoard

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

        updateBoard =
            UpdateBoard(
                boardService = boardService,
                passwordUtil = passwordUtil,
                theadContextUtils = theadContextUtils,
                eventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
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
            title = "원본 제목",
            content = "원본 내용입니다.",
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
        @DisplayName("로그인 사용자가 자신의 게시글 수정 성공")
        fun `given owner user and valid request, when update own board, then success and publish event`() {
            // Given
            val board = createTestBoard(ownerUserId, null)
            whenever(theadContextUtils.getCurrentUserIdOrFail()).thenReturn(ownerUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(false)

            val newTitle = "수정된 제목"
            val newContent = "수정된 내용입니다. 길이를 맞춥니다."
            val request = UpdateBoard.ExecuteRequest(board.id, newTitle, newContent, null)

            // When
            val response = updateBoard(request)

            // Then
            assertEquals(board.id.toString(), response.id)
            assertEquals(newTitle, response.title)
            assertEquals(newContent, response.content)
            assertNotNull(response.updatedAt)

            val updatedBoard = boardRepository.findById(board.id)
            assertEquals(newTitle, updatedBoard.title)
            assertEquals(newContent, updatedBoard.content)

            // Verify
            verify(eventPublisher).publishBoardUpdated(argThat { id == board.id })
            verify(theadContextUtils).getCurrentUserIdOrFail()
        }

        @Test
        @DisplayName("관리자가 다른 사용자의 게시글 수정 성공")
        fun `given admin user and valid request, when update other user board, then success`() {
            // Given
            val board = createTestBoard(otherUserId, null)
            whenever(theadContextUtils.getCurrentUserIdOrFail()).thenReturn(adminUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(true)

            val newTitle = "관리자가 수정한 제목"
            val newContent = "관리자가 수정한 내용입니다."
            val request = UpdateBoard.ExecuteRequest(board.id, newTitle, newContent, null)

            // When
            val response = updateBoard(request)

            // Then
            assertEquals(newTitle, response.title)
            val updatedBoard = boardRepository.findById(board.id)
            assertEquals(newTitle, updatedBoard.title)
            assertEquals(newContent, updatedBoard.content)
            verify(eventPublisher).publishBoardUpdated(argThat { id == board.id })
            verify(theadContextUtils).getCurrentUserIdOrFail()
            verify(theadContextUtils).isAdmin()
        }

        @Test
        @DisplayName("비회원 사용자가 올바른 비밀번호로 게시글 수정 성공")
        fun `given guest user with correct password and valid request, when update guest board, then success`() {
            // Given
            val board = createTestBoard(guestUserId, correctPassword)
            val newTitle = "비회원 수정 제목"
            val newContent = "비회원 수정 내용입니다."
            val request = UpdateBoard.ExecuteRequest(board.id, newTitle, newContent, correctPassword)

            // When
            val response = updateBoard(request)

            // Then
            assertEquals(newTitle, response.title)
            val updatedBoard = boardRepository.findById(board.id)
            assertEquals(newTitle, updatedBoard.title)
            assertEquals(newContent, updatedBoard.content)
            verify(eventPublisher).publishBoardUpdated(argThat { id == board.id })
            assertTrue(passwordUtil.matches(correctPassword, updatedBoard.password!!))
            verifyNoInteractions(theadContextUtils)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 권한 없음")
    inner class PermissionFailCases {
        @Test
        @DisplayName("로그인 사용자가 다른 사용자 게시글 수정 시 AppException(INSUFFICIENT_PERMISSION) 발생")
        fun `given non-owner user, when update other user board, then throw AppException`() {
            // Given
            val board = createTestBoard(ownerUserId, null)
            whenever(theadContextUtils.getCurrentUserIdOrFail()).thenReturn(otherUserId)
            whenever(theadContextUtils.isAdmin()).thenReturn(false)

            val request = UpdateBoard.ExecuteRequest(board.id, "다른 제목", "다른 내용", null)

            // When & Then
            val exception = assertThrows<AppException> { updateBoard(request) }
            assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.errorCode)
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("로그인 필요한 게시글 수정 시 비로그인 상태면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user, when update member board, then throw AppException`() {
            // Given
            val board = createTestBoard(ownerUserId, null)
            whenever(theadContextUtils.getCurrentUserIdOrFail()).thenThrow(
                AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "로그인이 필요합니다"),
            )
            val request = UpdateBoard.ExecuteRequest(board.id, "제목", "내용", null)

            // When & Then
            val exception = assertThrows<AppException> { updateBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("로그인이 필요합니다"))
            verifyNoInteractions(eventPublisher)
        }
    }
}
