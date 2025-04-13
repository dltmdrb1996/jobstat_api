//package com.example.jobstat.community.repository
//
//import com.example.jobstat.community.board.entity.Board
//import com.example.jobstat.community.board.entity.BoardCategory
//import com.example.jobstat.community.board.repository.BoardRepository
//import com.example.jobstat.community.board.repository.CategoryRepository
//import com.example.jobstat.community.comment.entity.Comment
//import com.example.jobstat.comment.repository.CommentRepository
//import com.example.jobstat.utils.base.JpaIntegrationTestSupport
//import org.junit.jupiter.api.Assertions.assertFalse
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.data.domain.PageRequest
//import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
//import kotlin.test.assertEquals
//import kotlin.test.assertFailsWith
//import kotlin.test.assertTrue
//
//@DisplayName("BoardRepository 통합 테스트")
//class BoardRepositoryIntegrationTest : JpaIntegrationTestSupport() {
//    @Autowired
//    private lateinit var boardRepository: BoardRepository
//
//    @Autowired
//    private lateinit var categoryRepository: CategoryRepository
//
//    @Autowired
//    private lateinit var commentRepository: CommentRepository
//
//    private lateinit var testCategory: com.example.jobstat.community.board.entity.BoardCategory
//    private lateinit var testBoard: Board
//
//    @BeforeEach
//    fun setUp() {
//        cleanupTestData()
//        // 회원 게시글인 경우 password = null → 반드시 userId를 전달 (예: 1L)
//        testCategory = com.example.jobstat.community.board.entity.BoardCategory.create("TEST_CATEGORY", "Test Category", "Test Description")
//        testCategory = categoryRepository.save(testCategory)
//
//        testBoard =
//            Board.create(
//                title = "Test Title",
//                content = "Test Content",
//                author = "testUser",
//                password = null, // 회원 게시글 → 비밀번호 없음
//                category = testCategory,
//                userId = 1L, // 반드시 회원 userId 전달
//            )
//    }
//
//    override fun cleanupTestData() {
//        executeInTransaction {
//            commentRepository.findAll(PageRequest.of(0, 100)).forEach { comment ->
//                commentRepository.deleteById(comment.id)
//            }
//            boardRepository.findAll(PageRequest.of(0, 100)).forEach { board ->
//                boardRepository.delete(board)
//            }
//            categoryRepository.findAll().forEach { category ->
//                categoryRepository.deleteById(category.id)
//            }
//        }
//        flushAndClear()
//    }
//
//    @Nested
//    @DisplayName("게시글 생성 테스트")
//    inner class CreateBoardTest {
//        @Test
//        @DisplayName("새로운 게시글을 생성할 수 있다")
//        fun createBoardSuccess() {
//            // When
//            val savedBoard = boardRepository.save(testBoard)
//
//            // Then
//            assertEquals(testBoard.title, savedBoard.title)
//            assertEquals(testBoard.content, savedBoard.content)
//            assertEquals(testBoard.author, savedBoard.author)
//            assertEquals(testCategory.id, savedBoard.category.id)
//            assertTrue(savedBoard.id > 0)
//            assertEquals(0, savedBoard.viewCount)
//            assertEquals(0, savedBoard.likeCount)
//        }
//
//        @Test
//        @DisplayName("최대 길이의 제목과 내용으로 게시글을 생성할 수 있다")
//        fun createBoardWithMaxLengthSuccess() {
//            // Given
//            val maxLengthTitle = "a".repeat(100)
//            val maxLengthContent = "a".repeat(5000)
//            val board =
//                Board.create(
//                    title = maxLengthTitle,
//                    content = maxLengthContent,
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//
//            // When
//            val savedBoard = boardRepository.save(board)
//
//            // Then
//            assertEquals(maxLengthTitle, savedBoard.title)
//            assertEquals(maxLengthContent, savedBoard.content)
//        }
//
//        @Test
//        @DisplayName("제목이 100자를 초과하면 실패한다")
//        fun createBoardWithTooLongTitleFail() {
//            // Given
//            val tooLongTitle = "a".repeat(101)
//
//            // When & Then
//            assertFailsWith<IllegalArgumentException> {
//                Board.create(
//                    title = tooLongTitle,
//                    content = "Test Content",
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            }
//        }
//
//        @Test
//        @DisplayName("내용이 5000자를 초과하면 실패한다")
//        fun createBoardWithTooLongContentFail() {
//            // Given
//            val tooLongContent = "a".repeat(5001)
//
//            // When & Then
//            assertFailsWith<IllegalArgumentException> {
//                Board.create(
//                    title = "Test Title",
//                    content = tooLongContent,
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            }
//        }
//    }
//
//    @Nested
//    @DisplayName("게시글 조회 테스트")
//    inner class FindBoardTest {
//        @Test
//        @DisplayName("ID로 게시글을 조회할 수 있다")
//        fun findByIdSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            // When
//            val foundBoard = boardRepository.findById(savedBoard.id)
//
//            // Then
//            assertEquals(savedBoard.id, foundBoard.id)
//            assertEquals(savedBoard.title, foundBoard.title)
//            assertEquals(savedBoard.content, foundBoard.content)
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
//        fun findByNonExistentIdFail() {
//            assertFailsWith<JpaObjectRetrievalFailureException> {
//                boardRepository.findById(999L)
//            }
//        }
//
//        @Test
//        @DisplayName("카테고리별로 게시글을 조회할 수 있다")
//        fun findByCategorySuccess() {
//            // Given
//            val anotherCategory =
//                categoryRepository.save(
//                    com.example.jobstat.community.board.entity.BoardCategory.create("ANOTHER_CATEGORY", "Another Category", "Another Description"),
//                )
//            val board1 = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val board2 =
//                Board.create(
//                    title = "Another Title",
//                    content = "Another Content",
//                    author = "testUser",
//                    password = null,
//                    category = anotherCategory,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(board2) { boardRepository.save(it) }
//
//            // When
//            val foundBoards = boardRepository.findByCategory(testCategory.id, PageRequest.of(0, 10))
//
//            // Then
//            assertEquals(1, foundBoards.content.size)
//            assertEquals(board1.id, foundBoards.content[0].id)
//        }
//
//        @Test
//        @DisplayName("작성자로 게시글을 조회할 수 있다")
//        fun findByAuthorSuccess() {
//            // Given
//            val board1 = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val board2 =
//                Board.create(
//                    title = "Another Title",
//                    content = "Another Content",
//                    author = "anotherUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(board2) { boardRepository.save(it) }
//
//            // When
//            val foundBoards = boardRepository.findByAuthor("testUser", PageRequest.of(0, 10))
//
//            // Then
//            assertEquals(1, foundBoards.content.size)
//            assertEquals(board1.id, foundBoards.content[0].id)
//        }
//
//        @Test
//        @DisplayName("작성자와 카테고리로 게시글을 조회할 수 있다")
//        fun findByAuthorAndCategorySuccess() {
//            // Given
//            val anotherCategory =
//                categoryRepository.save(
//                    com.example.jobstat.community.board.entity.BoardCategory.create("ANOTHER_CATEGORY", "Another Category", "Another Description"),
//                )
//
//            // 같은 작성자, 다른 카테고리
//            val board1 = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//            val board2 =
//                Board.create(
//                    title = "Another Title",
//                    content = "Another Content",
//                    author = "testUser",
//                    password = null,
//                    category = anotherCategory,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(board2) { boardRepository.save(it) }
//
//            // When
//            val foundBoards = boardRepository.findByAuthorAndCategory("testUser", testCategory.id, PageRequest.of(0, 10))
//
//            // Then
//            assertEquals(1, foundBoards.content.size)
//            assertEquals(board1.id, foundBoards.content[0].id)
//        }
//
//        @Test
//        @DisplayName("조회수 순으로 상위 N개 게시글을 조회할 수 있다")
//        fun findTopNByViewCountSuccess() {
//            // Given
//            testBoard.incrementViewCount() // 조회수 1
//            val board1 = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val board2 =
//                Board.create(
//                    title = "Title 2",
//                    content = "Content 2",
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            board2.incrementViewCount() // 조회수 1
//            board2.incrementViewCount() // 조회수 2
//            saveAndGetAfterCommit(board2) { boardRepository.save(it) }
//
//            val board3 =
//                Board.create(
//                    title = "Title 3",
//                    content = "Content 3",
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            board3.incrementViewCount() // 조회수 1
//            board3.incrementViewCount() // 조회수 2
//            board3.incrementViewCount() // 조회수 3
//            saveAndGetAfterCommit(board3) { boardRepository.save(it) }
//
//            // When
//            val topBoards = boardRepository.findTopNByOrderByViewCountDesc(2)
//
//            // Then
//            assertEquals(2, topBoards.size)
//            assertEquals(board3.id, topBoards[0].id)
//            assertEquals(board2.id, topBoards[1].id)
//        }
//
//        @Test
//        @DisplayName("키워드로 제목과 내용을 검색할 수 있다")
//        fun searchBoardSuccess() {
//            // Given
//            val board1 = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val board2 =
//                Board.create(
//                    title = "Test Different Title",
//                    content = "Different Content with test keyword",
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            val board3 =
//                Board.create(
//                    title = "Unrelated",
//                    content = "Test Unrelated",
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(board2) { boardRepository.save(it) }
//            saveAndGetAfterCommit(board3) { boardRepository.save(it) }
//
//            // When - 제목으로 검색
//            val titleResults = boardRepository.search("Test", PageRequest.of(0, 10))
//
//            // Then
//            // 예시에서는 모든 게시글이 검색되도록 되어 있음
//            assertEquals(3, titleResults.content.size)
//            assertEquals(board1.id, titleResults.content[0].id)
//        }
//
//        @Test
//        @DisplayName("대소문자를 구분하지 않고 검색할 수 있다")
//        fun searchCaseInsensitiveSuccess() {
//            // Given
//            saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            // When
//            val upperResults = boardRepository.search("TEST", PageRequest.of(0, 10))
//            val lowerResults = boardRepository.search("test", PageRequest.of(0, 10))
//            val mixedResults = boardRepository.search("TeSt", PageRequest.of(0, 10))
//
//            // Then
//            assertTrue(upperResults.content.isNotEmpty())
//            assertEquals(upperResults.content.size, lowerResults.content.size)
//            assertEquals(upperResults.content.size, mixedResults.content.size)
//        }
//    }
//
//    @Nested
//    @DisplayName("게시글 수정/삭제 테스트")
//    inner class UpdateDeleteBoardTest {
//        @Test
//        @DisplayName("게시글 내용을 수정할 수 있다")
//        fun updateContentSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            // When
//            val foundBoard = boardRepository.findById(savedBoard.id)
//            foundBoard.updateContent("New Title", "New Content")
//            val updatedBoard = saveAndGetAfterCommit(foundBoard) { boardRepository.save(it) }
//
//            // Then
//            assertEquals("New Title", updatedBoard.title)
//            assertEquals("New Content", updatedBoard.content)
//        }
//
//        @Test
//        @DisplayName("게시글의 조회수를 증가시킬 수 있다")
//        fun incrementViewCountSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            // When
//            val foundBoard = boardRepository.findById(savedBoard.id)
//            foundBoard.incrementViewCount()
//            val updatedBoard = saveAndGetAfterCommit(foundBoard) { boardRepository.save(it) }
//
//            // Then
//            assertEquals(1, updatedBoard.viewCount)
//        }
//
//        @Test
//        @DisplayName("게시글의 좋아요 수를 증가시킬 수 있다")
//        fun incrementLikeCountSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            // When
//            val foundBoard = boardRepository.findById(savedBoard.id)
//            foundBoard.incrementLikeCount()
//            val updatedBoard = saveAndGetAfterCommit(foundBoard) { boardRepository.save(it) }
//
//            // Then
//            assertEquals(1, updatedBoard.likeCount)
//        }
//
//        @Test
//        @DisplayName("게시글을 삭제할 수 있다")
//        fun deleteBoardSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            // When
//            boardRepository.delete(savedBoard)
//            flushAndClear()
//
//            // Then
//            assertFailsWith<JpaObjectRetrievalFailureException> {
//                boardRepository.findById(savedBoard.id)
//            }
//        }
//
//        @Test
//        @DisplayName("게시글 삭제시 연관된 댓글도 함께 삭제된다")
//        fun deleteBoardWithCommentsSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val comment =
//                com.example.jobstat.community.comment.entity.Comment.create(
//                    content = "Test Comment",
//                    author = "commentUser",
//                    password = null,
//                    board = savedBoard,
//                    userId = 1L,
//                )
//            val savedComment = saveAndGetAfterCommit(comment) { commentRepository.save(it) }
//            savedBoard.addComment(savedComment)
//            // When
//            boardRepository.delete(savedBoard)
//            flushAndClear()
//
//            // Then
//            assertFailsWith<JpaObjectRetrievalFailureException> {
//                boardRepository.findById(savedBoard.id)
//            }
//            assertTrue(commentRepository.findByBoardId(savedBoard.id, PageRequest.of(0, 10)).isEmpty)
//        }
//    }
//
//    @Nested
//    @DisplayName("게시글 통계 테스트")
//    inner class BoardStatisticsTest {
//        @Test
//        @DisplayName("작성자별 게시글 수를 계산할 수 있다")
//        fun countByAuthorSuccess() {
//            // Given
//            saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val board2 =
//                Board.create(
//                    title = "Title 2",
//                    content = "Content 2",
//                    author = "testUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(board2) { boardRepository.save(it) }
//
//            val board3 =
//                Board.create(
//                    title = "Title 3",
//                    content = "Content 3",
//                    author = "anotherUser",
//                    password = null,
//                    category = testCategory,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(board3) { boardRepository.save(it) }
//
//            // When
//            val testUserCount = boardRepository.countByAuthor("testUser")
//            val anotherUserCount = boardRepository.countByAuthor("anotherUser")
//            val nonExistentUserCount = boardRepository.countByAuthor("nonexistent")
//
//            // Then
//            assertEquals(2, testUserCount)
//            assertEquals(1, anotherUserCount)
//            assertEquals(0, nonExistentUserCount)
//        }
//
//        @Test
//        @DisplayName("동일 작성자의 중복 제목 여부를 확인할 수 있다")
//        fun checkDuplicateTitleByAuthorSuccess() {
//            // Given
//            saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            // When & Then
//            assertTrue(boardRepository.existsByAuthorAndTitle("testUser", "Test Title"))
//            assertFalse(boardRepository.existsByAuthorAndTitle("testUser", "Different Title"))
//            assertFalse(boardRepository.existsByAuthorAndTitle("anotherUser", "Test Title"))
//        }
//    }
//
//    @Nested
//    @DisplayName("게시글 상세 조회 테스트")
//    inner class BoardDetailQueryTest {
//        @Test
//        @DisplayName("댓글을 포함한 게시글 상세 정보를 조회할 수 있다")
//        fun findAllWithDetailsSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val comment1 =
//                com.example.jobstat.community.comment.entity.Comment.create(
//                    content = "Comment 1",
//                    author = "user1",
//                    password = null,
//                    board = savedBoard,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(comment1) { commentRepository.save(it) }
//
//            val comment2 =
//                com.example.jobstat.community.comment.entity.Comment.create(
//                    content = "Comment 2",
//                    author = "user2",
//                    password = null,
//                    board = savedBoard,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(comment2) { commentRepository.save(it) }
//
//            // When
//            val boardsWithDetails = boardRepository.findAllWithDetails(PageRequest.of(0, 10))
//
//            // Then
//            assertEquals(1, boardsWithDetails.content.size)
//            assertEquals(2, boardsWithDetails.content[0].comments.size)
//        }
//
//        @Test
//        @DisplayName("카테고리별로 댓글을 포함한 게시글을 조회할 수 있다")
//        fun findByCategoryWithCommentsSuccess() {
//            // Given
//            val savedBoard = saveAndGetAfterCommit(testBoard) { boardRepository.save(it) }
//
//            val comment =
//                com.example.jobstat.community.comment.entity.Comment.create(
//                    content = "Test Comment",
//                    author = "commentUser",
//                    password = null,
//                    board = savedBoard,
//                    userId = 1L,
//                )
//            saveAndGetAfterCommit(comment) { commentRepository.save(it) }
//
//            // When
//            val boardsWithComments = boardRepository.findByCategoryIdWithComments(testCategory.id)
//
//            // Then
//            assertEquals(1, boardsWithComments.size)
//            assertEquals(1, boardsWithComments[0].comments.size)
//            assertEquals(comment.content, boardsWithComments[0].comments[0].content)
//        }
//    }
//}
