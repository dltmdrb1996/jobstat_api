package com.wildrew.jobstat.community.board.usecase.command

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.board.service.BoardServiceImpl
import com.wildrew.jobstat.community.counting.CounterService
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher // Mock 대상
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils // Mock 대상
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*
import java.time.LocalDateTime

@DisplayName("LikeBoard UseCase 테스트")
class LikeBoardUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var theadContextUtils: TheadContextUtils
    private lateinit var eventPublisher: CommunityCommandEventPublisher
    private lateinit var counterService: CounterService

    private lateinit var likeBoard: LikeBoard

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private val testUserId = 1L
    private val guestUserId: Long? = null

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        theadContextUtils = mock()
        eventPublisher = mock()
        counterService = mock()

        likeBoard =
            LikeBoard(
                counterService = counterService,
                theadContextUtils = theadContextUtils,
                boardService = boardService,
                communityCommandEventPublisher = eventPublisher,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardService.createBoard("제목", "내용", "글쓴이", testCategory.id, null, 100L) // 게시글 생성
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
        @DisplayName("로그인 사용자가 게시글 좋아요 성공")
        fun `given logged in user and existing board, when like board, then increment count and publish event`() {
            // Given
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(testUserId)
            val initialLikeCount = boardRepository.findLikeCountById(testBoard.id) ?: 0
            val expectedLikeCount = initialLikeCount + 1

            // CounterService Mock 설정: incrementLikeCount 호출 시 예상 카운트 반환
            whenever(
                counterService.incrementLikeCount(
                    boardId = eq(testBoard.id),
                    userId = eq(testUserId.toString()),
                    dbLikeCount = eq(initialLikeCount), // 현재 DB(Fake) 값 전달
                ),
            ).thenReturn(expectedLikeCount) // 증가된 카운트 반환하도록 설정

            val request = LikeBoard.Request(boardId = testBoard.id)

            // When
            val response = likeBoard(request)

            // Then
            assertEquals(expectedLikeCount, response.likeCount)

            // Verify
            verify(theadContextUtils).getCurrentUserId()
            verify(counterService).incrementLikeCount(
                boardId = eq(testBoard.id),
                userId = eq(testUserId.toString()),
                dbLikeCount = eq(initialLikeCount),
            )
            verify(eventPublisher).publishBoardLiked(
                boardId = eq(testBoard.id),
                createdAt = any<LocalDateTime>(),
                userId = eq(testUserId),
                likeCount = eq(expectedLikeCount),
                eventTs = anyLong(),
            )
        }

        @Nested
        @DisplayName("실패 케이스")
        inner class FailCases {
            @Test
            @DisplayName("비로그인 사용자가 좋아요 시 AppException(AUTHENTICATION_FAILURE) 발생")
            fun `given guest user, when like board, then throw AppException`() {
                // Given
                whenever(theadContextUtils.getCurrentUserId()).thenReturn(guestUserId) // 비로그인 Mock
                val request = LikeBoard.Request(boardId = testBoard.id)

                // When & Then
                val exception = assertThrows<AppException> { likeBoard(request) }
                assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)
                assertTrue(exception.message.contains("로그인이 필요합니다"))

                // Verify
                verify(theadContextUtils).getCurrentUserId()
                verifyNoInteractions(counterService, eventPublisher) // 카운터, 이벤트 미호출 확인
            }

            @Test
            @DisplayName("존재하지 않는 게시글 좋아요 시 AppException(RESOURCE_NOT_FOUND) 발생")
            fun `given non-existent boardId, when like board, then throw AppException`() {
                // Given
                val nonExistentBoardId = 999L
                whenever(theadContextUtils.getCurrentUserId()).thenReturn(testUserId)

                val request = LikeBoard.Request(boardId = nonExistentBoardId)

                // When & Then
                val exception = assertThrows<AppException> { likeBoard(request) }
                assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
                assertTrue(exception.message.contains("게시글을 찾을 수 없습니다"))

                // Verify
                verify(theadContextUtils).getCurrentUserId()
                verifyNoInteractions(counterService, eventPublisher)
            }

            @Test
            @DisplayName("CounterService에서 예외 발생 시 전파되는지 확인 (예시)")
            fun `given counterService throws exception, when like board, then exception propagates`() {
                // Given
                whenever(theadContextUtils.getCurrentUserId()).thenReturn(testUserId)
                val initialLikeCount = boardRepository.findLikeCountById(testBoard.id) ?: 0
                val counterException = RuntimeException("Redis connection failed")

                whenever(
                    counterService.incrementLikeCount(
                        boardId = eq(testBoard.id),
                        userId = eq(testUserId.toString()),
                        dbLikeCount = eq(initialLikeCount),
                    ),
                ).thenThrow(counterException)

                val request = LikeBoard.Request(boardId = testBoard.id)

                // When & Then
                val thrownException = assertThrows<RuntimeException> { likeBoard(request) }
                assertEquals(counterException, thrownException)

                // Verify
                verify(theadContextUtils).getCurrentUserId()
                verify(counterService).incrementLikeCount(any(), any(), any())
                verifyNoInteractions(eventPublisher)
            }
        }
    }
}
