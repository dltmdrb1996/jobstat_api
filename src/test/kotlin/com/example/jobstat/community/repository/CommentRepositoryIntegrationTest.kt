package com.example.jobstat.community.repository

import com.example.jobstat.community.internal.entity.Board
import com.example.jobstat.community.internal.entity.BoardCategory
import com.example.jobstat.community.internal.entity.Comment
import com.example.jobstat.community.internal.repository.BoardRepository
import com.example.jobstat.community.internal.repository.CategoryRepository
import com.example.jobstat.community.internal.repository.CommentRepository
import com.example.jobstat.utils.base.JpaIntegrationTestSupport
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("CommentRepository 통합 테스트")
class CommentRepositoryIntegrationTest : JpaIntegrationTestSupport() {
    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var testCategory: BoardCategory
    private lateinit var testBoard: Board
    private lateinit var testComment: Comment

    @BeforeEach
    fun setUp() {
        cleanupTestData()

        testCategory =
            categoryRepository.save(
                BoardCategory.create("TEST_CATEGORY", "Test Category", "Test Description"),
            )

        testBoard =
            Board.create(
                title = "Test Title",
                content = "Test Content",
                author = "testUser",
                password = null,
                category = testCategory,
                // 회원 게시글인 경우 userId를 반드시 전달 (예: 1L)
                userId = 1L,
            )
        testBoard = boardRepository.save(testBoard)

        // 회원 댓글인 경우 password가 null이므로 userId를 전달 (예: 1L)
        testComment =
            Comment.create(
                content = "Test Comment",
                author = "commentUser",
                password = null,
                board = testBoard,
                userId = 1L,
            )
    }

    override fun cleanupTestData() {
        executeInTransaction {
            commentRepository.findAll(PageRequest.of(0, 100)).forEach { comment ->
                commentRepository.deleteById(comment.id)
            }
            boardRepository.findAll(PageRequest.of(0, 100)).forEach { board ->
                boardRepository.delete(board)
            }
            categoryRepository.findAll().forEach { category ->
                categoryRepository.deleteById(category.id)
            }
        }
        flushAndClear()
    }

