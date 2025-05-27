package com.wildrew.jobstat.community.board.usecase.get

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.board.service.BoardServiceImpl
import com.wildrew.jobstat.community.common.toEpochMilli
import com.wildrew.jobstat.community.counting.CounterService
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*

@DisplayName("GetBoardById UseCase 테스트")
class GetBoardByIdUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var theadContextUtils: TheadContextUtils
    private lateinit var counterService: CounterService

    private lateinit var getBoardById: GetBoardById

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
        counterService = mock()

        getBoardById =
            GetBoardById(
                boardService = boardService,
                counterService = counterService,
                theadContextUtils = theadContextUtils,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardService.createBoard("테스트 제목", "테스트 내용", "작성자", testCategory.id, null, 100L)
        boardRepository.updateViewCount(testBoard.id, 10)
        boardRepository.updateLikeCount(testBoard.id, 5)
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
        @DisplayName("로그인 사용자가 게시글 상세 조회 성공")
        fun `given logged in user and existing boardId, when execute, then return board details with counters`() {
            // Given
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(testUserId)
            val expectedViewCount = testBoard.viewCount + 1 // 조회수 1 증가 예상
            val expectedLikeCount = testBoard.likeCount // 좋아요 수는 그대로
            val expectedUserLiked = true

            // CounterService Mock 설정
            whenever(
                counterService.getSingleBoardCounters(
                    boardId = eq(testBoard.id),
                    userId = eq(testUserId.toString()),
                    dbViewCount = eq(testBoard.viewCount),
                    dbLikeCount = eq(testBoard.likeCount),
                ),
            ).thenReturn(
                CounterService.BoardCounters(
                    boardId = testBoard.id,
                    viewCount = expectedViewCount,
                    likeCount = expectedLikeCount,
                    userLiked = expectedUserLiked,
                ),
            )

            val request = GetBoardById.Request(boardId = testBoard.id)

            // When
            val response = getBoardById(request)

            // Then
            assertEquals(testBoard.id.toString(), response.id)
            assertEquals(testBoard.title, response.title)
            assertEquals(testBoard.content, response.content)
            assertEquals(testBoard.author, response.author)
            assertEquals(expectedViewCount, response.viewCount)
            assertEquals(expectedLikeCount, response.likeCount)
            assertEquals(expectedUserLiked, response.userLiked)
            assertEquals(testBoard.commentCount, response.commentCount)
            assertEquals(testBoard.category.id, response.categoryId)
            assertEquals(testBoard.createdAt, response.createdAt)
            assertEquals(testBoard.updatedAt.toEpochMilli(), response.eventTs)

            // Verify
            verify(theadContextUtils).getCurrentUserId()
            verify(counterService).getSingleBoardCounters(any(), eq(testUserId.toString()), any(), any())
        }

        @Test
        @DisplayName("비로그인 사용자가 게시글 상세 조회 성공")
        fun `given guest user and existing boardId, when execute, then return board details without user liked info`() {
            // Given
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(guestUserId) // 비로그인
            val expectedViewCount = testBoard.viewCount + 1
            val expectedLikeCount = testBoard.likeCount
            val expectedUserLiked = false // 비로그인은 항상 false

            whenever(
                counterService.getSingleBoardCounters(
                    boardId = eq(testBoard.id),
                    userId = eq(null), // 비로그인 시 userId null 전달
                    dbViewCount = eq(testBoard.viewCount),
                    dbLikeCount = eq(testBoard.likeCount),
                ),
            ).thenReturn(
                CounterService.BoardCounters(
                    boardId = testBoard.id,
                    viewCount = expectedViewCount,
                    likeCount = expectedLikeCount,
                    userLiked = expectedUserLiked,
                ),
            )

            val request = GetBoardById.Request(boardId = testBoard.id)

            // When
            val response = getBoardById(request)

            // Then
            assertEquals(expectedViewCount, response.viewCount)
            assertEquals(expectedLikeCount, response.likeCount)
            assertFalse(response.userLiked) // 비로그인 확인

            // Verify
            verify(theadContextUtils).getCurrentUserId()
            verify(counterService).getSingleBoardCounters(any(), eq(null), any(), any())
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailCases {
        @Test
        @DisplayName("존재하지 않는 게시글 ID 조회 시 EntityNotFoundException 발생 (BoardService 레벨)")
        fun `given non-existent boardId, when execute, then throw EntityNotFoundException`() {
            // Given
            val nonExistentBoardId = 999L
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(testUserId)

            val request = GetBoardById.Request(boardId = nonExistentBoardId)

            // When & Then
            assertThrows<EntityNotFoundException> { getBoardById(request) }
            verifyNoInteractions(counterService)
        }

        @Test
        @DisplayName("CounterService에서 예외 발생 시 전파")
        fun `given counterService throws exception, when execute, then exception propagates`() {
            // Given
            whenever(theadContextUtils.getCurrentUserId()).thenReturn(testUserId)
            val counterException = RuntimeException("Redis connection failed")
            whenever(counterService.getSingleBoardCounters(any(), any(), any(), any()))
                .thenThrow(counterException)

            val request = GetBoardById.Request(boardId = testBoard.id)

            // When & Then
            val thrownException = assertThrows<RuntimeException> { getBoardById(request) }
            assertEquals(counterException, thrownException)
        }
    }
}
