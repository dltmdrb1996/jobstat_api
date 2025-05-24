package com.wildrew.app.community.comment.usecase.get

import com.wildrew.app.community.board.entity.Board
import com.wildrew.app.community.board.entity.BoardCategory
import com.wildrew.app.community.board.fixture.BoardFixture
import com.wildrew.app.community.board.fixture.CategoryFixture
import com.wildrew.app.community.board.repository.FakeBoardRepository
import com.wildrew.app.community.board.repository.FakeCategoryRepository
import com.wildrew.app.community.comment.entity.Comment
import com.wildrew.app.community.comment.repository.FakeCommentRepository
import com.wildrew.app.community.comment.service.CommentService
import com.wildrew.app.community.comment.service.CommentServiceImpl
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("GetCommentsByIds UseCase 테스트")
class GetCommentsByIdsUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository

    private lateinit var commentService: CommentService

    private lateinit var getCommentsByIds: GetCommentsByIds

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private lateinit var comment1: Comment
    private lateinit var comment2: Comment
    private lateinit var comment3: Comment

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        getCommentsByIds =
            GetCommentsByIds(
                commentService = commentService,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
        comment1 = commentService.createComment(testBoard.id, "댓글 1", "작성자1", null, 1L)
        comment2 = commentService.createComment(testBoard.id, "댓글 2", "작성자2", "pw12", null) // 비회원
        comment3 = commentService.createComment(testBoard.id, "댓글 3", "작성자1", null, 1L)
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
        @DisplayName("존재하는 여러 댓글 ID로 조회 성공")
        fun `given list of existing commentIds, when execute, then return corresponding comment items`() {
            // Given
            val requestedIds = listOf(comment1.id, comment3.id)
            val request = GetCommentsByIds.Request(commentIds = requestedIds)

            // When
            val response = getCommentsByIds(request)

            // Then
            assertEquals(2, response.comments.size)
            assertTrue(response.comments.any { it.id == comment1.id.toString() && it.content == comment1.content })
            assertTrue(response.comments.any { it.id == comment3.id.toString() && it.content == comment3.content })
            assertFalse(response.comments.any { it.id == comment2.id.toString() }) // 요청 안 된 ID는 없는지 확인
        }

        @Test
        @DisplayName("존재하지 않는 ID 포함 시 존재하는 댓글만 반환")
        fun `given mixed existing and non-existing ids, when execute, then return only existing comment items`() {
            // Given
            val nonExistentId = 999L
            val requestedIds = listOf(comment2.id, nonExistentId)
            val request = GetCommentsByIds.Request(commentIds = requestedIds)

            // When
            val response = getCommentsByIds(request)

            // Then
            assertEquals(1, response.comments.size) // 존재하는 comment2만 반환
            assertEquals(comment2.id.toString(), response.comments[0].id)
            assertEquals(comment2.content, response.comments[0].content)
        }

        @Test
        @DisplayName("모두 존재하지 않는 ID 목록 조회 시 빈 목록 반환")
        fun `given list with only non-existing ids, when execute, then return empty list`() {
            // Given
            val requestedIds = listOf(998L, 999L)
            val request = GetCommentsByIds.Request(commentIds = requestedIds)

            // When
            val response = getCommentsByIds(request)

            // Then
            assertTrue(response.comments.isEmpty())
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailCases {
        @Test
        @DisplayName("빈 ID 목록 요청 시 ConstraintViolationException 발생")
        fun `given empty id list, when execute, then throw ConstraintViolationException`() {
            // Given
            val request = GetCommentsByIds.Request(commentIds = emptyList())

            // When & Then
            assertThrows<ConstraintViolationException> { getCommentsByIds(request) }
        }
    }
}