    @Nested
    @DisplayName("댓글 생성 테스트")
    inner class CreateCommentTest {
        @Test
        @DisplayName("새로운 댓글을 생성할 수 있다")
        fun createCommentSuccess() {
            // When
            val savedComment = commentRepository.save(testComment)

            // Then
            assertEquals(testComment.content, savedComment.content)
            assertEquals(testComment.author, savedComment.author)
            assertEquals(testBoard.id, savedComment.board.id)
            assertTrue(savedComment.id > 0)
        }

        @Test
        @DisplayName("최대 길이의 내용으로 댓글을 생성할 수 있다")
        fun createMaxLengthCommentSuccess() {
            // Given
            val maxLengthContent = "a".repeat(1000)
            val comment =
                Comment.create(
                    content = maxLengthContent,
                    author = "commentUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )

            // When
            val savedComment = commentRepository.save(comment)

            // Then
            assertEquals(maxLengthContent, savedComment.content)
        }

        @Test
        @DisplayName("내용이 1000자를 초과하면 실패한다")
        fun createTooLongCommentFail() {
            // Given
            val tooLongContent = "a".repeat(1001)

            // When & Then
            assertFailsWith<IllegalArgumentException> {
                Comment.create(
                    content = tooLongContent,
                    author = "commentUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )
            }
        }

        @Test
        @DisplayName("빈 내용으로 댓글을 생성할 수 없다")
        fun createEmptyContentFail() {
            assertFailsWith<IllegalArgumentException> {
                Comment.create(
                    content = "",
                    author = "commentUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                Comment.create(
                    content = "   ",
                    author = "commentUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )
            }
        }
    }

    @Nested
    @DisplayName("댓글 조회 테스트")
    inner class FindCommentTest {
        @Test
        @DisplayName("ID로 댓글을 조회할 수 있다")
        fun findByIdSuccess() {
            // Given
            val savedComment = saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            // When
            val foundComment = commentRepository.findById(savedComment.id)

            // Then
            assertEquals(savedComment.id, foundComment.id)
            assertEquals(savedComment.content, foundComment.content)
            assertEquals(savedComment.author, foundComment.author)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun findByNonExistentIdFail() {
            assertFailsWith<JpaObjectRetrievalFailureException> {
                commentRepository.findById(999L)
            }
        }

        @Test
        @DisplayName("게시글 ID로 댓글을 조회할 수 있다")
        fun findByBoardIdSuccess() {
            // Given
            val savedComment = saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            // When
            val comments = commentRepository.findByBoardId(testBoard.id, PageRequest.of(0, 10))

            // Then
            assertEquals(1, comments.content.size)
            assertEquals(savedComment.id, comments.content[0].id)
        }

        @Test
        @DisplayName("존재하지 않는 게시글 ID로 조회시 빈 결과를 반환한다")
        fun findByNonExistentBoardIdSuccess() {
            // When
            val comments = commentRepository.findByBoardId(999L, PageRequest.of(0, 10))

            // Then
            assertTrue(comments.isEmpty)
        }

        @Test
        @DisplayName("게시글의 최근 댓글을 조회할 수 있다")
        fun findRecentCommentsSuccess() {
            // Given
            val comment1 = saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            val comment2 =
                Comment.create(
                    content = "Recent Comment",
                    author = "commentUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )
            val savedComment2 = saveAndGetAfterCommit(comment2) { commentRepository.save(it) }

            // When
            val recentComments = commentRepository.findRecentComments(testBoard.id, PageRequest.of(0, 10))

            // Then
            assertEquals(2, recentComments.size)
            // 최신 댓글이 먼저 오도록 정렬했다고 가정하면 savedComment2가 첫번째에 위치
            assertEquals(savedComment2.id, recentComments[0].id)
        }

        @Test
        @DisplayName("작성자로 댓글을 조회할 수 있다")
        fun findByAuthorSuccess() {
            // Given
            val savedComment = saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            val comment2 =
                Comment.create(
                    content = "Another Comment",
                    author = "anotherUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )
            saveAndGetAfterCommit(comment2) { commentRepository.save(it) }

            // When
            val comments = commentRepository.findByAuthor("commentUser", PageRequest.of(0, 10))

            // Then
            assertEquals(1, comments.content.size)
            assertEquals(savedComment.id, comments.content[0].id)
        }

        @Test
        @DisplayName("게시글 ID와 작성자로 댓글을 조회할 수 있다")
        fun findByBoardIdAndAuthorSuccess() {
            // Given
            val savedComment = saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            // 다른 게시글에 같은 작성자의 댓글 추가
            val anotherBoard =
                Board.create(
                    title = "Another Title",
                    content = "Another Content",
                    author = "testUser",
                    password = null,
                    category = testCategory,
                    userId = 1L,
                )
            val savedBoard2 = saveAndGetAfterCommit(anotherBoard) { boardRepository.save(it) }

            val comment2 =
                Comment.create(
                    content = "Another Comment",
                    author = "commentUser",
                    password = null,
                    board = savedBoard2,
                    userId = 1L,
                )
            saveAndGetAfterCommit(comment2) { commentRepository.save(it) }

            // When
            val comments =
                commentRepository.findByBoardIdAndAuthor(
                    testBoard.id,
                    "commentUser",
                    PageRequest.of(0, 10),
                )

            // Then
            assertEquals(1, comments.content.size)
            assertEquals(savedComment.id, comments.content[0].id)
        }
    }

    @Nested
    @DisplayName("댓글 수정/삭제 테스트")
    inner class UpdateDeleteCommentTest {
        @Test
        @DisplayName("댓글 내용을 수정할 수 있다")
        fun updateContentSuccess() {
            // Given
            val savedComment = saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            // When
            val foundComment = commentRepository.findById(savedComment.id)
            foundComment.updateContent("Updated Content")
            val updatedComment = saveAndGetAfterCommit(foundComment) { commentRepository.save(it) }

            // Then
            assertEquals("Updated Content", updatedComment.content)
        }

        @Test
        @DisplayName("댓글을 삭제할 수 있다")
        fun deleteCommentSuccess() {
            // Given
            val savedComment = saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            // When
            commentRepository.deleteById(savedComment.id)
            flushAndClear()

            // Then
            assertFailsWith<JpaObjectRetrievalFailureException> {
                commentRepository.findById(savedComment.id)
            }
        }

        @Test
        @DisplayName("게시글의 모든 댓글을 삭제할 수 있다")
        fun deleteAllBoardCommentsSuccess() {
            // Given
            saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            val comment2 =
                Comment.create(
                    content = "Another Comment",
                    author = "commentUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )
            saveAndGetAfterCommit(comment2) { commentRepository.save(it) }

            // When
            commentRepository.deleteByBoardId(testBoard.id)
            flushAndClear()

            // Then
            assertTrue(commentRepository.findByBoardId(testBoard.id, PageRequest.of(0, 10)).isEmpty)
        }
    }

    @Nested
    @DisplayName("댓글 통계 테스트")
    inner class CommentStatisticsTest {
        @Test
        @DisplayName("게시글별 댓글 수를 계산할 수 있다")
        fun countByBoardIdSuccess() {
            // Given
            saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            val comment2 =
                Comment.create(
                    content = "Comment 2",
                    author = "commentUser",
                    password = null,
                    board = testBoard,
                    userId = 1L,
                )
            saveAndGetAfterCommit(comment2) { commentRepository.save(it) }

            // When
            val commentCount = commentRepository.countByBoardId(testBoard.id)
            val nonExistentBoardCount = commentRepository.countByBoardId(999L)

            // Then
            assertEquals(2, commentCount)
            assertEquals(0, nonExistentBoardCount)
        }

        @Test
        @DisplayName("게시글과 작성자별 댓글 존재 여부를 확인할 수 있다")
        fun existsByBoardIdAndAuthorSuccess() {
            // Given
            saveAndGetAfterCommit(testComment) { commentRepository.save(it) }

            // When & Then
            assertTrue(commentRepository.existsByBoardIdAndAuthor(testBoard.id, "commentUser"))
            assertFalse(commentRepository.existsByBoardIdAndAuthor(testBoard.id, "nonexistent"))
            assertFalse(commentRepository.existsByBoardIdAndAuthor(999L, "commentUser"))
        }
    }
}
