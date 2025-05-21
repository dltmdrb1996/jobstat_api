package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.BoardFixture
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.board.repository.FakeBoardRepository
import com.example.jobstat.community.board.repository.FakeCategoryRepository
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.statistics_read.core.core_model.BoardRankingMetric
import com.example.jobstat.statistics_read.core.core_model.BoardRankingPeriod
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import kotlin.test.assertTrue

@DisplayName("BoardService 테스트 (Refactored)")
class BoardServiceTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    private lateinit var boardService: BoardService
    private lateinit var testCategory: BoardCategory

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)

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

    @AfterEach
    fun tearDown() {
        boardRepository.clear()
        categoryRepository.clear()
    }

    @Nested
    @DisplayName("게시글 생성 (createBoard)")
    inner class CreateBoard {
        @Test
        @DisplayName("성공: 유효한 정보 (userId 포함)로 게시글 생성")
        fun `given valid details with userId, when createBoard, then return new board`() {
            // Given
            val title = "새 게시글 제목"
            val content = "새 게시글 내용입니다."
            val author = "작성자1"
            val categoryId = testCategory.id
            val userId = 101L

            // When
            val createdBoard = boardService.createBoard(title, content, author, categoryId, null, userId)

            // Then
            assertNotNull(createdBoard)
            assertTrue(createdBoard.id > 0)
            assertEquals(title, createdBoard.title)
            assertEquals(content, createdBoard.content)
            assertEquals(author, createdBoard.author)
            assertEquals(categoryId, createdBoard.category.id)
            assertEquals(userId, createdBoard.userId)
            assertNull(createdBoard.password)
            assertEquals(0, createdBoard.viewCount)
            assertEquals(0, createdBoard.likeCount)
            assertEquals(0, createdBoard.commentCount)

            // Verify persistence
            val found = boardRepository.findById(createdBoard.id)
            assertEquals(createdBoard.id, found.id)
            assertEquals(userId, found.userId)
        }

        @Test
        @DisplayName("성공: 비밀번호 포함하여 게시글 생성")
        fun `given valid details with password, when createBoard, then return board with password`() {
            // Given
            val title = "비밀글 제목"
            val content = "비밀 내용"
            val author = "작성자2"
            val categoryId = testCategory.id
            val password = "pass!@#$"
            val userId = 102L

            // When
            val createdBoard = boardService.createBoard(title, content, author, categoryId, password, userId)

            // Then
            assertNotNull(createdBoard)
            assertEquals(title, createdBoard.title)
            assertNotNull(createdBoard.password)
            assertEquals(password, createdBoard.password)
            assertEquals(userId, createdBoard.userId)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리 ID 사용 시 EntityNotFoundException 발생")
        fun `given non-existent categoryId, when createBoard, then throw EntityNotFoundException`() {
            // Given
            val nonExistentCategoryId = 999L

            assertThrows<EntityNotFoundException> {
                boardService.createBoard("제목", "내용", "작성자", nonExistentCategoryId, null, 1L)
            }
        }

        @Test
        @DisplayName("실패: 제목 또는 내용이 제약조건 위반 시 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given invalid title or content, when createBoard, then throw IllegalArgumentException`() {
            // Given
            val validTitle = "Valid Title"
            val validContent = "Valid Content"
            val validAuthor = "Author"
            val validCategoryId = testCategory.id
            val blankTitle = "   "
            val tooLongTitle = "a".repeat(101)
            val blankContent = ""
            val tooLongContent = "b".repeat(5001)

            // When & Then
            assertThrows<IllegalArgumentException>("Blank Title") {
                boardService.createBoard(blankTitle, validContent, validAuthor, validCategoryId, null, 1L)
            }
            assertThrows<IllegalArgumentException>("Too Long Title") {
                boardService.createBoard(tooLongTitle, validContent, validAuthor, validCategoryId, null, 1L)
            }
            assertThrows<IllegalArgumentException>("Blank Content") {
                boardService.createBoard(validTitle, blankContent, validAuthor, validCategoryId, null, 1L)
            }
            assertThrows<IllegalArgumentException>("Too Long Content") {
                boardService.createBoard(validTitle, tooLongContent, validAuthor, validCategoryId, null, 1L)
            }
        }
    }

    @Nested
    @DisplayName("게시글 수정 (updateBoard)")
    inner class UpdateBoard {
        @Test
        @DisplayName("성공: 존재하는 게시글의 제목과 내용을 수정한다")
        fun `given existing board and valid details, when updateBoard, then return updated board`() {
            // Given
            val originalBoard = boardService.createBoard("원본 제목", "원본 내용", "author", testCategory.id, null, 1L)
            val newTitle = "수정된 제목"
            val newContent = "수정된 내용입니다."

            // When
            val updatedBoard = boardService.updateBoard(originalBoard.id, newTitle, newContent)

            // Then
            assertNotNull(updatedBoard)
            assertEquals(originalBoard.id, updatedBoard.id)
            assertEquals(newTitle, updatedBoard.title)
            assertEquals(newContent, updatedBoard.content)
            // Verify other fields unchanged
            assertEquals(originalBoard.author, updatedBoard.author)
            assertEquals(originalBoard.category.id, updatedBoard.category.id)
            assertEquals(originalBoard.userId, updatedBoard.userId)
            assertEquals(originalBoard.password, updatedBoard.password)
            assertEquals(originalBoard.viewCount, updatedBoard.viewCount)
            assertEquals(originalBoard.likeCount, updatedBoard.likeCount)

            // Verify persistence
            val found = boardRepository.findById(originalBoard.id)
            assertEquals(newTitle, found.title)
            assertEquals(newContent, found.content)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게시글 ID로 수정 시 EntityNotFoundException 발생")
        fun `given non-existent id, when updateBoard, then throw EntityNotFoundException`() {
            // Given
            val nonExistentId = 999L

            assertThrows<EntityNotFoundException> {
                boardService.updateBoard(nonExistentId, "새 제목", "새 내용")
            }
        }

        @Test
        @DisplayName("실패: 제목 또는 내용 제약조건 위반 시 IllegalArgumentException 발생 (Entity 레벨)")
        fun `given invalid title or content, when updateBoard, then throw IllegalArgumentException`() {
            // Given
            val board = boardService.createBoard("Orig Title", "Orig Content", "author", testCategory.id, null, 1L)
            val blankTitle = "   "
            val tooLongTitle = "a".repeat(101)
            val blankContent = ""
            val tooLongContent = "b".repeat(5001)

            // When & Then
            assertThrows<IllegalArgumentException>("Blank Title") {
                boardService.updateBoard(board.id, blankTitle, "New Content")
            }
            assertThrows<IllegalArgumentException>("Too Long Title") {
                boardService.updateBoard(board.id, tooLongTitle, "New Content")
            }
            assertThrows<IllegalArgumentException>("Blank Content") {
                boardService.updateBoard(board.id, "New Title", blankContent)
            }
            assertThrows<IllegalArgumentException>("Too Long Content") {
                boardService.updateBoard(board.id, "New Title", tooLongContent)
            }
        }
    }

    @Nested
    @DisplayName("게시글 삭제 (deleteBoard)")
    inner class DeleteBoard {
        @Test
        @DisplayName("성공: 존재하는 게시글을 삭제한다")
        fun `given existing board, when deleteBoard, then board is deleted`() {
            // Given
            val boardToDelete = boardService.createBoard("삭제될 게시글", "내용", "author", testCategory.id, null, 1L)
            val boardId = boardToDelete.id

            // When
            boardService.deleteBoard(boardId)

            // Then
            assertThrows<EntityNotFoundException>("삭제 후 조회 시 실패해야 함") {
                boardService.getBoard(boardId)
            }
            assertFalse(boardRepository.existsById(boardId))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 게시글 ID로 삭제 시 AppException(RESOURCE_NOT_FOUND) 발생")
        fun `given non-existent id, when deleteBoard, then throw ResourceNotFound AppException`() {
            // Given
            val nonExistentId = 999L

            val exception =
                assertThrows<AppException> {
                    boardService.deleteBoard(nonExistentId)
                }
            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
            assertTrue(exception.message.contains("게시글이 존재하지 않습니다.") ?: false)
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    inner class GetBoard {
        @Test
        @DisplayName("ID로 게시글 조회 (getBoard)")
        fun `given existing id, when getBoard, then return board`() {
            // Given
            val savedBoard = boardService.createBoard("제목", "내용", "author", testCategory.id, null, 1L)

            // When
            val foundBoard = boardService.getBoard(savedBoard.id)

            // Then
            assertNotNull(foundBoard)
            assertEquals(savedBoard.id, foundBoard.id)
            assertEquals(savedBoard.title, foundBoard.title)
        }

        @Test
        @DisplayName("ID로 게시글 조회 실패 (getBoard - non-existent)")
        fun `given non-existent id, when getBoard, then throw EntityNotFoundException`() {
            // Given
            val nonExistentId = 999L
            // When & Then
            assertThrows<EntityNotFoundException> {
                boardService.getBoard(nonExistentId)
            }
        }

        @Test
        @DisplayName("작성자로 게시글 목록 조회 (getBoardsByAuthor - Pageable)")
        fun `given author and pageable, when getBoardsByAuthor, then return paginated boards`() {
            // Given
            val author1 = "author1"
            val author2 = "author2"
            repeat(8) { boardService.createBoard("T$it", "C$it", author1, testCategory.id, null, it.toLong()) }
            repeat(3) { boardService.createBoard("T${it + 10}", "C${it + 10}", author2, testCategory.id, null, (it + 10).toLong()) }
            val pageable = PageRequest.of(1, 5, Sort.by("id").descending()) // 2nd page, 5 items, ID desc

            // When
            val boardPage = boardService.getBoardsByAuthor(author1, pageable)

            // Then
            assertEquals(3, boardPage.content.size) // 8 total, 5 on page 0, 3 on page 1
            assertEquals(8, boardPage.totalElements)
            assertEquals(2, boardPage.totalPages)
            assertEquals(1, boardPage.number) // Page index
            assertTrue(boardPage.content.all { it.author == author1 })
        }

        @Test
        @DisplayName("카테고리로 게시글 목록 조회 (getBoardsByCategory - Pageable)")
        fun `given categoryId and pageable, when getBoardsByCategory, then return paginated boards`() {
            // Given
            val category1 = testCategory
            val category2 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT2").create())
            repeat(6) { boardService.createBoard("T$it", "C$it", "A", category1.id, null, it.toLong()) }
            repeat(4) { boardService.createBoard("T${it + 10}", "C${it + 10}", "A", category2.id, null, (it + 10).toLong()) }
            val pageable = PageRequest.of(0, 5)

            // When
            val pageCat1 = boardService.getBoardsByCategory(category1.id, pageable)
            val pageCat2 = boardService.getBoardsByCategory(category2.id, pageable)

            // Then
            assertEquals(5, pageCat1.content.size)
            assertEquals(6, pageCat1.totalElements)
            assertTrue(pageCat1.content.all { it.category.id == category1.id })

            assertEquals(4, pageCat2.content.size)
            assertEquals(4, pageCat2.totalElements)
            assertTrue(pageCat2.content.all { it.category.id == category2.id })
        }

        @Test
        @DisplayName("작성자 및 카테고리로 게시글 목록 조회 (getBoardsByAuthorAndCategory - Pageable)")
        fun `given author, categoryId, and pageable, when getBoardsByAuthorAndCategory, then return filtered paginated boards`() {
            // Given
            val author1 = "A1"
            val author2 = "A2"
            val cat1 = testCategory
            val cat2 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT2").create())

            repeat(3) { boardService.createBoard("A1C1_$it", "C", author1, cat1.id, null, it.toLong()) }
            repeat(2) { boardService.createBoard("A2C1_${it + 5}", "C", author2, cat1.id, null, (it + 5).toLong()) }
            boardService.createBoard("A1C2_10", "C", author1, cat2.id, null, 10L)

            val pageable = PageRequest.of(0, 5)

            // When
            val pageA1C1 = boardService.getBoardsByAuthorAndCategory(author1, cat1.id, pageable)
            val pageA2C1 = boardService.getBoardsByAuthorAndCategory(author2, cat1.id, pageable)
            val pageA1C2 = boardService.getBoardsByAuthorAndCategory(author1, cat2.id, pageable)

            // Then
            assertEquals(3, pageA1C1.content.size)
            assertEquals(3, pageA1C1.totalElements)
            assertTrue(pageA1C1.content.all { it.author == author1 && it.category.id == cat1.id })

            assertEquals(2, pageA2C1.content.size)
            assertTrue(pageA2C1.content.all { it.author == author2 && it.category.id == cat1.id })

            assertEquals(1, pageA1C2.content.size)
            assertTrue(pageA1C2.content.all { it.author == author1 && it.category.id == cat2.id })
        }

        @Test
        @DisplayName("모든 게시글 목록 조회 (getAllBoards - Pageable)")
        fun `given pageable, when getAllBoards, then return all boards paginated`() {
            // Given
            repeat(22) { boardService.createBoard("T$it", "C", "A", testCategory.id, null, it.toLong()) }
            val pageable = PageRequest.of(2, 10)

            // When
            val boardPage = boardService.getAllBoards(pageable)

            // Then
            assertEquals(2, boardPage.content.size)
            assertEquals(22, boardPage.totalElements)
            assertEquals(3, boardPage.totalPages)
            assertEquals(2, boardPage.number)
        }

        @Test
        @DisplayName("모든 게시글 목록과 댓글 정보 조회 (getAllBoardsWithComments - Pageable)")
        fun `given pageable, when getAllBoardsWithComments, then return boards (fake might not load comments)`() {
            val board1 = boardService.createBoard("T1", "C", "A", testCategory.id, null, 1L)

            val pageable = PageRequest.of(0, 10)

            // When
            val boardPage = boardService.getAllBoardsWithComments(pageable)

            // Then
            assertTrue(boardPage.content.isNotEmpty())
            assertEquals(board1.id, boardPage.content[0].id)
        }

        @Test
        @DisplayName("조회수 상위 N개 게시글 조회 (getTopNBoardsByViews)")
        fun `given boards with views, when getTopNBoardsByViews, then return top N ordered by views desc`() {
            // Given
            val board1 = boardService.createBoard("T1", "C", "A", testCategory.id, null, 1L)
            val board2 = boardService.createBoard("T2", "C", "A", testCategory.id, null, 2L)
            val board3 = boardService.createBoard("T3", "C", "A", testCategory.id, null, 3L)
            val board4 = boardService.createBoard("T4", "C", "A", testCategory.id, null, 4L)

            boardRepository.updateViewCount(board1.id, 5)
            boardRepository.updateViewCount(board2.id, 15)
            boardRepository.updateViewCount(board3.id, 10)

            val limit = 3

            // When
            val topBoards = boardService.getTopNBoardsByViews(limit)

            // Then
            assertEquals(limit, topBoards.size)
            assertEquals(board2.id, topBoards[0].id) // 15 views
            assertEquals(board3.id, topBoards[1].id) // 10 views
            assertEquals(board1.id, topBoards[2].id) // 5 views
        }

        @Test
        @DisplayName("조회수 상위 N개 - 조회수 같을 때 ID 내림차순 정렬 (getTopNBoardsByViews)")
        fun `given boards with equal views, when getTopNBoardsByViews, then ordered by id desc`() {
            // Given
            val board1 = boardService.createBoard("T1", "C", "A", testCategory.id, null, 1L) // Lower ID
            val board2 = boardService.createBoard("T2", "C", "A", testCategory.id, null, 2L) // Higher ID
            val board3 = boardService.createBoard("T3", "C", "A", testCategory.id, null, 3L) // Different view count

            boardRepository.updateViewCount(board1.id, 10)
            boardRepository.updateViewCount(board2.id, 10) // Same view count
            boardRepository.updateViewCount(board3.id, 5)

            val limit = 3

            // When
            val topBoards = boardService.getTopNBoardsByViews(limit)

            // Then
            assertEquals(limit, topBoards.size)
            assertEquals(board2.id, topBoards[0].id)
            assertEquals(board1.id, topBoards[1].id)
            assertEquals(board3.id, topBoards[2].id)
        }

        @Test
        @DisplayName("키워드 검색 (searchBoards - Pageable)")
        fun `given keyword and pageable, when searchBoards, then return matching boards paginated`() {
            // Given
            boardService.createBoard("검색 테스트", "내용1", "A", testCategory.id, null, 1L)
            boardService.createBoard("제목2", "테스트 내용 포함", "A", testCategory.id, null, 2L)
            boardService.createBoard("다른 제목", "다른 내용", "A", testCategory.id, null, 3L)
            boardService.createBoard("Test Search", "Content", "A", testCategory.id, null, 4L) // Case-insensitive test

            val keyword = "테스트"
            val pageable = PageRequest.of(0, 5)

            // When
            val searchResult = boardService.searchBoards(keyword, pageable)
            val searchResultCase = boardService.searchBoards("search", pageable)

            // Then
            assertEquals(2, searchResult.content.size)
            assertEquals(2, searchResult.totalElements)
            assertTrue(searchResult.content.any { it.title.contains(keyword) })
            assertTrue(searchResult.content.any { it.content.contains(keyword) })

            // Then Case-insensitive
            assertEquals(1, searchResultCase.content.size)
            assertTrue(searchResultCase.content[0].title.contains("Search", ignoreCase = true))
        }

        @Test
        @DisplayName("ID 목록으로 게시글 조회 (getBoardsByIds)")
        fun `given list of ids, when getBoardsByIds, then return corresponding boards`() {
            // Given
            val board1 = boardService.createBoard("T1", "C", "A", testCategory.id, null, 1L)
            val board2 = boardService.createBoard("T2", "C", "A", testCategory.id, null, 2L)
            val board3 = boardService.createBoard("T3", "C", "A", testCategory.id, null, 3L)
            val nonExistentId = 998L
            val idsToFetch = listOf(board3.id, nonExistentId, board1.id)

            // When
            val boards = boardService.getBoardsByIds(idsToFetch)

            // Then
            assertEquals(2, boards.size)
            assertTrue(boards.any { it.id == board1.id })
            assertTrue(boards.any { it.id == board3.id })
            assertFalse(boards.any { it.id == board2.id })
        }
    }

    @Nested
    @DisplayName("게시글 조회 (Cursor Pagination)")
    inner class GetBoardCursor {
        private fun setupCursorTestData(count: Int): List<Board> =
            (1..count)
                .map {
                    boardService.createBoard("Cursor Title $it", "Content $it", "AuthorCursor", testCategory.id, null, it.toLong())
                }.sortedByDescending { it.id }

        @Test
        @DisplayName("전체 게시글 Cursor 조회 (getBoardsAfter)")
        fun `given limit and lastBoardId, when getBoardsAfter, then return next set of boards`() {
            // Given
            val allBoardsDesc = setupCursorTestData(12) // IDs 12 down to 1
            val limit = 5

            // When - First page
            val page1 = boardService.getBoardsAfter(null, limit)
            // When - Second page
            val page2 = boardService.getBoardsAfter(page1.lastOrNull()?.id, limit)
            // When - Third page
            val page3 = boardService.getBoardsAfter(page2.lastOrNull()?.id, limit)
            // When - Fourth page (empty)
            val page4 = boardService.getBoardsAfter(page3.lastOrNull()?.id, limit)

            // Then
            assertEquals(limit, page1.size)
            assertEquals(allBoardsDesc.take(5).map { it.id }, page1.map { it.id })

            assertEquals(limit, page2.size)
            assertEquals(allBoardsDesc.drop(5).take(5).map { it.id }, page2.map { it.id })

            assertEquals(2, page3.size) // Remaining 2
            assertEquals(allBoardsDesc.drop(10).take(2).map { it.id }, page3.map { it.id })

            assertTrue(page4.isEmpty())
        }

        @Test
        @DisplayName("카테고리별 게시글 Cursor 조회 (getBoardsByCategoryAfter)")
        fun `given category, limit, lastBoardId, when getBoardsByCategoryAfter, then return next set`() {
            // Given
            val cat1 = testCategory
            val cat2 = categoryRepository.save(CategoryFixture.aCategory().withName("CAT2").create())
            val cat1Boards =
                (1..7)
                    .map { boardService.createBoard("C1_$it", "C", "A", cat1.id, null, it.toLong()) }
                    .sortedByDescending { it.id }
            val cat2Boards =
                (8..11)
                    .map { boardService.createBoard("C2_$it", "C", "A", cat2.id, null, it.toLong()) }
                    .sortedByDescending { it.id }

            val limit = 3

            // When - Cat1 pages
            val cat1Page1 = boardService.getBoardsByCategoryAfter(cat1.id, null, limit)
            val cat1Page2 = boardService.getBoardsByCategoryAfter(cat1.id, cat1Page1.lastOrNull()?.id, limit)
            val cat1Page3 = boardService.getBoardsByCategoryAfter(cat1.id, cat1Page2.lastOrNull()?.id, limit)

            // When - Cat2 pages
            val cat2Page1 = boardService.getBoardsByCategoryAfter(cat2.id, null, limit)
            val cat2Page2 = boardService.getBoardsByCategoryAfter(cat2.id, cat2Page1.lastOrNull()?.id, limit)

            // Then - Cat1
            assertEquals(limit, cat1Page1.size)
            assertEquals(limit, cat1Page2.size)
            assertEquals(1, cat1Page3.size)
            assertEquals(cat1Boards.take(3).map { it.id }, cat1Page1.map { it.id })
            assertEquals(cat1Boards.drop(3).take(3).map { it.id }, cat1Page2.map { it.id })
            assertEquals(cat1Boards.drop(6).take(1).map { it.id }, cat1Page3.map { it.id })
            assertTrue(cat1Page1.all { it.category.id == cat1.id })

            // Then - Cat2
            assertEquals(limit, cat2Page1.size)
            assertEquals(1, cat2Page2.size)
            assertEquals(cat2Boards.take(3).map { it.id }, cat2Page1.map { it.id })
            assertEquals(cat2Boards.drop(3).take(1).map { it.id }, cat2Page2.map { it.id })
            assertTrue(cat2Page1.all { it.category.id == cat2.id })
        }

        @Test
        @DisplayName("작성자별 게시글 Cursor 조회 (getBoardsByAuthorAfter)")
        fun `given author, limit, lastBoardId, when getBoardsByAuthorAfter, then return next set`() {
            // Given
            val author1 = "Author1"
            val author2 = "Author2"
            val author1Boards =
                (1..6)
                    .map { boardService.createBoard("A1_$it", "C", author1, testCategory.id, null, it.toLong()) }
                    .sortedByDescending { it.id }
            (7..9).forEach { boardService.createBoard("A2_$it", "C", author2, testCategory.id, null, it.toLong()) }

            val limit = 4

            // When
            val page1 = boardService.getBoardsByAuthorAfter(author1, null, limit)
            val page2 = boardService.getBoardsByAuthorAfter(author1, page1.lastOrNull()?.id, limit)
            val page3 = boardService.getBoardsByAuthorAfter(author1, page2.lastOrNull()?.id, limit)

            // Then
            assertEquals(limit, page1.size)
            assertEquals(2, page2.size)
            assertTrue(page3.isEmpty())
            assertEquals(author1Boards.take(4).map { it.id }, page1.map { it.id })
            assertEquals(author1Boards.drop(4).take(2).map { it.id }, page2.map { it.id })
            assertTrue(page1.all { it.author == author1 })
        }

        @Test
        @DisplayName("키워드 검색 Cursor 조회 (searchBoardsAfter)")
        fun `given keyword, limit, lastBoardId, when searchBoardsAfter, then return next matching set`() {
            // Given
            val keyword = "match"
            val matchingBoards = mutableListOf<Board>()
            val nonMatchingBoards = mutableListOf<Board>()
            (1..15).forEach {
                if (it % 3 == 0) {
                    matchingBoards.add(boardService.createBoard("Title $it", "Content with $keyword", "A", testCategory.id, null, it.toLong()))
                } else {
                    nonMatchingBoards.add(boardService.createBoard("Title $it", "No keyword here", "A", testCategory.id, null, it.toLong()))
                }
            }
            val sortedMatchingDesc = matchingBoards.sortedByDescending { it.id }
            val limit = 2

            // When
            val page1 = boardService.searchBoardsAfter(keyword, null, limit)
            val page2 = boardService.searchBoardsAfter(keyword, page1.lastOrNull()?.id, limit)
            val page3 = boardService.searchBoardsAfter(keyword, page2.lastOrNull()?.id, limit)
            val page4 = boardService.searchBoardsAfter(keyword, page3.lastOrNull()?.id, limit)

            // Then
            assertEquals(limit, page1.size)
            assertEquals(limit, page2.size)
            assertEquals(1, page3.size)
            assertTrue(page4.isEmpty())

            assertEquals(listOf(15L, 12L), page1.map { it.id })
            assertEquals(listOf(9L, 6L), page2.map { it.id })
            assertEquals(listOf(3L), page3.map { it.id })

            assertTrue(page1.all { it.content.contains(keyword) })
            assertTrue(page2.all { it.content.contains(keyword) })
            assertTrue(page3.all { it.content.contains(keyword) })
        }
    }

    @Nested
    @DisplayName("게시글 랭킹")
    inner class BoardRanking {
        private fun setupRankingTestData(): Map<String, Board> {
            val now = LocalDateTime.now()
            val boards = mutableMapOf<String, Board>()

            boards["B_LikeHigh_ViewMid_Recent"] =
                boardService.createBoard("Like High Recent", "C", "A", testCategory.id, null, 1L).also {
                    boardRepository.updateLikeCount(it.id, 20)
                    boardRepository.updateViewCount(it.id, 50)
                }
            boards["B_LikeMid_ViewHigh_Mid"] =
                boardService.createBoard("View High Mid", "C", "A", testCategory.id, null, 2L).also {
                    boardRepository.updateLikeCount(it.id, 10)
                    boardRepository.updateViewCount(it.id, 100)
                }
            boards["B_LikeLow_ViewLow_Old"] =
                boardService.createBoard("Low Old", "C", "A", testCategory.id, null, 3L).also {
                    boardRepository.updateLikeCount(it.id, 5)
                    boardRepository.updateViewCount(it.id, 10)
                }
            boards["B_LikeMid_ViewMid_RecentTie"] =
                boardService.createBoard("Like Mid Recent Tie", "C", "A", testCategory.id, null, 4L).also {
                    boardRepository.updateLikeCount(it.id, 10)
                    boardRepository.updateViewCount(it.id, 50)
                }
            return boards
        }

        @Test
        @DisplayName("랭킹 ID 조회 (getBoardIdsRankedByMetric - Pageable)")
        fun `given metric, period, pageable, when getBoardIdsRankedByMetric, then return ranked board IDs`() {
            // Given
            val boards = setupRankingTestData()
            val period = BoardRankingPeriod.WEEK
            val pageableLikes = PageRequest.of(0, 2, Sort.by("score").descending())
            val pageableViews = PageRequest.of(0, 2, Sort.by("score").descending())

            // When
            val rankedByLikes = boardService.getBoardIdsRankedByMetric(BoardRankingMetric.LIKES, period, pageableLikes)
            val rankedByViews = boardService.getBoardIdsRankedByMetric(BoardRankingMetric.VIEWS, period, pageableViews)

            // Then - Likes
            assertEquals(2, rankedByLikes.content.size)
            assertEquals(boards["B_LikeHigh_ViewMid_Recent"]?.id, rankedByLikes.content[0])
            assertEquals(boards["B_LikeMid_ViewMid_RecentTie"]?.id, rankedByLikes.content[1])

            // Then - Views
            assertEquals(2, rankedByViews.content.size)
            assertEquals(boards["B_LikeMid_ViewHigh_Mid"]?.id, rankedByViews.content[0])
            assertEquals(boards["B_LikeMid_ViewMid_RecentTie"]?.id, rankedByViews.content[1])
        }

        @Test
        @DisplayName("랭킹 ID 조회 Cursor (getBoardIdsRankedByMetricAfter)")
        fun `given metric, period, cursor, when getBoardIdsRankedByMetricAfter, then return next ranked IDs`() {
            // Given
            val boards = setupRankingTestData()
            val period = BoardRankingPeriod.WEEK
            val metric = BoardRankingMetric.LIKES
            val limit = 2

            // When - First page
            val page1Ids = boardService.getBoardIdsRankedByMetricAfter(metric, period, null, limit)

            val lastBoardPage1 = boards.values.find { it.id == page1Ids.lastOrNull() }
            val lastScorePage1 = lastBoardPage1?.likeCount
            val lastIdPage1 = lastBoardPage1?.id

            // When
            val page2Ids = boardService.getBoardIdsRankedByMetricAfter(metric, period, lastIdPage1, limit)

            // Then - Page 1
            assertEquals(limit, page1Ids.size)
            assertEquals(boards["B_LikeHigh_ViewMid_Recent"]?.id, page1Ids[0])
            assertEquals(boards["B_LikeMid_ViewMid_RecentTie"]?.id, page1Ids[1])

            // Then - Page 2
            assertNotNull(lastScorePage1)
            assertNotNull(lastIdPage1)
            assertEquals(limit, page2Ids.size)
            assertEquals(boards["B_LikeMid_ViewHigh_Mid"]?.id, page2Ids[0])
            assertEquals(boards["B_LikeLow_ViewLow_Old"]?.id, page2Ids[1])
        }

        @Test
        @DisplayName("랭킹 정보 조회 (getBoardRankingsForPeriod)")
        fun `given metric, period, limit, when getBoardRankingsForPeriod, then return list of BoardRankingQueryResult`() {
            // Given
            val boards = setupRankingTestData()
            val period = BoardRankingPeriod.WEEK
            val metric = BoardRankingMetric.VIEWS
            val limit = 3L

            // When
            val rankings = boardService.getBoardRankingsForPeriod(metric, period, limit)

            // Then
            assertEquals(limit.toInt(), rankings.size)

            assertEquals(boards["B_LikeMid_ViewHigh_Mid"]?.id, rankings[0].boardId)
            assertEquals(100, rankings[0].score)

            assertEquals(boards["B_LikeMid_ViewMid_RecentTie"]?.id, rankings[1].boardId)
            assertEquals(50, rankings[1].score)

            assertEquals(boards["B_LikeHigh_ViewMid_Recent"]?.id, rankings[2].boardId)
            assertEquals(50, rankings[2].score)
        }

        @Test
        @DisplayName("랭킹 조회 - 기간 필터링 (getBoardRankingsForPeriod)")
        fun `given different periods, when getBoardRankingsForPeriod, then results reflect period`() {
            // Given
            val now = LocalDateTime.now()
            val boardRecent =
                boardRepository
                    .saveWithTimestamp(
                        BoardFixture
                            .aBoard()
                            .withCategory(testCategory)
                            .withAuthor("A")
                            .withTitle("Recent")
                            .create(),
                        now.minusHours(5),
                    ).also { boardRepository.updateLikeCount(it.id, 10) }

            val boardOld =
                boardRepository
                    .saveWithTimestamp(
                        BoardFixture
                            .aBoard()
                            .withCategory(testCategory)
                            .withAuthor("A")
                            .withTitle("Old")
                            .create(),
                        now.minusDays(3),
                    ).also { boardRepository.updateLikeCount(it.id, 20) }

            val limit = 5L
            val metric = BoardRankingMetric.LIKES

            // When
            val rankDay = boardService.getBoardRankingsForPeriod(metric, BoardRankingPeriod.DAY, limit)
            val rankWeek = boardService.getBoardRankingsForPeriod(metric, BoardRankingPeriod.WEEK, limit)

            assertEquals(1, rankDay.size, "DAY 기간은 최근 게시글(5시간 전 생성)만 포함해야 합니다")
            assertEquals(boardRecent.id, rankDay[0].boardId)

            assertEquals(2, rankWeek.size, "WEEK 기간은 두 게시글을 모두 포함해야 합니다")
            assertEquals(boardOld.id, rankWeek[0].boardId, "전체적으로 가장 높은 점수가 WEEK에서 첫 번째여야 합니다 (좋아요 20개)")
            assertEquals(boardRecent.id, rankWeek[1].boardId) // 좋아요 10개
        }
    }

    @Nested
    @DisplayName("게시글 통계")
    inner class BoardStatistics {
        @Test
        @DisplayName("작성자별 게시글 수 계산 (countBoardsByAuthor)")
        fun `given author, when countBoardsByAuthor, then return correct count`() {
            // Given
            val author1 = "user1"
            val author2 = "user2"
            repeat(3) { boardService.createBoard("T$it", "C", author1, testCategory.id, null, it.toLong()) }
            boardService.createBoard("T10", "C", author2, testCategory.id, null, 10L)

            // When
            val count1 = boardService.countBoardsByAuthor(author1)
            val count2 = boardService.countBoardsByAuthor(author2)
            val countNonExistent = boardService.countBoardsByAuthor("nonexistent")

            // Then
            assertEquals(3, count1)
            assertEquals(1, count2)
            assertEquals(0, countNonExistent)
        }

        @Test
        @DisplayName("삭제된 게시글은 통계에서 제외 (countBoardsByAuthor)")
        fun `given deleted boards, when countBoardsByAuthor, then exclude deleted`() {
            // Given
            val author = "user1"
            val board1 = boardService.createBoard("T1", "C", author, testCategory.id, null, 1L)
            val board2 = boardService.createBoard("T2", "C", author, testCategory.id, null, 2L) // To be deleted
            val board3 = boardService.createBoard("T3", "C", author, testCategory.id, null, 3L)

            boardService.deleteBoard(board2.id)

            // When
            val count = boardService.countBoardsByAuthor(author)

            // Then
            assertEquals(2, count) // Excludes board2
        }
    }
}
