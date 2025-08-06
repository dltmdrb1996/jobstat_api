package com.wildrew.jobstat.community.board.usecase.command

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
        testBoard = boardService.createBoard("제목", "내용", "글쓴이", testCategory.id, null, 100L)
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("로그인 사용자가 게시글 좋아요 성공")
        fun `given logged in user and existing board, when like board, then increment count and publish event`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrFail()).thenReturn(testUserId)
            val initialLikeCount = 0
            val expectedLikeCount = 1

            whenever(
                counterService.incrementLikeCount(
                    boardId = eq(testBoard.id),
                    userId = eq(testUserId.toString()),
                    dbLikeCount = eq(initialLikeCount),
                ),
            ).thenReturn(expectedLikeCount)

            val request = LikeBoard.Request(boardId = testBoard.id)

            // When
            val response = likeBoard(request)

            // Then
            assertEquals(expectedLikeCount, response.likeCount)

            // Verify
            verify(theadContextUtils).getCurrentUserIdOrFail()
            verify(counterService).incrementLikeCount(any(), any(), any())
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
        @DisplayName("비로그인 사용자가 좋아요 시 AppException(AUTHENTICATION_FAILURE) 발생")
        fun `given guest user, when like board, then throw AppException`() {
            // Given
            whenever(theadContextUtils.getCurrentUserIdOrFail()).thenThrow(
                AppException.fromErrorCode(ErrorCode.AUTHENTICATION_FAILURE, "로그인이 필요합니다"),
            )
            val request = LikeBoard.Request(boardId = testBoard.id)

            // When & Then
            val exception = assertThrows<AppException> { likeBoard(request) }
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, exception.errorCode)

            // Verify
            verify(theadContextUtils).getCurrentUserIdOrFail()
            verifyNoInteractions(counterService, eventPublisher)
        }

        @Test
        @DisplayName("존재하지 않는 게시글 좋아요 시 AppException(RESOURCE_NOT_FOUND) 발생")
        fun `given non-existent boardId, when like board, then throw AppException`() {
            // Given
            val nonExistentBoardId = 999L
            whenever(theadContextUtils.getCurrentUserIdOrFail()).thenReturn(testUserId)

            val request = LikeBoard.Request(boardId = nonExistentBoardId)

            // When & Then
            val exception = assertThrows<AppException> { likeBoard(request) }
            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)

            // Verify
            verify(theadContextUtils).getCurrentUserIdOrFail()
            verifyNoInteractions(counterService, eventPublisher)
        }
    }
}
