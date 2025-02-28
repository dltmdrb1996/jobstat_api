package com.example.jobstat.community.usecase

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.comment.service.CommentService
import com.example.jobstat.community.comment.service.CommentServiceImpl
import com.example.jobstat.community.comment.usecase.AddComment
import com.example.jobstat.community.fake.BoardFixture
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.utils.SecurityUtils
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.properties.Delegates
import kotlin.test.assertFailsWith

@DisplayName("AddComment Usecase 테스트")
class AddCommentTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService
    private lateinit var addComment: AddComment
    private lateinit var securityUtils: SecurityUtils
    private lateinit var passwordUtil: FakePasswordUtil
    private var testBoardId by Delegates.notNull<Long>()
    private lateinit var testBoard: Board

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        securityUtils = mock()
        passwordUtil = FakePasswordUtil()
        addComment =
            AddComment(
                commentService,
                securityUtils,
                passwordUtil,
                Validation.buildDefaultValidatorFactory().validator,
            )

        // 테스트용 카테고리 및 게시글 생성
        val category =
            CategoryFixture
                .aCategory()
                .withName("TEST_CAT")
                .withDisplayName("Test Category")
                .withDescription("Test Desc")
                .create()
        categoryRepository.save(category)
        testBoard = BoardFixture.aBoard().withCategory(category).create()
        testBoardId =
            boardService
                .createBoard(
                    title = testBoard.title,
                    content = testBoard.content,
                    author = testBoard.author,
                    categoryId = category.id,
                    password = testBoard.password,
                ).id
    }

    @Nested
    @DisplayName("회원 댓글 작성")
    inner class MemberComment {
        @BeforeEach
        fun setup() {
            whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
        }

        @Test
        @DisplayName("회원이 유효한 요청으로 댓글 작성에 성공한다")
        fun createValidComment() {
            // given
            val request =
                AddComment.ExecuteRequest(
                    boardId = testBoardId,
                    content = "좋은 글이네요!",
                    author = "댓글작성자",
                    password = null,
                )

            // when
            val response = addComment(request)

            // then
            assertNotNull(response.id)
            assertEquals("좋은 글이네요!", response.content)
            assertEquals("댓글작성자", response.author)
            assertEquals(testBoardId, response.boardId)
            assertNotNull(response.createdAt)

            // 게시글의 댓글 수가 증가했는지 확인
            val board = boardRepository.findById(testBoardId)
            assertEquals(1, board.commentCount)
        }

        @Test
        @DisplayName("회원이 여러 댓글을 작성할 수 있다")
        fun createMultipleComments() {
            // given
            val request1 =
                AddComment.ExecuteRequest(
                    boardId = testBoardId,
                    content = "첫 번째 댓글",
                    author = "댓글작성자",
                    password = null,
                )
            val request2 =
                AddComment.ExecuteRequest(
                    boardId = testBoardId,
                    content = "두 번째 댓글",
                    author = "댓글작성자",
                    password = null,
                )

            // when
            addComment(request1)
            addComment(request2)

            // then
            val board = boardRepository.findById(testBoardId)
            assertEquals(2, board.commentCount)
        }
    }

    @Nested
    @DisplayName("비회원 댓글 작성")
    inner class GuestComment {
        @BeforeEach
        fun setup() {
            whenever(securityUtils.getCurrentUserId()).thenReturn(null)
        }

        @Test
        @DisplayName("비회원이 비밀번호와 함께 댓글 작성에 성공한다")
        fun createValidCommentWithPassword() {
            // given
            val request =
                AddComment.ExecuteRequest(
                    boardId = testBoardId,
                    content = "비회원 댓글 내용",
                    author = "게스트",
                    password = "guestpw123",
                )

            // when
            val response = addComment(request)

            // then
            assertNotNull(response.id)
            assertEquals("비회원 댓글 내용", response.content)
            assertEquals("게스트", response.author)
            assertEquals(testBoardId, response.boardId)

            // 게시글의 댓글 수가 증가했는지 확인
            val board = boardRepository.findById(testBoardId)
            assertEquals(1, board.commentCount)
        }

        @Test
        @DisplayName("비회원이 비밀번호 없이 댓글 작성 시 예외가 발생한다")
        fun failCreateCommentWithoutPassword() {
            // given
            val request =
                AddComment.ExecuteRequest(
                    boardId = testBoardId,
                    content = "비회원 댓글 내용",
                    author = "게스트",
                    password = null,
                )

            // when & then
            assertFailsWith<AppException> {
                addComment(request)
            }

            // 게시글의 댓글 수가 증가하지 않았는지 확인
            val board = boardRepository.findById(testBoardId)
            assertEquals(0, board.commentCount)
        }
    }

    @Nested
    @DisplayName("유효성 검사")
    inner class ValidationTest {
        @BeforeEach
        fun setup() {
            whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
        }

        @Test
        @DisplayName("1000자를 초과하는 내용으로 댓글을 작성할 수 없다")
        fun failWithTooLongContent() {
            // given
            val request =
                AddComment.ExecuteRequest(
                    boardId = testBoardId,
                    content = "a".repeat(1001),
                    author = "댓글작성자",
                    password = null,
                )

            // when & then
            val exception =
                assertFailsWith<ConstraintViolationException> {
                    addComment(request)
                }
            assertTrue(
                exception.constraintViolations.any {
                    it.propertyPath.toString() == "content"
                },
            )

            // 게시글의 댓글 수가 증가하지 않았는지 확인
            val board = boardRepository.findById(testBoardId)
            assertEquals(0, board.commentCount)
        }

        @Test
        @DisplayName("작성자 이름이 비어있으면 댓글을 작성할 수 없다")
        fun failWithEmptyAuthor() {
            // given
            val request =
                AddComment.ExecuteRequest(
                    boardId = testBoardId,
                    content = "댓글 내용",
                    author = "",
                    password = null,
                )

            // when & then
            val exception =
                assertFailsWith<ConstraintViolationException> {
                    addComment(request)
                }
            assertTrue(
                exception.constraintViolations.any {
                    it.propertyPath.toString() == "author"
                },
            )

            // 게시글의 댓글 수가 증가하지 않았는지 확인
            val board = boardRepository.findById(testBoardId)
            assertEquals(0, board.commentCount)
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 댓글을 작성할 수 없다")
        fun failWithNonExistentBoard() {
            // given
            val request =
                AddComment.ExecuteRequest(
                    boardId = 999L,
                    content = "댓글 내용",
                    author = "댓글작성자",
                    password = null,
                )

            // when & then
            assertFailsWith<EntityNotFoundException> {
                addComment(request)
            }
        }
    }
}
