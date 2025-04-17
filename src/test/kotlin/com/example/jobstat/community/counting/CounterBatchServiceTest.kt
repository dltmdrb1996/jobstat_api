package com.example.jobstat.community.counting

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.fixture.BoardFixture
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.event.CommunityCommandEventPublisher
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.*
import java.time.LocalDateTime

@DisplayName("CounterBatchService 단위 테스트 (Mockito)")
class CounterBatchServiceTest {
    private lateinit var counterBatchService: CounterBatchService
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var communityCommandEventPublisher: CommunityCommandEventPublisher

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        communityCommandEventPublisher = mock<CommunityCommandEventPublisher>()
        counterBatchService = CounterBatchService(boardRepository, communityCommandEventPublisher)
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
    }

    private fun createAndSaveBoard(
        id: Long,
        initialViewCount: Int = 0,
        initialLikeCount: Int = 0,
    ): Board {
        val board =
            BoardFixture
                .aBoard()
                .withId(id)
                .withCategory(CategoryFixture.aCategory().create())
                .create()
        board.reflectivelySetField("viewCount", initialViewCount)
        board.reflectivelySetField("likeCount", initialLikeCount)
        return boardRepository.save(board)
    }

    private fun Any.reflectivelySetField(
        fieldName: String,
        value: Any,
    ) {
        try {
            this::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(this@reflectivelySetField, value)
            }
        } catch (e: NoSuchFieldException) {
            var superClass = this::class.java.superclass
            while (superClass != null) {
                try {
                    superClass.getDeclaredField(fieldName).apply {
                        isAccessible = true
                        set(this@reflectivelySetField, value)
                    }
                    return
                } catch (e2: NoSuchFieldException) {
                    superClass = superClass.superclass
                }
            }
            throw e
        }
    }

    @Nested
    @DisplayName("DB 카운터 처리 (processSingleBoardCounter)")
    inner class ProcessSingleBoardCounter {
        @Test
        @DisplayName("성공: 조회수와 좋아요 수가 모두 0이면 DB 업데이트 없이 true 반환")
        fun `given zero counts, when processSingleBoardCounter, then return true without db interaction`() {
            val boardId = 1L
            val viewCountDelta = 0
            val likeCountDelta = 0

            val result = counterBatchService.processSingleBoardCounter(boardId, viewCountDelta, likeCountDelta)

            assertTrue(result)
            assertThrows<EntityNotFoundException> { boardRepository.findById(boardId) }
            verify(communityCommandEventPublisher, never()).publishBoardViewed(
                anyLong(),
                any<LocalDateTime>(),
                anyInt(),
                anyLong(),
            )
        }

        @Test
        @DisplayName("성공: 조회수만 증가 시 DB 업데이트 및 이벤트 발행 후 true 반환")
        fun `given positive view count delta, when processSingleBoardCounter, then updates db, publishes event, returns true`() {
            val boardId = 1L
            val initialViewCount = 10
            val initialLikeCount = 5
            val viewCountDelta = 3
            val likeCountDelta = 0
            val board = createAndSaveBoard(boardId, initialViewCount, initialLikeCount)

            assertNotNull(board.createdAt)

            val result = counterBatchService.processSingleBoardCounter(boardId, viewCountDelta, likeCountDelta)

            assertTrue(result)
            val updatedBoard = boardRepository.findById(boardId)
            assertEquals(initialViewCount + viewCountDelta, updatedBoard.viewCount)
            assertEquals(initialLikeCount + likeCountDelta, updatedBoard.likeCount)

            verify(communityCommandEventPublisher, times(1)).publishBoardViewed(
                eq(boardId),
                eq(board.createdAt),
                eq(initialViewCount + viewCountDelta),
                anyLong(),
            )
        }

        @Test
        @DisplayName("성공: 좋아요만 증가 시 DB 업데이트 후 true 반환 (조회 이벤트 발행 안함)")
        fun `given positive like count delta, when processSingleBoardCounter, then updates db, returns true without view event`() {
            val boardId = 2L
            val initialViewCount = 10
            val initialLikeCount = 5
            val viewCountDelta = 0
            val likeCountDelta = 2
            createAndSaveBoard(boardId, initialViewCount, initialLikeCount)

            val result = counterBatchService.processSingleBoardCounter(boardId, viewCountDelta, likeCountDelta)

            assertTrue(result)
            val updatedBoard = boardRepository.findById(boardId)
            assertEquals(initialViewCount + viewCountDelta, updatedBoard.viewCount)
            assertEquals(initialLikeCount + likeCountDelta, updatedBoard.likeCount)

            verify(communityCommandEventPublisher, never()).publishBoardViewed(
                anyLong(),
                any<LocalDateTime>(),
                anyInt(),
                anyLong(),
            )
        }

        @Test
        @DisplayName("성공: 조회수/좋아요 모두 증가 시 DB 업데이트 및 이벤트 발행 후 true 반환")
        fun `given positive view and like counts, when processSingleBoardCounter, then updates db, publishes event, returns true`() {
            val boardId = 3L
            val initialViewCount = 20
            val initialLikeCount = 15
            val viewCountDelta = 5
            val likeCountDelta = 3
            val board = createAndSaveBoard(boardId, initialViewCount, initialLikeCount)
            assertNotNull(board.createdAt)

            val result = counterBatchService.processSingleBoardCounter(boardId, viewCountDelta, likeCountDelta)

            assertTrue(result)
            val updatedBoard = boardRepository.findById(boardId)
            assertEquals(initialViewCount + viewCountDelta, updatedBoard.viewCount)
            assertEquals(initialLikeCount + likeCountDelta, updatedBoard.likeCount)

            verify(communityCommandEventPublisher, times(1)).publishBoardViewed(
                eq(boardId),
                eq(board.createdAt),
                eq(initialViewCount + viewCountDelta),
                anyLong(),
            )
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게시글 ID 처리 시 false 반환")
        fun `given non-existent boardId, when processSingleBoardCounter, then returns false`() {
            val nonExistentBoardId = 99L
            val viewCountDelta = 1
            val likeCountDelta = 1

            val result = counterBatchService.processSingleBoardCounter(nonExistentBoardId, viewCountDelta, likeCountDelta)

            assertFalse(result)
            verify(communityCommandEventPublisher, never()).publishBoardViewed(
                anyLong(),
                any<LocalDateTime>(),
                anyInt(),
                anyLong(),
            )
        }

        @Test
        @DisplayName("실패: DB 조회 중 예외 발생 시 false 반환")
        fun `given db error on findById, when processSingleBoardCounter, then returns false`() {
            val boardId = 5L
            val viewCountDelta = 1
            val likeCountDelta = 1

            val result = counterBatchService.processSingleBoardCounter(boardId, viewCountDelta, likeCountDelta)
            assertFalse(result)

            verify(communityCommandEventPublisher, never()).publishBoardViewed(
                anyLong(),
                any<LocalDateTime>(),
                anyInt(),
                anyLong(),
            )
        }

        @Test
        @DisplayName("예외: 이벤트 발행 중 예외 발생해도 DB 업데이트는 롤백되지 않고 false 반환 (REQUIRES_NEW)")
        fun `given event publisher error, when processSingleBoardCounter, then returns false but db changes persist (ideally)`() {
            val boardId = 6L
            val initialViewCount = 5
            val initialLikeCount = 2
            val viewCountDelta = 1
            val likeCountDelta = 1
            val board = createAndSaveBoard(boardId, initialViewCount, initialLikeCount)
            assertNotNull(board.createdAt)

            doThrow(RuntimeException("카프카 에러")).whenever(communityCommandEventPublisher).publishBoardViewed(
                anyLong(),
                any<LocalDateTime>(),
                anyInt(),
                anyLong(),
            )

            val result = counterBatchService.processSingleBoardCounter(boardId, viewCountDelta, likeCountDelta)

            assertFalse(result)

            val updatedBoard = boardRepository.findById(boardId)
            assertEquals(initialViewCount + viewCountDelta, updatedBoard.viewCount)
            assertEquals(initialLikeCount + likeCountDelta, updatedBoard.likeCount)

            verify(communityCommandEventPublisher, times(1)).publishBoardViewed(
                eq(boardId),
                eq(board.createdAt),
                eq(initialViewCount + viewCountDelta),
                anyLong(),
            )
        }
    }
}
