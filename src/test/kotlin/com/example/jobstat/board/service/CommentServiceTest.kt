package com.example.jobstat.board.service

import com.example.jobstat.board.fake.BoardFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.entity.Board
import com.example.jobstat.board.internal.service.CommentService
import com.example.jobstat.board.internal.service.CommentServiceImpl
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest
import kotlin.test.assertFailsWith

@DisplayName("CommentService 테스트")
class CommentServiceTest {
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var commentService: CommentService
    private lateinit var testBoard: Board

    @BeforeEach
    fun setUp() {
        commentRepository = FakeCommentRepository()
        boardRepository = FakeBoardRepository()
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        // 테스트용 게시글 생성 – Fixture 사용
        testBoard = boardRepository.save(BoardFixture.aBoard().create())
    }

    @Nested
    @DisplayName("댓글 생성")
    inner class CreateComment {
        @Test
        @DisplayName("유효한 정보로 댓글을 생성할 수 있다")
        fun createValidComment() {
            val createdComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "테스트 댓글",
                    author = "테스트사용자",
                    password = null,
                )
            assertEquals("테스트 댓글", createdComment.content)
            assertEquals("테스트사용자", createdComment.author)
            assertEquals(testBoard.id, createdComment.board.id)
            assertNull(createdComment.password)
        }

