package com.wildrew.app.community.comment.usecase.get

import com.wildrew.app.community.board.entity.Board
import com.wildrew.app.community.board.entity.BoardCategory
import com.wildrew.app.community.board.fixture.BoardFixture
import com.wildrew.app.community.board.fixture.CategoryFixture
import com.wildrew.app.community.board.repository.FakeBoardRepository
import com.wildrew.app.community.board.repository.FakeCategoryRepository
import com.wildrew.app.community.comment.repository.FakeCommentRepository
import com.wildrew.app.community.comment.service.CommentService
import com.wildrew.app.community.comment.service.CommentServiceImpl
import com.wildrew.app.community.comment.utils.CommentConstants
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("GetCommentsByAuthor UseCase 테스트")
class GetCommentsByAuthorUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository

    private lateinit var commentService: CommentService

    private lateinit var getCommentsByAuthor: GetCommentsByAuthor

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private val targetAuthor = "targetAuthor"
    private val otherAuthor = "otherAuthor"
    private val defaultPageSize = CommentConstants.DEFAULT_PAGE_SIZE // 20 가정

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        getCommentsByAuthor =
            GetCommentsByAuthor(
                commentService = commentService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())

        repeat(25) { commentService.createComment(testBoard.id, "Comment $it", targetAuthor, null, (it + 1).toLong()) }
        repeat(5) { commentService.createComment(testBoard.id, "Other Comment $it", otherAuthor, null, (it + 100).toLong()) }
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
        @DisplayName("특정 작성자의 첫 페이지 댓글 목록 조회")
        fun `given target author and page 0, when execute, then return first page of comments`() {
            // Given
            val request = GetCommentsByAuthor.Request(author = targetAuthor, page = 0)

            // When
            val response = getCommentsByAuthor(request)

            // Then
            assertEquals(defaultPageSize, response.items.content.size)
            assertEquals(25, response.totalCount)
            assertTrue(response.hasNext) // 다음 페이지 있음
            assertTrue(response.items.content.all { it.author == targetAuthor })
        }

        @Test
        @DisplayName("특정 작성자의 두 번째 페이지 댓글 목록 조회")
        fun `given target author and page 1, when execute, then return second page of comments`() {
            // Given
            val request = GetCommentsByAuthor.Request(author = targetAuthor, page = 1)

            // When
            val response = getCommentsByAuthor(request)

            // Then
            assertEquals(5, response.items.content.size)
            assertEquals(25, response.totalCount)
            assertFalse(response.hasNext) // 마지막 페이지
            assertTrue(response.items.content.all { it.author == targetAuthor })
        }

        @Test
        @DisplayName("댓글 없는 작성자 조회 시 빈 페이지 반환")
        fun `given author with no comments, when execute, then return empty page`() {
            // Given
            val request = GetCommentsByAuthor.Request(author = "noCommentAuthor", page = 0)

            // When
            val response = getCommentsByAuthor(request)

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
        @DisplayName("작성자 이름 비어있으면 ConstraintViolationException 발생")
        fun `given blank author, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetCommentsByAuthor.Request(author = " ", page = 0)

            // When & Then
            assertThrows<ConstraintViolationException> { getCommentsByAuthor(request) }
        }
    }
}
