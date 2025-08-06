package com.wildrew.jobstat.community.comment.usecase.get

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
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils // Mock 대상
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.*

@DisplayName("GetCommentDetail UseCase 테스트")
class GetCommentDetailUseCaseTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository

    private lateinit var commentService: CommentService

    private lateinit var theadContextUtils: TheadContextUtils

    private lateinit var getCommentDetail: GetCommentDetail

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private lateinit var testComment: Comment
    private val testUserId = 1L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()

        commentService = CommentServiceImpl(commentRepository, boardRepository)

        theadContextUtils = mock()

        getCommentDetail =
            GetCommentDetail(
                commentService = commentService,
                theadContextUtils = theadContextUtils,
                validator = Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
        testComment =
            commentService.createComment(
                boardId = testBoard.id,
                content = "테스트 댓글 내용",
                author = "테스터",
                password = null,
                userId = testUserId,
            )
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
        @DisplayName("존재하는 댓글 ID로 상세 조회 성공")
        fun `given existing commentId, when execute, then return comment details`() {
            // Given
            val request = GetCommentDetail.Request(commentId = testComment.id)

            // When
            val response = getCommentDetail(request)

            // Then
            assertEquals(testComment.id.toString(), response.id)
            assertEquals(testBoard.id.toString(), response.boardId)
            assertEquals(testComment.userId, response.userId)
            assertEquals(testComment.content, response.content)
            assertEquals(testComment.author, response.author)
            assertEquals(testComment.createdAt, response.createdAt)
            assertEquals(testComment.updatedAt, response.updatedAt)
            assertNotNull(response.eventTs)

            verify(theadContextUtils).getCurrentUserIdOrNull()
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailCases {
        @Test
        @DisplayName("존재하지 않는 댓글 ID 조회 시 EntityNotFoundException 발생")
        fun `given non-existent commentId, when execute, then throw EntityNotFoundException`() {
            // Given
            val nonExistentCommentId = 999L
            val request = GetCommentDetail.Request(commentId = nonExistentCommentId)

            // When & Then
            assertThrows<EntityNotFoundException> { getCommentDetail(request) }
            verifyNoInteractions(theadContextUtils)
        }

        @Test
        @DisplayName("댓글 ID가 양수가 아니면 ConstraintViolationException 발생")
        fun `given non-positive commentId, when execute, then throw ConstraintViolationException`() {
            // Given
            val request1 = GetCommentDetail.Request(commentId = 0L)
            val request2 = GetCommentDetail.Request(commentId = -1L)

            // When & Then
            assertThrows<ConstraintViolationException> { getCommentDetail(request1) }
            assertThrows<ConstraintViolationException> { getCommentDetail(request2) }
            verifyNoInteractions(theadContextUtils)
        }
    }
}