        @Test
        @DisplayName("비밀댓글로 생성할 수 있다")
        fun createPasswordProtectedComment() {
            val createdComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "비밀 댓글",
                    author = "테스트사용자",
                    password = "testpassword123!",
                )
            assertNotNull(createdComment.password)
            assertEquals("testpassword123!", createdComment.password)
        }

        @Test
        @DisplayName("존재하지 않는 게시글에는 댓글을 생성할 수 없다")
        fun cannotCreateCommentOnNonExistentBoard() {
            assertFailsWith<EntityNotFoundException> {
                commentService.createComment(
                    boardId = 999L,
                    content = "Test Comment",
                    author = "testUser",
                    password = null,
                )
            }
        }

        @Test
        @DisplayName("1000자를 초과하는 내용으로 댓글을 생성할 수 없다")
        fun cannotCreateCommentWithTooLongContent() {
            val tooLongContent = "a".repeat(1001)
            assertFailsWith<IllegalArgumentException> {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = tooLongContent,
                    author = "testUser",
                    password = null,
                )
            }
        }

        @Test
        @DisplayName("빈 내용으로 댓글을 생성할 수 없다")
        fun cannotCreateCommentWithEmptyContent() {
            assertFailsWith<IllegalArgumentException> {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "",
                    author = "testUser",
                    password = null,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "   ",
                    author = "testUser",
                    password = null,
                )
            }
        }

        @Test
        @DisplayName("작성자 없이 댓글을 생성할 수 없다")
        fun cannotCreateCommentWithoutAuthor() {
            assertFailsWith<IllegalArgumentException> {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Test Comment",
                    author = "",
                    password = null,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Test Comment",
                    author = "   ",
                    password = null,
                )
            }
        }
    }

    @Nested
    @DisplayName("댓글 조회")
    inner class GetComment {
        @Test
        @DisplayName("ID로 댓글을 조회할 수 있다")
        fun getCommentById() {
            val savedComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Test Comment",
                    author = "testUser",
                    password = null,
                )
            val foundComment = commentService.getCommentById(savedComment.id)
            assertEquals(savedComment.id, foundComment.id)
            assertEquals(savedComment.content, foundComment.content)
            assertEquals(savedComment.author, foundComment.author)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun getCommentByNonExistentId() {
            assertFailsWith<EntityNotFoundException> {
                commentService.getCommentById(999L)
            }
        }

        @Test
        @DisplayName("게시글 ID로 댓글을 조회할 수 있다")
        fun getCommentsByBoardId() {
            repeat(3) { idx ->
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Comment $idx",
                    author = "testUser",
                    password = null,
                )
            }
            val comments = commentService.getCommentsByBoardId(testBoard.id, PageRequest.of(0, 10))
            assertEquals(3, comments.content.size)
            assertTrue(comments.content.all { it.board.id == testBoard.id })
        }

        @Test
        @DisplayName("작성자로 댓글을 조회할 수 있다")
        fun getCommentsByAuthor() {
            repeat(2) {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Comment",
                    author = "testUser",
                    password = null,
                )
            }
            commentService.createComment(
                boardId = testBoard.id,
                content = "Other Comment",
                author = "otherUser",
                password = null,
            )
            val comments = commentService.getCommentsByAuthor("testUser", PageRequest.of(0, 10))
            assertEquals(2, comments.content.size)
            assertTrue(comments.content.all { it.author == "testUser" })
        }

        @Test
        @DisplayName("게시글 ID와 작성자로 댓글을 조회할 수 있다")
        fun getCommentsByBoardIdAndAuthor() {
            // testBoard에 댓글 2개
            repeat(2) {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Comment $it",
                    author = "testUser",
                    password = null,
                )
            }
            // 다른 게시글에 댓글 1개
            val board2 = boardRepository.save(BoardFixture.aBoard().create())
            commentService.createComment(
                boardId = board2.id,
                content = "Other Board Comment",
                author = "testUser",
                password = null,
            )
            // 다른 작성자의 댓글
            commentService.createComment(
                boardId = testBoard.id,
                content = "Other User Comment",
                author = "otherUser",
                password = null,
            )
            val comments = commentService.getCommentsByBoardIdAndAuthor(testBoard.id, "testUser", PageRequest.of(0, 10))
            assertEquals(2, comments.content.size)
            assertTrue(comments.content.all { it.board.id == testBoard.id && it.author == "testUser" })
        }

        @Test
        @DisplayName("최근 댓글을 조회할 수 있다")
        fun getRecentComments() {
            repeat(10) { idx ->
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Comment $idx",
                    author = "testUser",
                    password = null,
                )
            }
            val recentComments = commentService.getRecentCommentsByBoardId(testBoard.id)
            assertEquals(5, recentComments.size) // 기본값 5개
            // 가장 최근 댓글의 내용에는 "9"가 포함되어 있다고 가정
            assertTrue(recentComments[0].content.contains("9"))
            // 5번째 최근 댓글은 "5"를 포함
            assertTrue(recentComments[4].content.contains("5"))
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    inner class UpdateComment {
        @Test
        @DisplayName("댓글 내용을 수정할 수 있다")
        fun updateCommentContent() {
            val savedComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Original Content",
                    author = "testUser",
                    password = null,
                )
            val updatedComment = commentService.updateComment(savedComment.id, "Updated Content")
            assertEquals("Updated Content", updatedComment.content)
        }

        @Test
        @DisplayName("존재하지 않는 댓글은 수정할 수 없다")
        fun cannotUpdateNonExistentComment() {
            assertFailsWith<EntityNotFoundException> {
                commentService.updateComment(999L, "New Content")
            }
        }

        @Test
        @DisplayName("1000자를 초과하는 내용으로 수정할 수 없다")
        fun cannotUpdateWithTooLongContent() {
            val savedComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Original Content",
                    author = "testUser",
                    password = null,
                )
            val tooLongContent = "a".repeat(1001)
            assertFailsWith<IllegalArgumentException> {
                commentService.updateComment(savedComment.id, tooLongContent)
            }
        }

        @Test
        @DisplayName("빈 내용으로 수정할 수 없다")
        fun cannotUpdateWithEmptyContent() {
            val savedComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Original Content",
                    author = "testUser",
                    password = null,
                )
            assertFailsWith<IllegalArgumentException> {
                commentService.updateComment(savedComment.id, "")
            }
            assertFailsWith<IllegalArgumentException> {
                commentService.updateComment(savedComment.id, "   ")
            }
        }

        @Test
        @DisplayName("수정해도 작성자와 게시글 정보는 유지된다")
        fun authorAndBoardRemainUnchanged() {
            val savedComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Original Content",
                    author = "testUser",
                    password = null,
                )
            val originalAuthor = savedComment.author
            val originalBoard = savedComment.board
            val updatedComment = commentService.updateComment(savedComment.id, "Updated Content")
            assertEquals(originalAuthor, updatedComment.author)
            assertEquals(originalBoard.id, updatedComment.board.id)
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    inner class DeleteComment {
        @Test
        @DisplayName("댓글을 삭제할 수 있다")
        fun deleteComment() {
            val savedComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Test Comment",
                    author = "testUser",
                    password = null,
                )
            commentService.deleteComment(savedComment.id)
            assertFailsWith<EntityNotFoundException> {
                commentService.getCommentById(savedComment.id)
            }
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제시 예외가 발생하지 않는다")
        fun deleteNonExistentComment() {
            assertDoesNotThrow {
                commentService.deleteComment(999L)
            }
        }

        @Test
        @DisplayName("댓글 삭제시 게시글은 유지된다")
        fun boardRemainsAfterCommentDeletion() {
            val savedComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Test Comment",
                    author = "testUser",
                    password = null,
                )
            commentService.deleteComment(savedComment.id)
            assertDoesNotThrow {
                boardRepository.findById(testBoard.id)
            }
        }
    }

    @Nested
    @DisplayName("댓글 통계")
    inner class CommentStatistics {
        @Test
        @DisplayName("게시글별 댓글 수를 계산할 수 있다")
        fun countCommentsByBoard() {
            repeat(3) {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Comment $it",
                    author = "testUser",
                    password = null,
                )
            }
            val count = commentService.countCommentsByBoardId(testBoard.id)
            assertEquals(3, count)
        }

        @Test
        @DisplayName("존재하지 않는 게시글의 댓글 수는 0이다")
        fun countCommentsOfNonExistentBoard() {
            val count = commentService.countCommentsByBoardId(999L)
            assertEquals(0, count)
        }

        @Test
        @DisplayName("작성자의 댓글 존재 여부를 확인할 수 있다")
        fun checkAuthorHasCommented() {
            commentService.createComment(
                boardId = testBoard.id,
                content = "Test Comment",
                author = "testUser",
                password = null,
            )
            assertTrue(commentService.hasCommentedOnBoard(testBoard.id, "testUser"))
            assertFalse(commentService.hasCommentedOnBoard(testBoard.id, "otherUser"))
            assertFalse(commentService.hasCommentedOnBoard(999L, "testUser"))
        }
    }
}
