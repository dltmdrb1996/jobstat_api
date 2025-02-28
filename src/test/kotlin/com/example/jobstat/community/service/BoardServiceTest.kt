package com.example.jobstat.community.service

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("BoardService 테스트")
class BoardServiceTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var testCategory: BoardCategory

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)

        // 카테고리 생성 – Fixture를 사용 (CategoryFixture는 name, displayName, description을 모두 받음)
        testCategory =
            categoryRepository.save(
                CategoryFixture
                    .aCategory()
                    .withName("TEST_CATEGORY")
                    .withDisplayName("Test Category")
                    .withDescription("Test Description")
                    .create(),
            )
    }

    @Nested
    @DisplayName("게시글 생성")
    inner class CreateBoard {
        @Test
        @DisplayName("유효한 정보로 게시글을 생성할 수 있다")
        fun createValidBoard() {
            val createdBoard =
                boardService.createBoard(
                    title = "테스트 제목",
                    content = "테스트 내용",
                    author = "테스트사용자",
                    categoryId = testCategory.id,
                    password = null,
                )

            assertEquals("테스트 제목", createdBoard.title)
            assertEquals("테스트 내용", createdBoard.content)
            assertEquals("테스트사용자", createdBoard.author)
            assertEquals(testCategory.id, createdBoard.category.id)
            assertEquals(0, createdBoard.viewCount)
            assertEquals(0, createdBoard.likeCount)
        }

        @Test
        @DisplayName("비밀글로 게시글을 생성할 수 있다")
        fun createPasswordProtectedBoard() {
            val createdBoard =
                boardService.createBoard(
                    title = "비밀 게시글",
                    content = "비밀 내용",
                    author = "테스트사용자",
                    categoryId = testCategory.id,
                    password = "test123!",
                )

            assertEquals("비밀 게시글", createdBoard.title)
            assertNotNull(createdBoard.password)
            assertEquals("test123!", createdBoard.password)
        }

        @Test
        @DisplayName("카테고리 없이 게시글을 생성할 수 없다")
        fun cannotCreateBoardWithoutCategory() {
            val ex =
                assertFailsWith<AppException> {
                    boardService.createBoard(
                        title = "No Category",
                        content = "Content",
                        author = "testUser",
                        categoryId = null,
                        password = null,
                    )
                }
            assertEquals(ErrorCode.INVALID_ARGUMENT, ex.errorCode)
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 게시글을 생성할 수 없다")
        fun cannotCreateBoardWithNonExistentCategory() {
            assertFailsWith<EntityNotFoundException> {
                boardService.createBoard(
                    title = "Test Title",
                    content = "Test Content",
                    author = "testUser",
                    categoryId = 999L,
                    password = null,
                )
            }
        }

        @Test
        @DisplayName("제목이 100자를 초과하면 생성할 수 없다")
        fun cannotCreateBoardWithTooLongTitle() {
            val tooLongTitle = "a".repeat(101)
            assertFailsWith<IllegalArgumentException> {
                boardService.createBoard(
                    title = tooLongTitle,
                    content = "Test Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
        }

        @Test
        @DisplayName("내용이 5000자를 초과하면 생성할 수 없다")
        fun cannotCreateBoardWithTooLongContent() {
            val tooLongContent = "a".repeat(5001)
            assertFailsWith<IllegalArgumentException> {
                boardService.createBoard(
                    title = "Test Title",
                    content = tooLongContent,
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
        }

        @Test
        @DisplayName("빈 제목으로 게시글을 생성할 수 없다")
        fun cannotCreateBoardWithEmptyTitle() {
            assertFailsWith<IllegalArgumentException> {
                boardService.createBoard(
                    title = "",
                    content = "Test Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                boardService.createBoard(
                    title = "   ",
                    content = "Test Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
        }

        @Test
        @DisplayName("빈 내용으로 게시글을 생성할 수 없다")
        fun cannotCreateBoardWithEmptyContent() {
            assertFailsWith<IllegalArgumentException> {
                boardService.createBoard(
                    title = "Test Title",
                    content = "",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                boardService.createBoard(
                    title = "Test Title",
                    content = "   ",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    inner class GetBoard {
        @Test
        @DisplayName("ID로 게시글을 조회할 수 있다")
        fun getBoardById() {
            val savedBoard =
                boardService.createBoard(
                    title = "Test Title",
                    content = "Test Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            val foundBoard = boardService.getBoardById(savedBoard.id)
            assertEquals(savedBoard.id, foundBoard.id)
            assertEquals(savedBoard.title, foundBoard.title)
            assertEquals(savedBoard.content, foundBoard.content)
            assertEquals(savedBoard.author, foundBoard.author)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 예외가 발생한다")
        fun cannotGetBoardByNonExistentId() {
            assertFailsWith<EntityNotFoundException> {
                boardService.getBoardById(999L)
            }
        }

        @Test
        @DisplayName("작성자로 게시글을 조회할 수 있다")
        fun getBoardsByAuthor() {
            repeat(3) { idx ->
                boardService.createBoard(
                    title = "Title $idx",
                    content = "Content $idx",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
            boardService.createBoard(
                title = "Other Title",
                content = "Other Content",
                author = "otherUser",
                categoryId = testCategory.id,
                password = null,
            )
            val foundBoards = boardService.getBoardsByAuthor("testUser", PageRequest.of(0, 10))
            assertEquals(3, foundBoards.content.size)
            assertTrue(foundBoards.content.all { it.author == "testUser" })
        }

        @Test
        @DisplayName("존재하지 않는 작성자로 조회시 빈 결과를 반환한다")
        fun getBoardsByNonExistentAuthor() {
            boardService.createBoard(
                title = "Test Title",
                content = "Test Content",
                author = "testUser",
                categoryId = testCategory.id,
                password = null,
            )
            val foundBoards = boardService.getBoardsByAuthor("nonexistent", PageRequest.of(0, 10))
            assertTrue(foundBoards.content.isEmpty())
        }

        @Test
        @DisplayName("카테고리로 게시글을 조회할 수 있다")
        fun getBoardsByCategory() {
            val category2 =
                categoryRepository.save(
                    CategoryFixture
                        .aCategory()
                        .withName("Category 2")
                        .withDisplayName("Category 2")
                        .withDescription("Desc")
                        .create(),
                )
            repeat(2) { idx ->
                boardService.createBoard(
                    title = "Title $idx",
                    content = "Content $idx",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
            boardService.createBoard(
                title = "Other Title",
                content = "Other Content",
                author = "testUser",
                categoryId = category2.id,
                password = null,
            )
            val foundBoards = boardService.getBoardsByCategory(testCategory.id, PageRequest.of(0, 10))
            assertEquals(2, foundBoards.content.size)
            assertTrue(foundBoards.content.all { it.category.id == testCategory.id })
        }

        @Test
        @DisplayName("작성자와 카테고리로 게시글을 조회할 수 있다")
        fun getBoardsByAuthorAndCategory() {
            val category2 =
                categoryRepository.save(
                    CategoryFixture
                        .aCategory()
                        .withName("Category 2")
                        .withDisplayName("Category 2")
                        .withDescription("Desc")
                        .create(),
                )
            repeat(2) { idx ->
                boardService.createBoard(
                    title = "Title $idx",
                    content = "Content $idx",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
            boardService.createBoard(
                title = "Other Title",
                content = "Other Content",
                author = "testUser",
                categoryId = category2.id,
                password = null,
            )
            boardService.createBoard(
                title = "Other Author",
                content = "Other Content",
                author = "otherUser",
                categoryId = testCategory.id,
                password = null,
            )
            val foundBoards = boardService.getBoardsByAuthorAndCategory("testUser", testCategory.id, PageRequest.of(0, 10))
            assertEquals(2, foundBoards.content.size)
            assertTrue(foundBoards.content.all { it.author == "testUser" && it.category.id == testCategory.id })
        }

        @Test
        @DisplayName("페이지별로 모든 게시글을 조회할 수 있다")
        fun getAllBoardsWithPaging() {
            repeat(15) { idx ->
                boardService.createBoard(
                    title = "Title $idx",
                    content = "Content $idx",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            }
            val page1 = boardService.getAllBoards(PageRequest.of(0, 10))
            val page2 = boardService.getAllBoards(PageRequest.of(1, 10))
            assertEquals(10, page1.content.size)
            assertEquals(5, page2.content.size)
            assertEquals(15, page1.totalElements)
            assertEquals(2, page1.totalPages)
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    inner class UpdateBoard {
        @Test
        @DisplayName("제목과 내용을 수정할 수 있다")
        fun updateTitleAndContent() {
            val savedBoard =
                boardService.createBoard(
                    title = "Original Title",
                    content = "Original Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            val updatedBoard = boardService.updateBoard(savedBoard.id, "New Title", "New Content")
            assertEquals("New Title", updatedBoard.title)
            assertEquals("New Content", updatedBoard.content)
        }

        @Test
        @DisplayName("존재하지 않는 게시글은 수정할 수 없다")
        fun cannotUpdateNonExistentBoard() {
            assertFailsWith<EntityNotFoundException> {
                boardService.updateBoard(999L, "New Title", "New Content")
            }
        }

        @Test
        @DisplayName("내용이 5000자를 초과하면 수정할 수 없다")
        fun cannotUpdateWithTooLongContent() {
            val savedBoard =
                boardService.createBoard(
                    title = "Original Title",
                    content = "Original Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            val tooLongContent = "a".repeat(5001)
            assertFailsWith<IllegalArgumentException> {
                boardService.updateBoard(savedBoard.id, "New Title", tooLongContent)
            }
        }

        @Test
        @DisplayName("빈 제목으로 수정할 수 없다")
        fun cannotUpdateWithEmptyTitle() {
            val savedBoard =
                boardService.createBoard(
                    title = "Original Title",
                    content = "Original Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            assertFailsWith<IllegalArgumentException> {
                boardService.updateBoard(savedBoard.id, "", "New Content")
            }
            assertFailsWith<IllegalArgumentException> {
                boardService.updateBoard(savedBoard.id, "   ", "New Content")
            }
        }

        @Test
        @DisplayName("빈 내용으로 수정할 수 없다")
        fun cannotUpdateWithEmptyContent() {
            val savedBoard =
                boardService.createBoard(
                    title = "Original Title",
                    content = "Original Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            assertFailsWith<IllegalArgumentException> {
                boardService.updateBoard(savedBoard.id, "New Title", "")
            }
            assertFailsWith<IllegalArgumentException> {
                boardService.updateBoard(savedBoard.id, "New Title", "   ")
            }
        }

        @Test
        @DisplayName("작성자와 카테고리는 수정 후에도 변경되지 않는다")
        fun authorAndCategoryRemainUnchanged() {
            val savedBoard =
                boardService.createBoard(
                    title = "Original Title",
                    content = "Original Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
            val originalAuthor = savedBoard.author
            val originalCategory = savedBoard.category
            val updatedBoard = boardService.updateBoard(savedBoard.id, "New Title", "New Content")
            assertEquals(originalAuthor, updatedBoard.author)
            assertEquals(originalCategory.id, updatedBoard.category.id)
        }

        @Test
        @DisplayName("조회수와 좋아요 수는 수정시 유지된다")
        fun viewAndLikeCountsRemainUnchanged() {
            val board =
                boardService.createBoard(
                    title = "Original Title",
                    content = "Original Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                ) as Board
            repeat(3) { board.incrementViewCount() }
            repeat(2) { board.incrementLikeCount() }
            boardRepository.save(board)
            val updatedBoard = boardService.updateBoard(board.id, "New Title", "New Content")
            assertEquals(3, updatedBoard.viewCount)
            assertEquals(2, updatedBoard.likeCount)
        }

        @Nested
        @DisplayName("게시글 삭제")
        inner class DeleteBoard {
            @Test
            @DisplayName("게시글을 삭제할 수 있다")
            fun deleteBoard() {
                val savedBoard =
                    boardService.createBoard(
                        title = "To be deleted",
                        content = "Content",
                        author = "testUser",
                        categoryId = testCategory.id,
                        password = null,
                    )
                boardService.deleteBoard(savedBoard.id)
                assertFailsWith<EntityNotFoundException> {
                    boardService.getBoardById(savedBoard.id)
                }
            }

            @Test
            @DisplayName("존재하지 않는 게시글 삭제시 예외가 발생하지 않는다")
            fun deleteNonExistentBoard() {
                assertDoesNotThrow {
                    boardService.deleteBoard(999L)
                }
            }

            @Test
            @DisplayName("삭제된 게시글과 동일한 제목으로 새 게시글을 생성할 수 있다")
            fun canCreateNewBoardWithDeletedTitle() {
                val savedBoard =
                    boardService.createBoard(
                        title = "Same Title",
                        content = "Content",
                        author = "testUser",
                        categoryId = testCategory.id,
                        password = null,
                    )
                boardService.deleteBoard(savedBoard.id)
                assertDoesNotThrow {
                    boardService.createBoard(
                        title = "Same Title",
                        content = "New Content",
                        author = "testUser",
                        categoryId = testCategory.id,
                        password = null,
                    )
                }
            }

            @Test
            @DisplayName("여러 게시글을 한 번에 삭제할 수 있다")
            fun deleteManyBoards() {
                val boards =
                    (1..5).map { idx ->
                        boardService.createBoard(
                            title = "Title $idx",
                            content = "Content $idx",
                            author = "testUser",
                            categoryId = testCategory.id,
                            password = null,
                        )
                    }
                boards.forEach { boardService.deleteBoard(it.id) }
                val foundBoards = boardService.getAllBoards(PageRequest.of(0, 10))
                assertTrue(foundBoards.content.isEmpty())
                boards.forEach { board ->
                    assertFailsWith<EntityNotFoundException> {
                        boardService.getBoardById(board.id)
                    }
                }
            }
        }

        @Nested
        @DisplayName("조회수/좋아요")
        inner class ViewAndLikeCount {
            @Test
            @DisplayName("조회수를 증가시킬 수 있다")
            fun incrementViewCount() {
                val savedBoard =
                    boardService.createBoard(
                        title = "Test Title",
                        content = "Content",
                        author = "testUser",
                        categoryId = testCategory.id,
                        password = null,
                    )
                val updatedBoard = boardService.incrementViewCount(savedBoard.id)
                assertEquals(1, updatedBoard.viewCount)
            }

            @Test
            @DisplayName("조회수를 여러 번 증가시킬 수 있다")
            fun incrementViewCountMultipleTimes() {
                var updatedBoard =
                    boardService.createBoard(
                        title = "Test Title",
                        content = "Content",
                        author = "testUser",
                        categoryId = testCategory.id,
                        password = null,
                    )
                repeat(3) {
                    updatedBoard = boardService.incrementViewCount(updatedBoard.id)
                }
                assertEquals(3, updatedBoard.viewCount)
            }

            @Test
            @DisplayName("존재하지 않는 게시글의 조회수는 증가시킬 수 없다")
            fun cannotIncrementViewCountOfNonExistentBoard() {
                assertFailsWith<EntityNotFoundException> {
                    boardService.incrementViewCount(999L)
                }
            }

            @Test
            @DisplayName("좋아요 수를 증가시킬 수 있다")
            fun incrementLikeCount() {
                val savedBoard =
                    boardService.createBoard(
                        title = "Test Title",
                        content = "Content",
                        author = "testUser",
                        categoryId = testCategory.id,
                        password = null,
                    )
                val updatedBoard = boardService.incrementLikeCount(savedBoard.id)
                assertEquals(1, updatedBoard.likeCount)
            }

            @Test
            @DisplayName("좋아요 수를 여러 번 증가시킬 수 있다")
            fun incrementLikeCountMultipleTimes() {
                var updatedBoard =
                    boardService.createBoard(
                        title = "Test Title",
                        content = "Content",
                        author = "testUser",
                        categoryId = testCategory.id,
                        password = null,
                    )
                repeat(5) {
                    updatedBoard = boardService.incrementLikeCount(updatedBoard.id)
                }
                assertEquals(5, updatedBoard.likeCount)
            }

            @Test
            @DisplayName("존재하지 않는 게시글의 좋아요는 증가시킬 수 없다")
            fun cannotIncrementLikeCountOfNonExistentBoard() {
                assertFailsWith<EntityNotFoundException> {
                    boardService.incrementLikeCount(999L)
                }
            }

            @Test
            @DisplayName("조회수 상위 N개 게시글을 조회할 수 있다")
            fun getTopNBoardsByViews() {
                val board1 =
                    boardService.createBoard(
                        title = "Title 1",
                        content = "Content 1",
                        author = "user1",
                        categoryId = testCategory.id,
                        password = null,
                    ) as Board
                repeat(5) { board1.incrementViewCount() }

                val board2 =
                    boardService.createBoard(
                        title = "Title 2",
                        content = "Content 2",
                        author = "user2",
                        categoryId = testCategory.id,
                        password = null,
                    ) as Board
                repeat(10) { board2.incrementViewCount() }

                val board3 =
                    boardService.createBoard(
                        title = "Title 3",
                        content = "Content 3",
                        author = "user3",
                        categoryId = testCategory.id,
                        password = null,
                    ) as Board
                repeat(3) { board3.incrementViewCount() }

                boardRepository.save(board1)
                boardRepository.save(board2)
                boardRepository.save(board3)

                val topBoards = boardService.getTopNBoardsByViews(2)
                assertEquals(2, topBoards.size)
                assertEquals(board2.id, topBoards[0].id) // 10회
                assertEquals(board1.id, topBoards[1].id) // 5회
            }

            @Test
            @DisplayName("조회수가 같은 경우 먼저 생성된 게시글이 우선된다")
            fun topBoardsOrderedByCreatedAtWhenViewCountEqual() {
                val board1 =
                    boardService.createBoard(
                        title = "Title 1",
                        content = "Content 1",
                        author = "user1",
                        categoryId = testCategory.id,
                        password = null,
                    ) as Board
                repeat(5) { board1.incrementViewCount() }

                val board2 =
                    boardService.createBoard(
                        title = "Title 2",
                        content = "Content 2",
                        author = "user2",
                        categoryId = testCategory.id,
                        password = null,
                    ) as Board
                repeat(5) { board2.incrementViewCount() }

                boardRepository.save(board1)
                boardRepository.save(board2)

                val topBoards = boardService.getTopNBoardsByViews(2)
                assertEquals(2, topBoards.size)
                // board1가 먼저 생성되었으므로 board1 우선
                assertEquals(board1.id, topBoards[0].id)
                assertEquals(board2.id, topBoards[1].id)
            }

            @Test
            @DisplayName("게시글이 N개 미만이면 전체를 반환한다")
            fun getTopNBoardsWhenLessThanN() {
                val board1 =
                    boardService.createBoard(
                        title = "Title 1",
                        content = "Content 1",
                        author = "user1",
                        categoryId = testCategory.id,
                        password = null,
                    ) as Board
                repeat(5) { board1.incrementViewCount() }
                boardRepository.save(board1)
                val topBoards = boardService.getTopNBoardsByViews(3)
                assertEquals(1, topBoards.size)
                assertEquals(board1.id, topBoards[0].id)
            }

            @Test
            @DisplayName("게시글이 없으면 빈 리스트를 반환한다")
            fun getTopNBoardsWhenNoBoards() {
                val topBoards = boardService.getTopNBoardsByViews(3)
                assertTrue(topBoards.isEmpty())
            }
        }

        @Nested
        @DisplayName("게시글 검색")
        inner class SearchBoard {
            @Test
            @DisplayName("제목으로 검색할 수 있다")
            fun searchBoardsByTitle() {
                boardService.createBoard(
                    title = "Test Title",
                    content = "Normal Content",
                    author = "user1",
                    categoryId = testCategory.id,
                    password = null,
                )
                boardService.createBoard(
                    title = "Another Title",
                    content = "Normal Content",
                    author = "user2",
                    categoryId = testCategory.id,
                    password = null,
                )
                val foundBoards = boardService.searchBoards("Test", PageRequest.of(0, 10))
                assertEquals(1, foundBoards.content.size)
                assertTrue(foundBoards.content[0].title.contains("Test"))
            }

            @Test
            @DisplayName("내용으로 검색할 수 있다")
            fun searchBoardsByContent() {
                boardService.createBoard(
                    title = "Title 1",
                    content = "Test Content",
                    author = "user1",
                    categoryId = testCategory.id,
                    password = null,
                )
                boardService.createBoard(
                    title = "Title 2",
                    content = "Normal Content",
                    author = "user2",
                    categoryId = testCategory.id,
                    password = null,
                )
                val foundBoards = boardService.searchBoards("Test", PageRequest.of(0, 10))
                assertEquals(1, foundBoards.content.size)
                assertTrue(foundBoards.content[0].content.contains("Test"))
            }

            @Test
            @DisplayName("제목과 내용 모두에 포함된 경우 검색된다")
            fun searchBoardsByTitleAndContent() {
                boardService.createBoard(
                    title = "Test Title",
                    content = "Test Content",
                    author = "user1",
                    categoryId = testCategory.id,
                    password = null,
                )
                val foundBoards = boardService.searchBoards("Test", PageRequest.of(0, 10))
                assertEquals(1, foundBoards.content.size)
                assertTrue(foundBoards.content[0].title.contains("Test"))
                assertTrue(foundBoards.content[0].content.contains("Test"))
            }

            @Test
            @DisplayName("검색어가 없으면 모든 게시글을 반환한다")
            fun searchBoardsWithEmptyKeyword() {
                repeat(3) { idx ->
                    boardService.createBoard(
                        title = "Title $idx",
                        content = "Content $idx",
                        author = "user$idx",
                        categoryId = testCategory.id,
                        password = null,
                    )
                }
                val foundBoards = boardService.searchBoards("", PageRequest.of(0, 10))
                assertEquals(3, foundBoards.content.size)
            }

            @Test
            @DisplayName("대소문자를 구분하지 않고 검색할 수 있다")
            fun searchBoardsCaseInsensitive() {
                boardService.createBoard(
                    title = "TEST Title",
                    content = "Test Content",
                    author = "user1",
                    categoryId = testCategory.id,
                    password = null,
                )
                val upperResult = boardService.searchBoards("TEST", PageRequest.of(0, 10))
                val lowerResult = boardService.searchBoards("test", PageRequest.of(0, 10))
                val mixedResult = boardService.searchBoards("TeSt", PageRequest.of(0, 10))
                assertEquals(1, upperResult.content.size)
                assertEquals(1, lowerResult.content.size)
                assertEquals(1, mixedResult.content.size)
            }

            @Test
            @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
            fun searchReturnsEmptyResults() {
                boardService.createBoard(
                    title = "Title",
                    content = "Content",
                    author = "user1",
                    categoryId = testCategory.id,
                    password = null,
                )
                val results = boardService.searchBoards("nonexistent", PageRequest.of(0, 10))
                assertTrue(results.content.isEmpty())
                assertEquals(0, results.totalElements)
            }

            @Test
            @DisplayName("검색 결과는 페이징 처리된다")
            fun searchResultsPaging() {
                repeat(15) { idx ->
                    boardService.createBoard(
                        title = "Test Title $idx",
                        content = "Content $idx",
                        author = "user$idx",
                        categoryId = testCategory.id,
                        password = null,
                    )
                }
                val page1 = boardService.searchBoards("Test", PageRequest.of(0, 10))
                val page2 = boardService.searchBoards("Test", PageRequest.of(1, 10))
                assertEquals(10, page1.content.size)
                assertEquals(5, page2.content.size)
                assertEquals(15, page1.totalElements)
                assertEquals(2, page1.totalPages)
            }
        }

        @Nested
        @DisplayName("게시글 통계")
        inner class BoardStatistics {
            @Test
            @DisplayName("작성자별 게시글 수를 계산할 수 있다")
            fun countBoardsByAuthor() {
                repeat(3) {
                    boardService.createBoard(
                        title = "Title $it",
                        content = "Content $it",
                        author = "user1",
                        categoryId = testCategory.id,
                        password = null,
                    )
                }
                boardService.createBoard(
                    title = "Other Title",
                    content = "Other Content",
                    author = "user2",
                    categoryId = testCategory.id,
                    password = null,
                )
                val user1Count = boardService.countBoardsByAuthor("user1")
                val user2Count = boardService.countBoardsByAuthor("user2")
                val nonExistentCount = boardService.countBoardsByAuthor("nonexistent")
                assertEquals(3, user1Count)
                assertEquals(1, user2Count)
                assertEquals(0, nonExistentCount)
            }

            @Test
            @DisplayName("작성자와 제목으로 중복 여부를 확인할 수 있다")
            fun checkBoardTitleDuplication() {
                boardService.createBoard(
                    title = "Duplicate Check",
                    content = "Content",
                    author = "testUser",
                    categoryId = testCategory.id,
                    password = null,
                )
                assertTrue(boardService.isBoardTitleDuplicated("testUser", "Duplicate Check"))
                assertFalse(boardService.isBoardTitleDuplicated("testUser", "New Title"))
                assertFalse(boardService.isBoardTitleDuplicated("otherUser", "Duplicate Check"))
            }

            @Test
            @DisplayName("삭제된 게시글은 통계에서 제외된다")
            fun excludeDeletedBoardsFromStatistics() {
                repeat(3) {
                    boardService.createBoard(
                        title = "Title $it",
                        content = "Content $it",
                        author = "user1",
                        categoryId = testCategory.id,
                        password = null,
                    )
                }
                val boardToDelete =
                    boardService.createBoard(
                        title = "To Delete",
                        content = "Content",
                        author = "user1",
                        categoryId = testCategory.id,
                        password = null,
                    )
                boardService.deleteBoard(boardToDelete.id)
                val count = boardService.countBoardsByAuthor("user1")
                assertEquals(3, count)
            }
        }
    }
}
