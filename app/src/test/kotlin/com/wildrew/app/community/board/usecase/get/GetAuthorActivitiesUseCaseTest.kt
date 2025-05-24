package com.wildrew.app.community.board.usecase.get

import com.wildrew.app.community.board.entity.BoardCategory
import com.wildrew.app.community.board.fixture.CategoryFixture
import com.wildrew.app.community.board.repository.FakeBoardRepository
import com.wildrew.app.community.board.repository.FakeCategoryRepository
import com.wildrew.app.community.board.service.BoardService
import com.wildrew.app.community.board.service.BoardServiceImpl
import com.wildrew.app.community.comment.repository.FakeCommentRepository
import com.wildrew.app.community.comment.service.CommentService
import com.wildrew.app.community.comment.service.CommentServiceImpl
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@DisplayName("GetAuthorActivities UseCase 테스트")
class GetAuthorActivitiesUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository

    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService

    private lateinit var getAuthorActivities: GetAuthorActivities

    private lateinit var testCategory: BoardCategory
    private val targetAuthor = "testAuthor"
    private val otherAuthor = "otherAuthor"

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()

        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)

        getAuthorActivities =
            GetAuthorActivities(
                boardService = boardService,
                commentService = commentService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
    }

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
        commentRepository.clear()
    }

    private fun setupTestData() {
        // targetAuthor의 게시글 12개 생성
        repeat(12) {
            boardService.createBoard("Title $it", "Content $it", targetAuthor, testCategory.id, null, (it + 1).toLong())
        }
        // otherAuthor의 게시글 3개 생성
        repeat(3) {
            boardService.createBoard("Other Title $it", "Other Content $it", otherAuthor, testCategory.id, null, (it + 100).toLong())
        }

        // targetAuthor의 댓글 8개 생성 (각기 다른 게시글에)
        val boards =
            boardRepository
                .findAll(
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                ).content
        repeat(8) { idx ->
            val board = boards[idx % boards.size] // 게시글 순환하며 댓글 달기
            commentService.createComment(board.id, "Comment $idx by $targetAuthor", targetAuthor, null, (idx + 1).toLong())
        }
        // otherAuthor의 댓글 5개 생성
        repeat(5) { idx ->
            val board = boards[idx % boards.size]
            commentService.createComment(board.id, "Comment $idx by $otherAuthor", otherAuthor, null, (idx + 100).toLong())
        }
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessCases {
        @Test
        @DisplayName("특정 작성자의 게시글과 댓글 목록 조회 (첫 페이지)")
        fun `given author and page 0, when execute, then return first page of activities`() {
            // Given
            setupTestData()
            val request = GetAuthorActivities.Request(author = targetAuthor, page = 0)

            // When
            val response = getAuthorActivities(request)

            // Then
            assertEquals(10, response.boards.content.size) // 기본 페이지 크기 10
            assertEquals(12, response.boardsTotalCount)
            assertTrue(response.boardsHasNext) // 다음 페이지 있음

            assertEquals(8, response.comments.content.size) // 댓글은 8개 뿐이므로 첫 페이지에 모두 나옴
            assertEquals(8, response.commentsTotalCount)
            assertFalse(response.commentsHasNext) // 다음 페이지 없음

            assertTrue(response.boards.content.all { it.title.contains("Title") }) // 실제 반환된 DTO 필드 확인
            assertTrue(response.comments.content.all { it.content.contains(targetAuthor) })
        }

        @Test
        @DisplayName("특정 작성자의 게시글과 댓글 목록 조회 (두 번째 페이지)")
        fun `given author and page 1, when execute, then return second page of activities`() {
            // Given
            setupTestData()
            val request = GetAuthorActivities.Request(author = targetAuthor, page = 1)

            // When
            val response = getAuthorActivities(request)

            // Then
            assertEquals(2, response.boards.content.size) // 남은 게시글 2개
            assertEquals(12, response.boardsTotalCount)
            assertFalse(response.boardsHasNext) // 마지막 페이지

            assertEquals(0, response.comments.content.size) // 댓글은 첫 페이지에 다 나옴
            assertEquals(8, response.commentsTotalCount)
            assertFalse(response.commentsHasNext)
        }

        @Test
        @DisplayName("활동 내역이 없는 작성자 조회 시 빈 페이지 반환")
        fun `given author with no activities, when execute, then return empty pages`() {
            // Given
            setupTestData() // 다른 작성자 데이터는 있지만,
            val request = GetAuthorActivities.Request(author = "nonExistentAuthor", page = 0) // 존재하지 않는 작성자

            // When
            val response = getAuthorActivities(request)

            // Then
            assertTrue(response.boards.content.isEmpty())
            assertEquals(0, response.boardsTotalCount)
            assertFalse(response.boardsHasNext)

            assertTrue(response.comments.content.isEmpty())
            assertEquals(0, response.commentsTotalCount)
            assertFalse(response.commentsHasNext)
        }
    }

    @Nested
    @DisplayName("실패 케이스 - 유효성 검사")
    inner class ValidationFailCases {
        @Test
        @DisplayName("작성자 이름이 비어있으면 ConstraintViolationException 발생")
        fun `given blank author, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetAuthorActivities.Request(author = " ", page = 0)

            // When & Then
            assertThrows<ConstraintViolationException> { getAuthorActivities(request) }
        }
    }
}
