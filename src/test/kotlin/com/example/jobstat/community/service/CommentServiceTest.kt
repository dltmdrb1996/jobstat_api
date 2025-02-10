package com.example.jobstat.community.service

import com.example.jobstat.community.fake.BoardFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import com.example.jobstat.community.internal.entity.Board
import com.example.jobstat.community.internal.service.CommentService
import com.example.jobstat.community.internal.service.CommentServiceImpl
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
        @DisplayName("유저 ID가 있는 경우 댓글 생성")
        fun createCommentWithUserId() {
            val userId = 100L
            val createdComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "테스트 댓글",
                    author = "테스트사용자",
                    password = null,
                    userId = userId,
                )
            assertEquals("테스트 댓글", createdComment.content)
            assertEquals("테스트사용자", createdComment.author)
            assertEquals(testBoard.id, createdComment.board.id)
            assertEquals(userId, createdComment.userId)
        }

        @Test
        @DisplayName("유저 ID가 없는 경우 댓글 생성")
        fun createCommentWithoutUserId() {
            val createdComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "테스트 댓글",
                    author = "테스트사용자",
                    password = null,
                    userId = null,
                )
            assertEquals("테스트 댓글", createdComment.content)
            assertEquals("테스트사용자", createdComment.author)
            assertEquals(testBoard.id, createdComment.board.id)
            assertNull(createdComment.userId)
        }

        @Test
        @DisplayName("비밀댓글로 생성할 수 있다")
        fun createPasswordProtectedComment() {
            val userId = 50L
            val createdComment =
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "비밀 댓글",
                    author = "테스트사용자",
                    password = "testpassword123!",
                    userId = userId,
                )
            assertNotNull(createdComment.password)
            assertEquals("testpassword123!", createdComment.password)
            assertEquals(userId, createdComment.userId)
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
                    userId = 1L,
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
                    userId = 1L,
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
                    userId = 1L,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "   ",
                    author = "testUser",
                    password = null,
                    userId = 1L,
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
                    userId = 1L,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Test Comment",
                    author = "   ",
                    password = null,
                    userId = 1L,
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
                    userId = 1L,
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
                    userId = idx.toLong(),
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
                    userId = it.toLong(),
                )
            }
            commentService.createComment(
                boardId = testBoard.id,
                content = "Other Comment",
                author = "otherUser",
                password = null,
                userId = 1L,
            )
            val comments = commentService.getCommentsByAuthor("testUser", PageRequest.of(0, 10))
            assertEquals(2, comments.content.size)
            assertTrue(comments.content.all { it.author == "testUser" })
        }

        @Test
        @DisplayName("게시글 ID와 작성자로 댓글을 조회할 수 있다")
        fun getCommentsByBoardIdAndAuthor() {
            repeat(2) {
                commentService.createComment(
                    boardId = testBoard.id,
                    content = "Comment $it",
                    author = "testUser",
                    password = null,
                    userId = it.toLong(),
                )
            }
            val board2 = boardRepository.save(BoardFixture.aBoard().create())
            commentService.createComment(
                boardId = board2.id,
                content = "Other Board Comment",
                author = "testUser",
                password = null,
                userId = 1L,
            )
            commentService.createComment(
                boardId = testBoard.id,
                content = "Other User Comment",
                author = "otherUser",
                password = null,
                userId = 1L,
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
                    userId = idx.toLong(),
                )
            }
            val recentComments = commentService.getRecentCommentsByBoardId(testBoard.id)
            assertEquals(5, recentComments.size)
            // 가장 최근 댓글은 "Comment 9"여야 함
            assertTrue(recentComments[0].content.contains("9"))
            // 5번째 최근 댓글은 "Comment 5"를 포함
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
                    userId = 1L,
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
                    userId = 1L,
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
                    userId = 1L,
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
                    userId = 1L,
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
                    userId = 1L,
                )
            commentService.deleteComment(savedComment.id)
            assertFailsWith<EntityNotFoundException> {
                commentService.getCommentById(savedComment.id)
            }
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제시 예외가 발생한다")
        fun deleteNonExistentComment() {
            assertFailsWith<EntityNotFoundException> {
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
                    userId = 1L,
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
                    userId = it.toLong(),
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
                userId = 1L,
            )
            assertTrue(commentService.hasCommentedOnBoard(testBoard.id, "testUser"))
            assertFalse(commentService.hasCommentedOnBoard(testBoard.id, "otherUser"))
            assertFalse(commentService.hasCommentedOnBoard(999L, "testUser"))
        }
    }

    @Test
    @DisplayName("댓글 생성 시 게시글의 댓글 수가 증가한다")
    fun incrementsBoardCommentCount() {
        assertEquals(0, testBoard.commentCount)

        commentService.createComment(
            boardId = testBoard.id,
            content = "첫 번째 댓글",
            author = "테스트사용자",
            password = null,
            userId = 1L,
        )

        assertEquals(1, testBoard.commentCount)

        commentService.createComment(
            boardId = testBoard.id,
            content = "두 번째 댓글",
            author = "테스트사용자",
            password = null,
            userId = 2L,
        )

        assertEquals(2, testBoard.commentCount)
    }
}
