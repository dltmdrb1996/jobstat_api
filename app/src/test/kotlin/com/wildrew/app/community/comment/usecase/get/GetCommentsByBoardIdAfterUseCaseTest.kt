package com.wildrew.app.community.comment.usecase.get

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.BoardFixture
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.repository.FakeCommentRepository
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.community.comment.service.CommentServiceImpl
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("GetCommentsByBoardIdAfter UseCase 테스트")
class GetCommentsByBoardIdAfterUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository

    private lateinit var commentService: CommentService

    private lateinit var getCommentsByBoardIdAfter: GetCommentsByBoardIdAfter

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private val testLimit = 5
    private lateinit var commentIdsDesc: List<Long>

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        getCommentsByBoardIdAfter =
            GetCommentsByBoardIdAfter(
                commentService = commentService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
        val createdComments = mutableListOf<Comment>()
        repeat(12) { createdComments.add(commentService.createComment(testBoard.id, "Comment $it", "Author", null, (it + 1).toLong())) }
        commentIdsDesc = createdComments.map { it.id }.sortedDescending()
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
        commentRepository.clear()
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("첫 페이지 조회 (lastCommentId = null)")
        fun `given boardId and null cursor, when execute, then return first page comments`() {
            // Given
            val request = GetCommentsByBoardIdAfter.Request(boardId = testBoard.id, lastCommentId = null, limit = testLimit)
            val expectedIds = commentIdsDesc.take(testLimit).map { it.toString() }

            // When
            val response = getCommentsByBoardIdAfter(request)

            // Then
            assertEquals(expectedIds, response.items.map { it.id })
            assertEquals(testLimit, response.items.size)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        fun `given boardId and cursor from first page, when execute, then return second page comments`() {
            // Given
            val lastId = commentIdsDesc[testLimit - 1]
            val request = GetCommentsByBoardIdAfter.Request(boardId = testBoard.id, lastCommentId = lastId, limit = testLimit)
            val expectedIds = commentIdsDesc.drop(testLimit).take(testLimit).map { it.toString() }

            // When
            val response = getCommentsByBoardIdAfter(request)

            // Then
            assertEquals(expectedIds, response.items.map { it.id })
            assertEquals(testLimit, response.items.size)
            assertTrue(response.hasNext)
        }

        @Test
        @DisplayName("마지막 페이지 조회")
        fun `given boardId and cursor from second page, when execute, then return last page comments`() {
            // Given
            val lastId = commentIdsDesc[testLimit * 2 - 1]
            val request = GetCommentsByBoardIdAfter.Request(boardId = testBoard.id, lastCommentId = lastId, limit = testLimit)
            val expectedIds = commentIdsDesc.drop(testLimit * 2).take(testLimit).map { it.toString() }

            // When
            val response = getCommentsByBoardIdAfter(request)

            // Then
            assertEquals(expectedIds, response.items.map { it.id })
            assertEquals(2, response.items.size)
            assertFalse(response.hasNext)
        }

        @Test
        @DisplayName("댓글 없는 게시글 조회 시 빈 목록 반환")
        fun `given board with no comments, when execute, then return empty list`() {
            // Given
            val board2 = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
            val request = GetCommentsByBoardIdAfter.Request(boardId = board2.id, lastCommentId = null, limit = testLimit)

            // When
            val response = getCommentsByBoardIdAfter(request)

            // Then
            assertTrue(response.items.isEmpty())
            assertFalse(response.hasNext)
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailCases {
        @Test
        @DisplayName("게시글 ID가 0 이하면 ConstraintViolationException 발생")
        fun `given non-positive boardId, when execute, then throw ConstraintViolationException`() {
            // Given
            val request1 = GetCommentsByBoardIdAfter.Request(boardId = 0L, lastCommentId = null, limit = 5)
            val request2 = GetCommentsByBoardIdAfter.Request(boardId = -1L, lastCommentId = null, limit = 5)

            // When & Then
            assertThrows<ConstraintViolationException> { getCommentsByBoardIdAfter(request1) }
            assertThrows<ConstraintViolationException> { getCommentsByBoardIdAfter(request2) }
        }
    }
}
