package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.counting.CounterService // Mock 대상
import com.example.jobstat.community.event.CommunityCommandEventPublisher // Mock 대상
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.SecurityUtils // Mock 대상
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*
import java.time.LocalDateTime

@DisplayName("UnlikeBoard UseCase 테스트")
class UnlikeBoardUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var securityUtils: SecurityUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher
    private lateinit var counterService: CounterService

    private lateinit var unlikeBoard: UnlikeBoard

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private val testUserId = 1L
    private val guestUserId: Long? = null

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        securityUtils = mock()
        eventPublisher = mock()
        counterService = mock()

        unlikeBoard =
            UnlikeBoard(
                counterService = counterService,
                securityUtils = securityUtils,
                boardService = boardService,
                communityCommandEventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardService.createBoard("제목", "내용", "글쓴이", testCategory.id, null, 100L)
        boardRepository.updateLikeCount(testBoard.id, 5) // 초기 좋아요 5개 설정
        testBoard = boardRepository.findById(testBoard.id) // 최신 상태 로드
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("로그인 사용자가 게시글 좋아요 취소 성공")
        fun `given logged in user and existing board, when unlike board, then decrement count and publish event`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val initialLikeCount = boardRepository.findLikeCountById(testBoard.id) ?: 0 // 현재 DB 값 (5)
            val expectedLikeCount = initialLikeCount - 1 // 예상 결과 (4)

            whenever(
                counterService.decrementLikeCount(
                    boardId = eq(testBoard.id),
                    userId = eq(testUserId.toString()),
                    dbLikeCount = eq(initialLikeCount),
                ),
            ).thenReturn(expectedLikeCount) // 감소된 카운트 반환

            val request = UnlikeBoard.Request(boardId = testBoard.id)

            // When
            val response = unlikeBoard(request)

            // Then
            assertEquals(expectedLikeCount, response.likeCount)

            // Verify
            verify(securityUtils).getCurrentUserId()
            verify(counterService).decrementLikeCount(
                boardId = eq(testBoard.id),
                userId = eq(testUserId.toString()),
                dbLikeCount = eq(initialLikeCount),
            )
            // 좋아요 취소 시에도 좋아요 이벤트 발행 (최종 카운트 동기화 목적)
            verify(eventPublisher).publishBoardLiked(
                boardId = eq(testBoard.id),
                createdAt = any<LocalDateTime>(),
                userId = eq(testUserId),
                likeCount = eq(expectedLikeCount),
                eventTs = anyLong(),
            )
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailCases {
        @Test
        @DisplayName("비로그인 사용자가 좋아요 취소 시 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user, when unlike board, then throw AppException`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(guestUserId)
            val request = UnlikeBoard.Request(boardId = testBoard.id)

            // When & Then
            val exception = assertThrows<AppException> { unlikeBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
            assertTrue(exception.message.contains("로그인이 필요합니다"))

            verify(securityUtils).getCurrentUserId()
            verifyNoInteractions(counterService, eventPublisher)
        }

        @Test
        @DisplayName("존재하지 않는 게시글 좋아요 취소 시 AppException(RESOURCE_NOT_FOUND) 발생")
        fun `given non-existent boardId, when unlike board, then throw AppException`() {
            // Given
            val nonExistentBoardId = 999L
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)

            val request = UnlikeBoard.Request(boardId = nonExistentBoardId)

            // When & Then
            val exception = assertThrows<AppException> { unlikeBoard(request) }
            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
            assertTrue(exception.message.contains("게시글을 찾을 수 없습니다"))

            verify(securityUtils).getCurrentUserId()
            verifyNoInteractions(counterService, eventPublisher)
        }

        @Test
        @DisplayName("CounterService에서 예외 발생 시 전파되는지 확인")
        fun `given counterService throws exception, when unlike board, then exception propagates`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val initialLikeCount = boardRepository.findLikeCountById(testBoard.id) ?: 0
            val counterException = RuntimeException("Redis decrement failed")

            whenever(
                counterService.decrementLikeCount(any(), any(), any()),
            ).thenThrow(counterException)

            val request = UnlikeBoard.Request(boardId = testBoard.id)

            // When & Then
            val thrownException = assertThrows<RuntimeException> { unlikeBoard(request) }
            assertEquals(counterException, thrownException)

            verify(securityUtils).getCurrentUserId()
            verify(counterService).decrementLikeCount(any(), any(), any())
            verifyNoInteractions(eventPublisher)
        }
    }
}
