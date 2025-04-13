//package com.example.jobstat.community.usecase
//
//import com.example.jobstat.community.board.service.BoardService
//import com.example.jobstat.community.board.service.BoardServiceImpl
//import com.example.jobstat.comment.service.CommentService
//import com.example.jobstat.comment.service.CommentServiceImpl
//import com.example.jobstat.comment.usecase.DeleteComment
//import com.example.jobstat.community.fake.BoardFixture
//import com.example.jobstat.community.fake.CategoryFixture
//import com.example.jobstat.community.fake.repository.FakeBoardRepository
//import com.example.jobstat.community.fake.repository.FakeCategoryRepository
//import com.example.jobstat.community.fake.repository.FakeCommentRepository
//import com.example.jobstat.core.error.AppException
//import com.example.jobstat.core.global.utils.SecurityUtils
//import com.example.jobstat.utils.FakePasswordUtil
//import jakarta.persistence.EntityNotFoundException
//import jakarta.validation.Validation
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//import org.mockito.kotlin.mock
//import org.mockito.kotlin.whenever
//import kotlin.properties.Delegates
//import kotlin.test.assertFailsWith
//import kotlin.test.assertTrue
//
//@DisplayName("DeleteComment Usecase 테스트")
//class DeleteCommentTest {
//    private lateinit var boardRepository: FakeBoardRepository
//    private lateinit var categoryRepository: FakeCategoryRepository
//    private lateinit var commentRepository: FakeCommentRepository
//    private lateinit var boardService: BoardService
//    private lateinit var commentService: CommentService
//    private lateinit var deleteComment: DeleteComment
//    private lateinit var securityUtils: SecurityUtils
//    private lateinit var passwordUtil: FakePasswordUtil
//    private var testBoardId by Delegates.notNull<Long>()
//
//    @BeforeEach
//    fun setUp() {
//        boardRepository = FakeBoardRepository()
//        categoryRepository = FakeCategoryRepository()
//        commentRepository = FakeCommentRepository()
//        boardService = BoardServiceImpl(boardRepository, categoryRepository)
//        commentService = CommentServiceImpl(commentRepository, boardRepository)
//        securityUtils = mock() // 모의 객체 생성
//        passwordUtil = FakePasswordUtil()
//        deleteComment = DeleteComment(commentService, passwordUtil, securityUtils, Validation.buildDefaultValidatorFactory().validator)
//
//        // 게시글 생성 (게스트 게시글: 비밀번호가 설정됨)
//        val category =
//            CategoryFixture
//                .aCategory()
//                .withName("CAT1")
//                .withDisplayName("Category 1")
//                .withDescription("Desc 1")
//                .create()
//        categoryRepository.save(category)
//        val board = BoardFixture.aBoard().withCategory(category).create()
//        testBoardId =
//            boardService
//                .createBoard(
//                    title = board.title,
//                    content = board.content,
//                    author = board.author,
//                    categoryId = category.id,
//                    password = passwordUtil.encode("1234"),
//                ).id
//    }
//
//    @Nested
//    @DisplayName("회원 댓글 삭제")
//    inner class MemberComment {
//        @BeforeEach
//        fun setup() {
//            whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
//        }
//
//        @Test
//        @DisplayName("회원이 자신의 댓글을 삭제할 수 있다")
//        fun deleteOwnComment() {
//            // given
//            val comment =
//                commentService.createComment(
//                    boardId = testBoardId,
//                    content = "Member Comment",
//                    author = "testUser",
//                    password = null,
//                    userId = 1L,
//                )
//            assertEquals(1, boardRepository.findById(testBoardId).commentCount)
//
//            // when
//            val request = DeleteComment.Request(password = null)
//            val response = deleteComment(request.of(testBoardId, comment.id))
//
//            // then
//            assertTrue(response.success)
//            assertFailsWith<EntityNotFoundException> { commentService.getCommentById(comment.id) }
//            assertEquals(0, boardRepository.findById(testBoardId).commentCount)
//        }
//
//        @Test
//        @DisplayName("회원이 다른 사용자의 댓글을 삭제할 수 없다")
//        fun cannotDeleteOthersComment() {
//            // given
//            val comment =
//                commentService.createComment(
//                    boardId = testBoardId,
//                    content = "Other's Comment",
//                    author = "otherUser",
//                    password = null,
//                    userId = 2L, // 다른 사용자의 ID
//                )
//            assertEquals(1, boardRepository.findById(testBoardId).commentCount)
//
//            // when & then
//            val request = DeleteComment.Request(password = null)
//            assertFailsWith<IllegalArgumentException> {
//                deleteComment(request.of(testBoardId, comment.id))
//            }
//            assertEquals(1, boardRepository.findById(testBoardId).commentCount)
//        }
//    }
//
//    @Nested
//    @DisplayName("비회원 댓글 삭제")
//    inner class GuestComment {
//        @BeforeEach
//        fun setup() {
//            whenever(securityUtils.getCurrentUserId()).thenReturn(null)
//        }
//
//        @Test
//        @DisplayName("비회원이 올바른 비밀번호로 댓글을 삭제할 수 있다")
//        fun deleteWithCorrectPassword() {
//            // given
//            val encodedPassword = passwordUtil.encode("pass123")
//            val comment =
//                commentService.createComment(
//                    boardId = testBoardId,
//                    content = "Guest Comment",
//                    author = "guest",
//                    password = encodedPassword,
//                    userId = null,
//                )
//            assertEquals(1, boardRepository.findById(testBoardId).commentCount)
//
//            // when
//            val request = DeleteComment.Request(password = "pass123")
//            val response = deleteComment(request.of(testBoardId, comment.id))
//
//            // then
//            assertTrue(response.success)
//            assertFailsWith<EntityNotFoundException> { commentService.getCommentById(comment.id) }
//            assertEquals(0, boardRepository.findById(testBoardId).commentCount)
//        }
//
//        @Test
//        @DisplayName("비회원이 잘못된 비밀번호로 댓글을 삭제할 수 없다")
//        fun cannotDeleteWithWrongPassword() {
//            // given
//            val encodedPassword = passwordUtil.encode("pass123")
//            val comment =
//                commentService.createComment(
//                    boardId = testBoardId,
//                    content = "Guest Comment",
//                    author = "guest",
//                    password = encodedPassword,
//                    userId = null,
//                )
//            assertEquals(1, boardRepository.findById(testBoardId).commentCount)
//
//            // when & then
//            val request = DeleteComment.Request(password = "wrongpass")
//            assertFailsWith<AppException> {
//                deleteComment(request.of(testBoardId, comment.id))
//            }
//            assertEquals(1, boardRepository.findById(testBoardId).commentCount)
//        }
//    }
//
//    @Nested
//    @DisplayName("다중 게시글 댓글 삭제")
//    inner class MultipleBoards {
//        @Test
//        @DisplayName("여러 게시글의 댓글 삭제가 독립적으로 동작한다")
//        fun deleteCommentsIndependently() {
//            // given
//            whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
//
//            val board2 =
//                boardService.createBoard(
//                    title = "Second Board",
//                    content = "Content",
//                    author = "Author",
//                    categoryId = testBoardId,
//                    password = null,
//                )
//
//            val comment1 =
//                commentService.createComment(
//                    boardId = testBoardId,
//                    content = "First board comment",
//                    author = "user",
//                    password = null,
//                    userId = 1L,
//                )
//
//            val comment2 =
//                commentService.createComment(
//                    boardId = board2.id,
//                    content = "Second board comment",
//                    author = "user",
//                    password = null,
//                    userId = 1L,
//                )
//
//            assertEquals(1, boardRepository.findById(testBoardId).commentCount)
//            assertEquals(1, boardRepository.findById(board2.id).commentCount)
//
//            // when
//            val request = DeleteComment.Request(password = null)
//            deleteComment(request.of(testBoardId, comment1.id))
//
//            // then
//            assertEquals(0, boardRepository.findById(testBoardId).commentCount)
//            assertEquals(1, boardRepository.findById(board2.id).commentCount)
//        }
//    }
//
//    @Nested
//    @DisplayName("유효성 검사")
//    inner class ValidationTest {
//        @Test
//        @DisplayName("존재하지 않는 댓글은 삭제할 수 없다")
//        fun cannotDeleteNonExistentComment() {
//            // given
//            whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
//            val request = DeleteComment.Request(password = null)
//
//            // when & then
//            assertFailsWith<EntityNotFoundException> {
//                deleteComment(request.of(testBoardId, 999L))
//            }
//        }
//    }
//}
