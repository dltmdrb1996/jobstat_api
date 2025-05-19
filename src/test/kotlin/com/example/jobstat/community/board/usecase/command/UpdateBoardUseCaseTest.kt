package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.event.CommunityCommandEventPublisher // Mock 대상
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_security.util.SecurityUtils // Mock 대상
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
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

    private lateinit var securityUtils: SecurityUtils
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

        securityUtils = mock()
        eventPublisher = mock()

        updateBoard =
            UpdateBoard(
                boardService = boardService,
                passwordUtil = passwordUtil,
                securityUtils = securityUtils,
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

    // 테스트 데이터 생성을 위한 헬퍼 함수
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
            whenever(securityUtils.getCurrentUserId()).thenReturn(ownerUserId)
            whenever(securityUtils.isAdmin()).thenReturn(false)

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

            // DB 상태 확인
            val updatedBoard = boardRepository.findById(board.id)
            assertEquals(newTitle, updatedBoard.title)
            assertEquals(newContent, updatedBoard.content)

            // Verify
            verify(eventPublisher).publishBoardUpdated(argThat { id == board.id })
            verify(securityUtils).getCurrentUserId()
        }

        @Test
        @DisplayName("관리자가 다른 사용자의 게시글 수정 성공")
        fun `given admin user and valid request, when update other user board, then success`() {
            // Given
            val board = createTestBoard(otherUserId, null)
            whenever(securityUtils.getCurrentUserId()).thenReturn(adminUserId)
            whenever(securityUtils.isAdmin()).thenReturn(true) // 관리자

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
            verify(securityUtils).getCurrentUserId()
            verify(securityUtils).isAdmin()
        }

        @Test
        @DisplayName("비회원 사용자가 올바른 비밀번호로 게시글 수정 성공")
        fun `given guest user with correct password and valid request, when update guest board, then success`() {
            // Given
            val board = createTestBoard(guestUserId, correctPassword)
            whenever(securityUtils.getCurrentUserId()).thenReturn(guestUserId) // 필요 없음 (비밀번호 검증)

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
            verifyNoInteractions(securityUtils)
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
            whenever(securityUtils.getCurrentUserId()).thenReturn(otherUserId) // 다른 사용자
            whenever(securityUtils.isAdmin()).thenReturn(false) // 관리자 아님

            val request = UpdateBoard.ExecuteRequest(board.id, "다른 제목", "다른 내용", null)

            // When & Then
            val exception = assertThrows<AppException> { updateBoard(request) }
            assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.errorCode)
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("비회원 게시글 수정 시 비밀번호 틀리면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user with wrong password, when update guest board, then throw AppException`() {
            // Given
            val board = createTestBoard(guestUserId, correctPassword)
            val request = UpdateBoard.ExecuteRequest(board.id, "제목", "내용", wrongPassword) // 틀린 비번

            // When & Then
            val exception = assertThrows<AppException> { updateBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호가 일치하지 않습니다"))
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("비회원 게시글 수정 시 비밀번호 미입력 시 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user without password, when update guest board, then throw AppException`() {
            // Given
            val board = createTestBoard(guestUserId, correctPassword)
            val request = UpdateBoard.ExecuteRequest(board.id, "제목", "내용", null) // 비번 없음

            // When & Then
            val exception = assertThrows<AppException> { updateBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("비밀번호가 필요합니다"))
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("로그인 필요한 게시글 수정 시 비로그인 상태면 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user, when update member board, then throw AppException`() {
            // Given
            val board = createTestBoard(ownerUserId, null) // 회원 게시글
            whenever(securityUtils.getCurrentUserId()).thenReturn(guestUserId) // 비로그인 상태
            val request = UpdateBoard.ExecuteRequest(board.id, "제목", "내용", null)

            // When & Then
            val exception = assertThrows<AppException> { updateBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("로그인이 필요합니다"))
            verifyNoInteractions(eventPublisher)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        private lateinit var board: Board

        @BeforeEach
        fun setupValidation() {
            // 모든 유효성 검사 테스트는 본인 게시글 수정 상황을 가정
            board = createTestBoard(ownerUserId, null)
            whenever(securityUtils.getCurrentUserId()).thenReturn(ownerUserId)
            whenever(securityUtils.isAdmin()).thenReturn(false)
        }

        @Test
        @DisplayName("제목이 비어있으면 ConstraintViolationException 발생")
        fun `given blank title, when update board, then throw ConstraintViolationException`() {
            // Given
            val request = UpdateBoard.ExecuteRequest(board.id, " ", "정상 내용", null)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { updateBoard(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "title" })
            verifyNoInteractions(eventPublisher)
        }

        @Test
        @DisplayName("내용이 최대 길이를 초과하면 ConstraintViolationException 발생")
        fun `given too long content, when update board, then throw ConstraintViolationException`() {
            // Given
            val request = UpdateBoard.ExecuteRequest(board.id, "정상 제목", "a".repeat(5001), null)

            // When & Then
            val exception = assertThrows<ConstraintViolationException> { updateBoard(request) }
            assertTrue(exception.constraintViolations.any { it.propertyPath.toString() == "content" })
            verifyNoInteractions(eventPublisher)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 기타")
    inner class OtherFailCases {
        @Test
        @DisplayName("존재하지 않는 게시글 수정 시 EntityNotFoundException 발생")
        fun `given non-existent boardId, when update board, then throw EntityNotFoundException`() {
            // Given
            val nonExistentBoardId = 999L
            whenever(securityUtils.getCurrentUserId()).thenReturn(ownerUserId)
            val request = UpdateBoard.ExecuteRequest(nonExistentBoardId, "제목", "내용", null)

            // When & Then
            assertThrows<EntityNotFoundException> { updateBoard(request) }
            verifyNoInteractions(eventPublisher)
        }
    }
}
