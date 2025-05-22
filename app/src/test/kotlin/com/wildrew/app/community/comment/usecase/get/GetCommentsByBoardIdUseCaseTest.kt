package com.wildrew.app.community.comment.usecase.get

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.BoardFixture
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.FakeBoardRepository
import com.wildrew.jobstat.community.board.repository.FakeCategoryRepository
import com.wildrew.jobstat.community.comment.repository.FakeCommentRepository
import com.wildrew.jobstat.community.comment.service.CommentService
import com.wildrew.jobstat.community.comment.service.CommentServiceImpl
import com.wildrew.jobstat.community.comment.utils.CommentConstants
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("GetCommentsByBoardId UseCase 테스트")
class GetCommentsByBoardIdUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository

    private lateinit var commentService: CommentService

    private lateinit var getCommentsByBoardId: GetCommentsByBoardId

    private lateinit var testCategory: BoardCategory
    private lateinit var board1: Board
    private lateinit var board2: Board
    private val defaultPageSize = CommentConstants.DEFAULT_PAGE_SIZE

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        getCommentsByBoardId =
            GetCommentsByBoardId(
                commentService = commentService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        board1 = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
        board2 = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())

        repeat(25) { commentService.createComment(board1.id, "B1 Comment $it", "Author", null, (it + 1).toLong()) }
        repeat(5) { commentService.createComment(board2.id, "B2 Comment $it", "Author", null, (it + 100).toLong()) }
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
        @DisplayName("게시글1의 첫 페이지 댓글 목록 조회")
        fun `given board1 id and page 0, when execute, then return first page of comments for board1`() {
            // Given
            val request = GetCommentsByBoardId.Request(boardId = board1.id, page = 0)

            // When
            val response = getCommentsByBoardId(request)

            // Then
            assertEquals(defaultPageSize, response.items.content.size)
            assertEquals(25, response.totalCount)
            assertTrue(response.hasNext)
            assertTrue(response.items.content.all { it.boardId == board1.id.toString() })
        }

        @Test
        @DisplayName("게시글1의 두 번째 페이지 댓글 목록 조회")
        fun `given board1 id and page 1, when execute, then return second page of comments for board1`() {
            // Given
            val request = GetCommentsByBoardId.Request(boardId = board1.id, page = 1)

            // When
            val response = getCommentsByBoardId(request)

            // Then
            assertEquals(5, response.items.content.size)
            assertEquals(25, response.totalCount)
            assertFalse(response.hasNext)
            assertTrue(response.items.content.all { it.boardId == board1.id.toString() })
        }

        @Test
        @DisplayName("게시글2의 첫 페이지 댓글 목록 조회 (총 5개)")
        fun `given board2 id and page 0, when execute, then return all comments for board2`() {
            // Given
            val request = GetCommentsByBoardId.Request(boardId = board2.id, page = 0)

            // When
            val response = getCommentsByBoardId(request)

            // Then
            assertEquals(5, response.items.content.size)
            assertEquals(5, response.totalCount)
            assertFalse(response.hasNext)
            assertTrue(response.items.content.all { it.boardId == board2.id.toString() })
        }

        @Test
        @DisplayName("댓글 없는 게시글 조회 시 빈 페이지 반환")
        fun `given board with no comments, when execute, then return empty page`() {
            // Given
            val board3 = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
            val request = GetCommentsByBoardId.Request(boardId = board3.id, page = 0)

            // When
            val response = getCommentsByBoardId(request)

            // Then
            assertTrue(response.items.content.isEmpty())
            assertEquals(0, response.totalCount)
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
            val request1 = GetCommentsByBoardId.Request(boardId = 0L, page = 0)
            val request2 = GetCommentsByBoardId.Request(boardId = -1L, page = 0)

            // When & Then
            assertThrows<ConstraintViolationException> { getCommentsByBoardId(request1) }
            assertThrows<ConstraintViolationException> { getCommentsByBoardId(request2) }
        }
    }
}
