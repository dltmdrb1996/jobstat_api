package com.wildrew.app.community.comment.service

import com.wildrew.app.community.board.entity.Board
import com.wildrew.app.community.board.entity.BoardCategory
import com.wildrew.app.community.board.fixture.BoardFixture
import com.wildrew.app.community.board.fixture.CategoryFixture
import com.wildrew.app.community.board.repository.FakeBoardRepository
import com.wildrew.app.community.board.repository.FakeCategoryRepository
import com.wildrew.app.community.comment.repository.FakeCommentRepository
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@DisplayName("CommentService 테스트 (Refactored)")
class CommentServiceTest {
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentService: CommentService
    private lateinit var testBoard: Board
    private lateinit var testCategory: BoardCategory

    @BeforeEach
    fun setUp() {
        commentRepository = FakeCommentRepository()
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentService = CommentServiceImpl(commentRepository, boardRepository)

        testCategory = categoryRepository.save(CategoryFixture.aCategory().create())
        testBoard = boardRepository.save(BoardFixture.aBoard().withCategory(testCategory).create())
    }

    @AfterEach
    fun tearDown() {
        commentRepository.clear()
        boardRepository.clear()
        categoryRepository.clear()
    }

    @Nested
    @DisplayName("댓글 생성 (createComment)")
    inner class CreateComment {
        @Test
        @DisplayName("성공: 유저 ID 포함하여 댓글 생성")
        fun `given valid details with userId, when createComment, then return new comment`() {
            // Given
            val boardId = testBoard.id
            val content = "테스트 댓글 내용"
            val author = "테스트 작성자"
            val userId = 123L

            // When
            val createdComment = commentService.createComment(boardId, content, author, null, userId)

            // Then
            assertNotNull(createdComment)
            assertTrue(createdComment.id > 0)
            assertEquals(content, createdComment.content)
            assertEquals(author, createdComment.author)
            assertEquals(boardId, createdComment.board.id)
            assertEquals(userId, createdComment.userId)
            assertNull(createdComment.password)

            // Verify
            val found = commentRepository.findById(createdComment.id)
            assertEquals(createdComment.id, found.id)
            assertEquals(userId, found.userId)
        }

        @Test
        @DisplayName("성공: 유저 ID 없이 (비회원) 댓글 생성")
        fun `given valid details without userId, when createComment, then return new comment`() {
            // Given
            val boardId = testBoard.id
            val content = "비회원 댓글"
            val author = "익명"

            // When
            val createdComment = commentService.createComment(boardId, content, author, null, null)

            // Then
            assertNotNull(createdComment)
            assertEquals(content, createdComment.content)
            assertEquals(author, createdComment.author)
            assertEquals(boardId, createdComment.board.id)
            assertNull(createdComment.userId)
        }

        @Test
        @DisplayName("성공: 비밀번호 포함하여 댓글 생성")
        fun `given valid details with password, when createComment, then return comment with password`() {
            // Given
            val boardId = testBoard.id
            val content = "비밀 댓글"
            val author = "작성자"
            val password = "securepassword123"
            val userId = 45L

            // When
            val createdComment = commentService.createComment(boardId, content, author, password, userId)

            // Then
            assertNotNull(createdComment)
            assertEquals(content, createdComment.content)
            assertNotNull(createdComment.password)
            assertEquals(password, createdComment.password)
            assertEquals(userId, createdComment.userId)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게시글 ID로 생성 시 EntityNotFoundException 발생")
        fun `given non-existent boardId, when createComment, then throw EntityNotFoundException`() {
            // Given
            val nonExistentBoardId = 999L

            // When & Then
            assertThrows<EntityNotFoundException> {
                commentService.createComment(nonExistentBoardId, "내용", "작성자", null, 1L)
            }
        }

        @Test
        @DisplayName("실패: 내용이 너무 길면 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given too long content, when createComment, then throw IllegalArgumentException`() {
            // Given
            val tooLongContent = "a".repeat(1001)

            // When & Then
            assertThrows<IllegalArgumentException> {
                commentService.createComment(testBoard.id, tooLongContent, "author", null, 1L)
            }
        }

        @Test
        @DisplayName("실패: 내용이 비어있으면 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given empty or blank content, when createComment, then throw IllegalArgumentException`() {
            // Given
            val emptyContent = ""
            val blankContent = "    "

            // When & Then
            assertThrows<IllegalArgumentException>("빈 내용") {
                commentService.createComment(testBoard.id, emptyContent, "author", null, 1L)
            }
            assertThrows<IllegalArgumentException>("공백 내용") {
                commentService.createComment(testBoard.id, blankContent, "author", null, 1L)
            }
        }

        @Test
        @DisplayName("실패: 작성자가 비어있으면 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given empty or blank author, when createComment, then throw IllegalArgumentException`() {
            // Given
            val emptyAuthor = ""
            val blankAuthor = "   "

            // When & Then
            assertThrows<IllegalArgumentException>("빈 작성자") {
                commentService.createComment(testBoard.id, "Content", emptyAuthor, null, 1L)
            }
            assertThrows<IllegalArgumentException>("공백 작성자") {
                commentService.createComment(testBoard.id, "Content", blankAuthor, null, 1L)
            }
        }

        @Test
        @DisplayName("성공: 댓글 생성 시 게시글의 댓글 수가 증가한다 (Board Entity 로직)")
        fun `when createComment, then board commentCount increases`() {
            // Given
            val initialCount = testBoard.commentCount

            // When
            val comment1 = commentService.createComment(testBoard.id, "댓글 1", "user1", null, 1L)
            val boardAfter1 = boardRepository.findById(testBoard.id)

            // Then
            assertEquals(initialCount + 1, boardAfter1.commentCount)

            // When
            val comment2 = commentService.createComment(testBoard.id, "댓글 2", "user2", null, 2L)
            val boardAfter2 = boardRepository.findById(testBoard.id)

            // Then
            assertEquals(initialCount + 2, boardAfter2.commentCount)
        }
    }

    @Nested
    @DisplayName("댓글 수정 (updateComment)")
    inner class UpdateComment {
        @Test
        @DisplayName("성공: 존재하는 댓글 내용을 수정한다")
        fun `given existing comment and valid content, when updateComment, then return updated comment`() {
            // Given
            val originalComment = commentService.createComment(testBoard.id, "원본 내용", "author", null, 1L)
            val newContent = "수정된 내용입니다."

            // When
            val updatedComment = commentService.updateComment(originalComment.id, newContent)

            // Then
            assertNotNull(updatedComment)
            assertEquals(originalComment.id, updatedComment.id)
            assertEquals(newContent, updatedComment.content)

            assertEquals(originalComment.author, updatedComment.author)
            assertEquals(originalComment.board.id, updatedComment.board.id)
            assertEquals(originalComment.userId, updatedComment.userId)

            // Verify persistence
            val found = commentRepository.findById(originalComment.id)
            assertEquals(newContent, found.content)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 댓글 ID로 수정 시 EntityNotFoundException 발생")
        fun `given non-existent id, when updateComment, then throw EntityNotFoundException`() {
            // Given
            val nonExistentId = 999L

            // When & Then
            assertThrows<EntityNotFoundException> {
                commentService.updateComment(nonExistentId, "새 내용")
            }
        }

        @Test
        @DisplayName("실패: 내용이 너무 길면 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given too long content, when updateComment, then throw IllegalArgumentException`() {
            // Given
            val comment = commentService.createComment(testBoard.id, "Original", "author", null, 1L)
            val tooLongContent = "a".repeat(1001)

            // When & Then
            assertThrows<IllegalArgumentException> {
                commentService.updateComment(comment.id, tooLongContent)
            }
        }

        @Test
        @DisplayName("실패: 내용이 비어있으면 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given empty or blank content, when updateComment, then throw IllegalArgumentException`() {
            // Given
            val comment = commentService.createComment(testBoard.id, "Original", "author", null, 1L)
            val emptyContent = ""
            val blankContent = "    "

            // When & Then
            assertThrows<IllegalArgumentException>("빈 내용") {
                commentService.updateComment(comment.id, emptyContent)
            }
            assertThrows<IllegalArgumentException>("공백 내용") {
                commentService.updateComment(comment.id, blankContent)
            }
        }
    }

    @Nested
    @DisplayName("댓글 삭제 (deleteComment)")
    inner class DeleteComment {
        @Test
        @DisplayName("성공: 존재하는 댓글을 삭제한다")
        fun `given existing comment, when deleteComment, then comment is deleted`() {
            // Given
            val commentToDelete = commentService.createComment(testBoard.id, "삭제될 댓글", "author", null, 1L)
            val commentId = commentToDelete.id
            val board = commentToDelete.board
            val initialBoardCommentCount = board.commentCount

            // When
            commentService.deleteComment(commentId)

            // Then
            assertThrows<EntityNotFoundException>("삭제 후 조회 시 실패해야 함") {
                commentService.getCommentById(commentId)
            }
            assertFalse(commentRepository.existsById(commentId))

            val boardAfterDelete = boardRepository.findById(board.id)
            assertEquals(initialBoardCommentCount - 1, boardAfterDelete.commentCount)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 댓글 ID로 삭제 시 EntityNotFoundException 발생")
        fun `given non-existent id, when deleteComment, then throw EntityNotFoundException`() {
            // Given
            val nonExistentId = 999L

            // WhenThen
            assertThrows<EntityNotFoundException> {
                commentService.deleteComment(nonExistentId)
            }
        }
    }

    @Nested
    @DisplayName("댓글 조회")
    inner class GetComments {
        @Test
        @DisplayName("ID로 댓글 조회 (getCommentById)")
        fun `given existing id, when getCommentById, then return comment`() {
            // Given
            val savedComment = commentService.createComment(testBoard.id, "내용", "작성자", null, 1L)

            // When
            val foundComment = commentService.getCommentById(savedComment.id)

            // Then
            assertNotNull(foundComment)
            assertEquals(savedComment.id, foundComment.id)
        }

        @Test
        @DisplayName("ID로 댓글 조회 실패 (getCommentById - non-existent)")
        fun `given non-existent id, when getCommentById, then throw EntityNotFoundException`() {
            // Given
            val nonExistentId = 999L
            // When & Then
            assertThrows<EntityNotFoundException> {
                commentService.getCommentById(nonExistentId)
            }
        }

        @Test
        @DisplayName("여러 ID로 댓글 목록 조회 (getCommentsByIds)")
        fun `given list of ids, when getCommentsByIds, then return corresponding comments`() {
            // Given
            val comment1 = commentService.createComment(testBoard.id, "C1", "A1", null, 1L)
            val comment2 = commentService.createComment(testBoard.id, "C2", "A2", null, 2L)
            val comment3 = commentService.createComment(testBoard.id, "C3", "A1", null, 1L)
            val nonExistentId = 998L
            val idsToFetch = listOf(comment1.id, nonExistentId, comment3.id)

            // When
            val comments = commentService.getCommentsByIds(idsToFetch)

            // Then
            assertEquals(2, comments.size)
            assertTrue(comments.any { it.id == comment1.id })
            assertTrue(comments.any { it.id == comment3.id })
            assertFalse(comments.any { it.id == comment2.id })
        }

        @Test
        @DisplayName("여러 ID로 댓글 목록 조회 - 빈 ID 리스트 (getCommentsByIds)")
        fun `given empty id list, when getCommentsByIds, then return empty list`() {
            // Given
            val emptyIds = emptyList<Long>()
            // When
            val comments = commentService.getCommentsByIds(emptyIds)
            // Then
            assertTrue(comments.isEmpty())
        }

        @Test
        @DisplayName("게시글 ID로 댓글 목록 조회 (getCommentsByBoardId - Pageable)")
        fun `given boardId and pageable, when getCommentsByBoardId, then return paginated comments`() {
            // Given
            repeat(15) { idx ->
                commentService.createComment(testBoard.id, "Comment $idx", "Author", null, idx.toLong())
            }
            val otherBoard = boardRepository.save(BoardFixture.aBoard().withCategory(category = testCategory).create())
            commentService.createComment(otherBoard.id, "Other board comment", "Author", null, 100L)

            val pageable = PageRequest.of(1, 5, Sort.by("id").ascending())

            // When
            val commentPage = commentService.getCommentsByBoardId(testBoard.id, pageable)

            // Then
            assertEquals(5, commentPage.content.size)
            assertEquals(15, commentPage.totalElements)
            assertEquals(3, commentPage.totalPages)
            assertEquals(1, commentPage.number)
            assertTrue(commentPage.content.all { it.board.id == testBoard.id })
        }

        @Test
        @DisplayName("게시글 ID로 댓글 목록 조회 - 결과 없음 (getCommentsByBoardId)")
        fun `given boardId with no comments, when getCommentsByBoardId, then return empty page`() {
            // Given
            val boardWithNoComments = boardRepository.save(BoardFixture.aBoard().withCategory(category = testCategory).create())
            val pageable = PageRequest.of(0, 10)

            // When
            val commentPage = commentService.getCommentsByBoardId(boardWithNoComments.id, pageable)

            // Then
            assertTrue(commentPage.content.isEmpty())
            assertEquals(0, commentPage.totalElements)
        }

        @Test
        @DisplayName("게시글 ID로 댓글 목록 조회 (getCommentsByBoardIdAfter - Cursor)")
        fun `given boardId, limit, and lastCommentId, when getCommentsByBoardIdAfter, then return next comments`() {
            val comments =
                (1..10)
                    .map { idx ->
                        commentService.createComment(testBoard.id, "Comment $idx", "Author", null, idx.toLong())
                    }.sortedBy { it.id }

            val limit = 4
            val firstPage = commentService.getCommentsByBoardIdAfter(testBoard.id, null, limit)

            // When
            val lastIdFirstPage = firstPage.lastOrNull()?.id
            assertNotNull(lastIdFirstPage, "첫번째 페이지는 비어있지 않아야 함")
            val secondPage = commentService.getCommentsByBoardIdAfter(testBoard.id, lastIdFirstPage, limit)

            val lastIdSecondPage = secondPage.lastOrNull()?.id
            assertNotNull(lastIdSecondPage, "두번째 페이지는 비어있지 않아야 함")
            val thirdPage = commentService.getCommentsByBoardIdAfter(testBoard.id, lastIdSecondPage, limit)

            val lastIdThirdPage = thirdPage.lastOrNull()?.id
            assertNotNull(lastIdThirdPage, "세번째 페이지는 비어있지 않아야 함")
            val fourthPage = commentService.getCommentsByBoardIdAfter(testBoard.id, lastIdThirdPage, limit)

            // Then
            assertEquals(limit, firstPage.size)
            assertEquals(limit, secondPage.size)
            assertEquals(2, thirdPage.size)
            assertTrue(fourthPage.isEmpty(), "네번째 페이지는 비어있어야 함")
        }

        @Test
        @DisplayName("게시글 ID로 댓글 목록 조회 - lastCommentId가 존재하지 않음 (getCommentsByBoardIdAfter)")
        fun `given non-existent lastCommentId, when getCommentsByBoardIdAfter, then return results as if lastCommentId was null`() {
            // Given
            (1..5).forEach { commentService.createComment(testBoard.id, "C$it", "A", null, it.toLong()) }
            val limit = 3
            val nonExistentLastId = 999L

            // When
            val firstPage = commentService.getCommentsByBoardIdAfter(testBoard.id, null, limit)
            val pageWithInvalidCursor = commentService.getCommentsByBoardIdAfter(testBoard.id, nonExistentLastId, limit)

            // Then
            assertEquals(firstPage.size, pageWithInvalidCursor.size)
            assertEquals(firstPage.map { it.id }, pageWithInvalidCursor.map { it.id })
        }

        @Test
        @DisplayName("작성자로 댓글 목록 조회 (getCommentsByAuthor - Pageable)")
        fun `given author and pageable, when getCommentsByAuthor, then return paginated comments`() {
            // Given
            val author1 = "author1"
            val author2 = "author2"
            repeat(7) { commentService.createComment(testBoard.id, "C $it", author1, null, it.toLong()) }
            repeat(4) { commentService.createComment(testBoard.id, "C $it", author2, null, (it + 10).toLong()) }
            val pageable = PageRequest.of(0, 5)

            // When
            val pageAuthor1 = commentService.getCommentsByAuthor(author1, pageable)
            val pageAuthor2 = commentService.getCommentsByAuthor(author2, pageable)

            // Then - Author 1
            assertEquals(5, pageAuthor1.content.size)
            assertEquals(7, pageAuthor1.totalElements)
            assertEquals(2, pageAuthor1.totalPages)
            assertTrue(pageAuthor1.content.all { it.author == author1 })

            // Then - Author 2
            assertEquals(4, pageAuthor2.content.size)
            assertEquals(4, pageAuthor2.totalElements)
            assertEquals(1, pageAuthor2.totalPages)
            assertTrue(pageAuthor2.content.all { it.author == author2 })
        }

        @Test
        @DisplayName("작성자로 댓글 목록 조회 (getCommentsByAuthorAfter - Cursor)")
        fun `given author, limit, and lastCommentId, when getCommentsByAuthorAfter, then return next comments`() {
            // Given
            val author = "targetAuthor"
            val targetComments =
                (1..8).map {
                    commentService.createComment(testBoard.id, "Target C$it", author, null, it.toLong())
                }
            (9..12).forEach {
                commentService.createComment(testBoard.id, "Other C$it", "otherAuthor", null, it.toLong())
            }

            val limit = 3
            // When
            val page1 = commentService.getCommentsByAuthorAfter(author, null, limit)
            val page2 = commentService.getCommentsByAuthorAfter(author, page1.lastOrNull()?.id, limit)
            val page3 = commentService.getCommentsByAuthorAfter(author, page2.lastOrNull()?.id, limit)
            val page4 = commentService.getCommentsByAuthorAfter(author, page3.lastOrNull()?.id, limit)

            // Then
            assertEquals(limit, page1.size)
            assertEquals(limit, page2.size)
            assertEquals(2, page3.size)
            assertTrue(page4.isEmpty())

            assertTrue(page1.all { it.author == author })
            assertTrue(page2.all { it.author == author })
            assertTrue(page3.all { it.author == author })
        }

        @Test
        @DisplayName("게시글 ID와 작성자로 댓글 목록 조회 (getCommentsByBoardIdAndAuthor - Pageable)")
        fun `given boardId, author, and pageable, when getCommentsByBoardIdAndAuthor, then return filtered paginated comments`() {
            // Given
            val author1 = "author1"
            val author2 = "author2"
            val board1 = testBoard
            val board2 = boardRepository.save(BoardFixture.aBoard().withCategory(category = testCategory).create())

            // Board 1 comments
            commentService.createComment(board1.id, "B1 C1", author1, null, 1L)
            commentService.createComment(board1.id, "B1 C2", author2, null, 2L)
            commentService.createComment(board1.id, "B1 C3", author1, null, 3L)
            // Board 2 comments
            commentService.createComment(board2.id, "B2 C1", author1, null, 4L)

            val pageable = PageRequest.of(0, 5)

            // When
            val pageB1A1 = commentService.getCommentsByBoardIdAndAuthor(board1.id, author1, pageable)
            val pageB1A2 = commentService.getCommentsByBoardIdAndAuthor(board1.id, author2, pageable)
            val pageB2A1 = commentService.getCommentsByBoardIdAndAuthor(board2.id, author1, pageable)

            // Then
            assertEquals(2, pageB1A1.content.size)
            assertTrue(pageB1A1.content.all { it.board.id == board1.id && it.author == author1 })

            assertEquals(1, pageB1A2.content.size)
            assertTrue(pageB1A2.content.all { it.board.id == board1.id && it.author == author2 })

            assertEquals(1, pageB2A1.content.size)
            assertTrue(pageB2A1.content.all { it.board.id == board2.id && it.author == author1 })
        }
    }

    @Nested
    @DisplayName("댓글 통계 및 확인")
    inner class CommentStatsAndChecks {
        @Test
        @DisplayName("게시글별 댓글 수 계산 (countCommentsByBoardId)")
        fun `given comments on board, when countCommentsByBoardId, then return correct count`() {
            // Given
            repeat(5) { commentService.createComment(testBoard.id, "C$it", "A", null, it.toLong()) }
            val otherBoard = boardRepository.save(BoardFixture.aBoard().withCategory(category = testCategory).create())
            commentService.createComment(otherBoard.id, "Other", "A", null, 10L)

            // When
            val count1 = commentService.countCommentsByBoardId(testBoard.id)
            val count2 = commentService.countCommentsByBoardId(otherBoard.id)
            val countNonExistent = commentService.countCommentsByBoardId(999L)

            // Then
            assertEquals(5, count1)
            assertEquals(1, count2)
            assertEquals(0, countNonExistent)
        }

        @Test
        @DisplayName("특정 게시글에 특정 작성자가 댓글 달았는지 확인 (hasCommentedOnBoard)")
        fun `given author commented or not, when hasCommentedOnBoard, then return true or false`() {
            // Given
            val authorCommented = "user1"
            val authorDidNotComment = "user2"
            val otherBoard = boardRepository.save(BoardFixture.aBoard().withCategory(category = testCategory).create())

            commentService.createComment(testBoard.id, "Comment", authorCommented, null, 1L)

            // When
            val hasCommented1 = commentService.hasCommentedOnBoard(testBoard.id, authorCommented)
            val hasCommented2 = commentService.hasCommentedOnBoard(testBoard.id, authorDidNotComment)
            val hasCommentedOtherBoard = commentService.hasCommentedOnBoard(otherBoard.id, authorCommented)
            val hasCommentedNonExistentBoard = commentService.hasCommentedOnBoard(999L, authorCommented)

            // Then
            assertTrue(hasCommented1)
            assertFalse(hasCommented2)
            assertFalse(hasCommentedOtherBoard)
            assertFalse(hasCommentedNonExistentBoard)
        }
    }
}
