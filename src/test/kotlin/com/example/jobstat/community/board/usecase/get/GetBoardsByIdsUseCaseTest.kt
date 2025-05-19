package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.counting.CounterService // Mock 대상
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_security.util.SecurityUtils // Mock 대상
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*

@DisplayName("GetBoardsByIds UseCase 테스트")
class GetBoardsByIdsUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService

    private lateinit var securityUtils: SecurityUtils
    private lateinit var counterService: CounterService

    private lateinit var getBoardsByIds: GetBoardsByIds

    private lateinit var testCategory: BoardCategory
    private lateinit var board1: Board
    private lateinit var board2: Board
    private lateinit var board3: Board
    private val testUserId = 1L
    private val guestUserId: Long? = null

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        securityUtils = mock()
        counterService = mock()

        getBoardsByIds =
            GetBoardsByIds(
                securityUtils = securityUtils,
                boardService = boardService,
                counterService = counterService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        board1 =
            boardService.createBoard("제목1", "내용1", "작성자1", testCategory.id, null, 101L).also {
                boardRepository.updateViewCount(it.id, 10)
                boardRepository.updateLikeCount(it.id, 1)
            }
        board2 =
            boardService.createBoard("제목2", "내용2", "작성자2", testCategory.id, null, 102L).also {
                boardRepository.updateViewCount(it.id, 20)
                boardRepository.updateLikeCount(it.id, 2)
            }
        board3 =
            boardService.createBoard("제목3", "내용3", "작성자1", testCategory.id, null, 101L).also {
                boardRepository.updateViewCount(it.id, 30)
                boardRepository.updateLikeCount(it.id, 3)
            }
        // DB 값 로드
        board1 = boardRepository.findById(board1.id)
        board2 = boardRepository.findById(board2.id)
        board3 = boardRepository.findById(board3.id)
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
    }

    // CounterService Mock 응답 생성을 위한 헬퍼
    private fun createMockCounters(
        board: Board,
        userId: Long?,
        userLiked: Boolean,
    ): CounterService.BoardCounters {
        val finalViewCount = board.viewCount + (if (userId != null) 1 else 0)
        return CounterService.BoardCounters(
            boardId = board.id,
            viewCount = finalViewCount,
            likeCount = board.likeCount,
            userLiked = userLiked,
        )
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("로그인 사용자가 여러 게시글 ID로 조회 성공")
        fun `given logged in user and list of existing boardIds, when execute, then return board items with counters`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val requestedIds = listOf(board1.id, board3.id)
            val dbBoardsWithCounts =
                listOf(
                    Triple(board1.id, board1.viewCount, board1.likeCount),
                    Triple(board3.id, board3.viewCount, board3.likeCount),
                )

            val mockCounters =
                listOf(
                    createMockCounters(board1, testUserId, true),
                    createMockCounters(board3, testUserId, false),
                )
            whenever(
                counterService.getBulkBoardCounters(
                    eq(dbBoardsWithCounts),
                    eq(testUserId.toString()),
                ),
            ).thenReturn(mockCounters)

            val request = GetBoardsByIds.Request(boardIds = requestedIds)

            // When
            val response = getBoardsByIds(request)

            // Then
            assertEquals(2, response.boards.size)

            val boardItem1 = response.boards.find { it.id == board1.id.toString() }!!
            assertNotNull(boardItem1)
            assertEquals(board1.title, boardItem1.title)
            assertEquals(board1.viewCount + 1, boardItem1.viewCount)
            assertEquals(board1.likeCount, boardItem1.likeCount)
            assertTrue(boardItem1.userLiked)

            val boardItem3 = response.boards.find { it.id == board3.id.toString() }!!
            assertNotNull(boardItem3)
            assertEquals(board3.title, boardItem3.title)
            assertEquals(board3.viewCount + 1, boardItem3.viewCount)
            assertEquals(board3.likeCount, boardItem3.likeCount)
            assertFalse(boardItem3.userLiked)

            // Verify interactions
            verify(securityUtils).getCurrentUserId()
            verify(counterService).getBulkBoardCounters(eq(dbBoardsWithCounts), eq(testUserId.toString()))
        }

        @Test
        @DisplayName("비로그인 사용자가 여러 게시글 ID로 조회 성공")
        fun `given guest user and list of existing boardIds, when execute, then return board items without user liked info`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(guestUserId) // 비로그인
            val requestedIds = listOf(board1.id, board2.id)
            val dbBoardsWithCounts =
                listOf(
                    Triple(board1.id, board1.viewCount, board1.likeCount),
                    Triple(board2.id, board2.viewCount, board2.likeCount),
                )

            val mockCounters =
                listOf(
                    createMockCounters(board1, guestUserId, false),
                    createMockCounters(board2, guestUserId, false),
                )
            whenever(
                counterService.getBulkBoardCounters(eq(dbBoardsWithCounts), eq(null)),
            ).thenReturn(mockCounters)

            val request = GetBoardsByIds.Request(boardIds = requestedIds)

            // When
            val response = getBoardsByIds(request)

            // Then
            assertEquals(2, response.boards.size)
            assertTrue(response.boards.all { !it.userLiked })

            // Verify
            verify(securityUtils).getCurrentUserId()
            verify(counterService).getBulkBoardCounters(eq(dbBoardsWithCounts), eq(null))
        }

        @Test
        @DisplayName("존재하지 않는 ID 포함하여 조회 시 존재하는 게시글만 반환")
        fun `given mixed existing and non-existing ids, when execute, then return only existing boards`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val nonExistentId = 999L
            val requestedIds = listOf(board1.id, nonExistentId, board3.id)
            val expectedDbBoardsWithCounts =
                listOf(
                    Triple(board1.id, board1.viewCount, board1.likeCount),
                    Triple(board3.id, board3.viewCount, board3.likeCount),
                )
            val mockCounters =
                listOf(
                    createMockCounters(board1, testUserId, false),
                    createMockCounters(board3, testUserId, false),
                )
            whenever(
                counterService.getBulkBoardCounters(eq(expectedDbBoardsWithCounts), eq(testUserId.toString())),
            ).thenReturn(mockCounters)

            val request = GetBoardsByIds.Request(boardIds = requestedIds)

            // When
            val response = getBoardsByIds(request)

            // Then
            assertEquals(2, response.boards.size)
            assertTrue(response.boards.any { it.id == board1.id.toString() })
            assertTrue(response.boards.any { it.id == board3.id.toString() })

            // Verify
            verify(counterService).getBulkBoardCounters(eq(expectedDbBoardsWithCounts), eq(testUserId.toString()))
        }

        @Test
        @DisplayName("ID 목록이 비어있으면 빈 목록 반환")
        fun `given empty id list, when execute, then return empty list`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val request = GetBoardsByIds.Request(boardIds = emptyList())

            // When & Then
            assertThrows<ConstraintViolationException> { getBoardsByIds(request) }
            verifyNoInteractions(counterService)
        }

        @Test
        @DisplayName("조회된 게시글이 없으면 빈 목록 반환")
        fun `given id list with only non-existing ids, when execute, then return empty list`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val nonExistentIds = listOf(998L, 999L)
            val request = GetBoardsByIds.Request(boardIds = nonExistentIds)

            // When
            val response = getBoardsByIds(request)

            // Then
            assertTrue(response.boards.isEmpty())
            verifyNoInteractions(counterService) // 게시글이 없으므로 카운터 서비스 호출 안됨
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailCases {
        @Test
        @DisplayName("CounterService에서 예외 발생 시 전파")
        fun `given counterService throws exception, when execute, then exception propagates`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val requestedIds = listOf(board1.id, board2.id)
            val dbBoardsWithCounts =
                listOf(
                    Triple(board1.id, board1.viewCount, board1.likeCount),
                    Triple(board2.id, board2.viewCount, board2.likeCount),
                )
            val counterException = RuntimeException("Redis bulk get failed")
            whenever(counterService.getBulkBoardCounters(any(), any())).thenThrow(counterException)

            val request = GetBoardsByIds.Request(boardIds = requestedIds)

            // When & Then
            val thrownException = assertThrows<RuntimeException> { getBoardsByIds(request) }
            assertEquals(counterException, thrownException)
        }

        @Test
        @DisplayName("CounterService 결과 누락 시 AppException(REDIS_OPERATION_FAILED) 발생")
        fun `given counterService returns incomplete data, when execute, then throw AppException`() {
            // Given
            whenever(securityUtils.getCurrentUserId()).thenReturn(testUserId)
            val requestedIds = listOf(board1.id, board2.id) // 2개 요청
            val dbBoardsWithCounts =
                listOf(
                    Triple(board1.id, board1.viewCount, board1.likeCount),
                    Triple(board2.id, board2.viewCount, board2.likeCount),
                )

            // CounterService Mock 설정 (board2 결과 누락)
            val mockCounters = listOf(createMockCounters(board1, testUserId, false))
            whenever(
                counterService.getBulkBoardCounters(eq(dbBoardsWithCounts), eq(testUserId.toString())),
            ).thenReturn(mockCounters) // 1개만 반환

            val request = GetBoardsByIds.Request(boardIds = requestedIds)

            // When & Then
            val exception = assertThrows<AppException> { getBoardsByIds(request) }
            assertEquals(ErrorCode.REDIS_OPERATION_FAILED, exception.errorCode) // UseCase 내부에서 정의된 에러 코드
        }
    }
}
