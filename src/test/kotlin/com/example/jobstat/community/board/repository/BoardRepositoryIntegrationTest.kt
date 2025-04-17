package com.example.jobstat.community.board.repository

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.fixture.BoardFixture
import com.example.jobstat.community.board.fixture.CategoryFixture
import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.community.comment.fixture.CommentFixture
import com.example.jobstat.community.comment.repository.CommentRepository
import com.example.jobstat.utils.base.JpaIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import java.time.LocalDateTime
import kotlin.math.min

@DisplayName("BoardRepository 통합 테스트")
class BoardRepositoryIntegrationTest : JpaIntegrationTestSupport() {
    @Autowired
    private lateinit var boardRepository: BoardRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    private lateinit var testCategory: BoardCategory

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        testCategory = saveAndFlush(CategoryFixture.aCategory().withName("TEST_CATEGORY").create()) { categoryRepository.save(it) }
    }

    @AfterEach
    override fun tearDown() {
        cleanupTestData()
    }

    override fun cleanupTestData() {
        executeInTransaction {
            val comments = commentRepository.findAll(PageRequest.of(0, Int.MAX_VALUE))
            comments.content.forEach { commentRepository.deleteById(it.id) }

            val boards = boardRepository.findAll(PageRequest.of(0, Int.MAX_VALUE))
            boards.content.forEach { boardRepository.deleteById(it.id) }

            val categories = categoryRepository.findAll() // 모든 카테고리 조회
            categories.forEach { categoryRepository.deleteById(it.id) }
        }
        flushAndClear()
    }

    // 테스트 데이터 생성을 위한 헬퍼 함수
    private fun createAndSaveBoard(
        author: String = "testUser",
        title: String = "Test Title",
        content: String = "Test Content",
        category: BoardCategory = testCategory,
        userId: Long? = 1L, // 회원 글 기본
        password: String? = null, // 비회원 글일 경우 사용
        viewCount: Int = 0,
        likeCount: Int = 0,
    ): Board {
        val board =
            BoardFixture
                .aBoard()
                .withAuthor(author)
                .withTitle(title)
                .withContent(content)
                .withCategory(category)
                .withPassword(password)
                .create()

        val savedBoard = saveAndFlush(board) { boardRepository.save(it) }

        var needsReload = false
        if (viewCount > 0) {
            boardRepository.updateViewCount(savedBoard.id, viewCount)
            needsReload = true
        }
        if (likeCount > 0) {
            boardRepository.updateLikeCount(savedBoard.id, likeCount)
            needsReload = true
        }

        if (needsReload) {
            flushAndClear()
            return boardRepository.findById(savedBoard.id)
        }

        return savedBoard
    }

    // 댓글 생성 헬퍼
    private fun createAndSaveComment(
        board: Board,
        content: String = "Test Comment",
        author: String = "commenter",
        userId: Long? = 2L,
    ): Comment {
        val comment =
            CommentFixture
                .aComment()
                .withBoard(board)
                .withContent(content)
                .withAuthor(author)
                .withUserId(userId)
                .create()

        val savedComment = saveAndFlush(comment) { commentRepository.save(it) }
        return savedComment
    }

    @Nested
    @DisplayName("게시글 생성 및 기본 CRUD 테스트")
    inner class BasicCrudTest {
        @Test
        @DisplayName("새로운 게시글을 생성하고 ID로 조회할 수 있다")
        fun `save and findById success`() {
            // given
            val boardFixture = BoardFixture.aBoard().withCategory(testCategory)
            val boardToSave = boardFixture.create()

            // when
            val savedBoard = saveAndFlush(boardToSave) { boardRepository.save(it) }
            flushAndClear()

            val foundBoard = boardRepository.findById(savedBoard.id)

            // then
            assertThat(foundBoard).isNotNull
            assertThat(foundBoard.id).isEqualTo(savedBoard.id)
            assertThat(foundBoard.title).isEqualTo(boardFixture.create().title)
            assertThat(foundBoard.content).isEqualTo(boardFixture.create().content)
            assertThat(foundBoard.author).isEqualTo(boardFixture.create().author)
            assertThat(foundBoard.category.id).isEqualTo(testCategory.id)
            assertThat(foundBoard.viewCount).isEqualTo(0)
            assertThat(foundBoard.likeCount).isEqualTo(0)
            assertThat(foundBoard.commentCount).isEqualTo(0)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회시 EntityNotFoundException 예외가 발생한다")
        fun `findById with non-existent id should throw EntityNotFoundException`() {
            // given
            val nonExistentId = 999L

            // when & then
            assertThrows<JpaObjectRetrievalFailureException> {
                boardRepository.findById(nonExistentId)
            }
        }

        @Test
        @DisplayName("게시글 내용을 수정할 수 있다 (save 메서드 사용)")
        fun `update content using save method success`() {
            // given
            val savedBoard = createAndSaveBoard()
            val newTitle = "수정된 제목"
            val newContent = "수정된 내용입니다."

            // when
            val boardToUpdate = boardRepository.findById(savedBoard.id)
            boardToUpdate.updateContent(newTitle, newContent)

            saveAndFlush(boardToUpdate) { boardRepository.save(it) }
            flushAndClear()

            val foundBoard = boardRepository.findById(savedBoard.id)

            // then
            assertThat(foundBoard.title).isEqualTo(newTitle)
            assertThat(foundBoard.content).isEqualTo(newContent)
        }

        @Test
        @DisplayName("ID로 게시글을 삭제할 수 있다")
        fun `deleteById success`() {
            // given
            val savedBoard = createAndSaveBoard()

            // when
            boardRepository.deleteById(savedBoard.id)
            flushAndClear()

            // then
            val exists = boardRepository.existsById(savedBoard.id)
            assertThat(exists).isFalse()
            assertThrows<JpaObjectRetrievalFailureException> {
                boardRepository.findById(savedBoard.id)
            }
        }

        @Test
        @DisplayName("여러 ID로 게시글 목록을 조회할 수 있다")
        fun `findAllByIds success`() {
            // given
            val board1 = createAndSaveBoard(title = "Board 1")
            val board2 = createAndSaveBoard(title = "Board 2")
            createAndSaveBoard(title = "Board 3")
            val idsToFind = listOf(board1.id, board2.id)

            // when
            val foundBoards = boardRepository.findAllByIds(idsToFind)

            // then
            assertThat(foundBoards).hasSize(2)
            assertThat(foundBoards).extracting("id").containsExactlyInAnyOrder(board1.id, board2.id)
        }

        @Test
        @DisplayName("존재하지 않는 ID가 포함된 목록으로 조회시 있는 것만 반환한다")
        fun `findAllByIds with non-existent id included`() {
            // given
            val board1 = createAndSaveBoard(title = "Board 1")
            val nonExistentId = 999L
            val idsToFind = listOf(board1.id, nonExistentId)

            // when
            val foundBoards = boardRepository.findAllByIds(idsToFind)

            // then
            assertThat(foundBoards).hasSize(1)
            assertThat(foundBoards[0].id).isEqualTo(board1.id)
        }
    }

    @Nested
    @DisplayName("게시글 조회 테스트 (Offset 기반)")
    inner class FindBoardOffsetTest {
        @Test
        @DisplayName("카테고리별로 게시글을 페이지네이션하여 조회할 수 있다")
        fun `findByCategory with pagination success`() {
            // given
            val category1 = testCategory
            val category2 = saveAndFlush(CategoryFixture.aCategory().withName("CAT2").create()) { categoryRepository.save(it) }
            val c1b1 = createAndSaveBoard(category = category1, title = "C1 B1")
            val c1b2 = createAndSaveBoard(category = category1, title = "C1 B2")
            createAndSaveBoard(category = category2, title = "C2 B1")
            val pageable = PageRequest.of(0, 1, Sort.by("id").ascending())

            // when
            val page1 = boardRepository.findByCategory(category1.id, pageable)
            val page2 = boardRepository.findByCategory(category1.id, pageable.next())

            // then
            assertThat(page1.content).hasSize(1)
            assertThat(page1.totalElements).isEqualTo(2)
            assertThat(page1.totalPages).isEqualTo(2)
            assertThat(page1.content[0].id).isEqualTo(c1b1.id)

            assertThat(page2.content).hasSize(1)
            assertThat(page2.content[0].id).isEqualTo(c1b2.id)
        }

        @Test
        @DisplayName("작성자로 게시글을 페이지네이션하여 조회할 수 있다")
        fun `findByAuthor with pagination success`() {
            // given
            createAndSaveBoard(author = "userA", title = "A1")
            createAndSaveBoard(author = "userA", title = "A2")
            createAndSaveBoard(author = "userB", title = "B1")
            val pageable = PageRequest.of(0, 1) // 첫번째 페이지, 1개씩

            // when
            val resultPage = boardRepository.findByAuthor("userA", pageable)

            // then
            assertThat(resultPage.content).hasSize(1)
            assertThat(resultPage.totalElements).isEqualTo(2) // userA의 게시글은 총 2개
            assertThat(resultPage.content[0].author).isEqualTo("userA")
        }

        @Test
        @DisplayName("작성자와 카테고리로 게시글을 페이지네이션하여 조회할 수 있다")
        fun `findByAuthorAndCategory with pagination success`() {
            // given
            val category1 = testCategory
            val category2 = saveAndFlush(CategoryFixture.aCategory().withName("CAT2").create()) { categoryRepository.save(it) }
            val targetBoard = createAndSaveBoard(author = "userA", category = category1, title = "A-C1-1")
            createAndSaveBoard(author = "userA", category = category2, title = "A-C2-1")
            createAndSaveBoard(author = "userB", category = category1, title = "B-C1-1")
            val pageable = PageRequest.of(0, 10)

            // when
            val resultPage = boardRepository.findByAuthorAndCategory("userA", category1.id, pageable)

            // then
            assertThat(resultPage.content).hasSize(1)
            assertThat(resultPage.totalElements).isEqualTo(1)
            assertThat(resultPage.content[0].id).isEqualTo(targetBoard.id)
        }

        @Test
        @DisplayName("조회수 순으로 상위 N개 게시글을 조회할 수 있다")
        fun `findTopNByOrderByViewCountDesc success`() {
            // given
            createAndSaveBoard(title = "View 1", viewCount = 1)
            val board2 = createAndSaveBoard(title = "View 3", viewCount = 3)
            val board3 = createAndSaveBoard(title = "View 2", viewCount = 2)
            val limit = 2

            // when
            val topBoards = boardRepository.findTopNByOrderByViewCountDesc(limit)

            // then
            assertThat(topBoards).hasSize(limit)
            // 조회수 내림차순 정렬 확인
            assertThat(topBoards[0].id).isEqualTo(board2.id) // View 3
            assertThat(topBoards[1].id).isEqualTo(board3.id) // View 2
            assertThat(topBoards).extracting("viewCount").containsExactly(3, 2)
        }

        @Test
        @DisplayName("키워드로 제목과 내용을 검색하고 페이지네이션 할 수 있다")
        fun `search with pagination success`() {
            // given
            val b1 = createAndSaveBoard(title = "검색 테스트 1", content = "내용입니다.")
            val b2 = createAndSaveBoard(title = "다른 제목", content = "검색 키워드 포함")
            createAndSaveBoard(title = "관련 없는 글", content = "테스트 내용")
            val keyword = "검색"
            // ID 오름차순으로 1개씩 조회
            val pageable = PageRequest.of(0, 1, Sort.by("id").ascending())

            // when
            val page1 = boardRepository.search(keyword, pageable)
            val page2 = boardRepository.search(keyword, pageable.next())

            // then
            assertThat(page1.totalElements).isEqualTo(2) // "검색" 포함 게시글 총 2개
            assertThat(page1.content).hasSize(1)
            assertThat(page1.content[0].id).isEqualTo(b1.id)

            assertThat(page2.content).hasSize(1)
            assertThat(page2.content[0].id).isEqualTo(b2.id)
        }

        @Test
        @DisplayName("검색 시 대소문자를 구분하지 않는다")
        fun `search case-insensitive success`() {
            // given
            val targetBoard = createAndSaveBoard(title = "Case Test", content = "Content")
            val pageable = PageRequest.of(0, 10)

            // when
            val lowerResult = boardRepository.search("case test", pageable)
            val upperResult = boardRepository.search("CASE TEST", pageable)
            val mixedResult = boardRepository.search("CaSe TeSt", pageable)

            // then
            assertThat(lowerResult.content)
                .hasSize(1)
                .first()
                .extracting("id")
                .isEqualTo(targetBoard.id)
            assertThat(upperResult.content)
                .hasSize(1)
                .first()
                .extracting("id")
                .isEqualTo(targetBoard.id)
            assertThat(mixedResult.content)
                .hasSize(1)
                .first()
                .extracting("id")
                .isEqualTo(targetBoard.id)
        }
    }

    @Nested
    @DisplayName("게시글 수정/삭제 테스트 (Repository 메서드 중심)")
    inner class UpdateDeleteByRepoMethodTest {
        @Test
        @DisplayName("updateViewCount: 게시글 조회수를 증가시킨다")
        fun `updateViewCount success`() {
            // given
            val board = createAndSaveBoard(viewCount = 5)
            val increment = 3

            // when
            boardRepository.updateViewCount(board.id, increment)
            flushAndClear() // DB 직접 업데이트 반영 및 영속성 컨텍스트 클리어

            // then
            // findViewCountById로 특정 컬럼만 조회하여 확인
            val updatedViewCount = boardRepository.findViewCountById(board.id)
            assertThat(updatedViewCount).isEqualTo(5 + increment)

            // 엔티티를 다시 로드해서 확인
            val reloadedBoard = boardRepository.findById(board.id)
            assertThat(reloadedBoard.viewCount).isEqualTo(5 + increment)
        }

        @Test
        @DisplayName("updateLikeCount: 게시글 좋아요 수를 증가시킨다")
        fun `updateLikeCount success`() {
            // given
            val board = createAndSaveBoard(likeCount = 10)
            val increment = 5

            // when
            boardRepository.updateLikeCount(board.id, increment)
            flushAndClear()

            // then
            val updatedLikeCount = boardRepository.findLikeCountById(board.id)
            assertThat(updatedLikeCount).isEqualTo(10 + increment)

            val reloadedBoard = boardRepository.findById(board.id)
            assertThat(reloadedBoard.likeCount).isEqualTo(10 + increment)
        }

        @Test
        @DisplayName("게시글 삭제시 연관된 댓글도 함께 삭제된다 (Cascade 설정 확인)")
        fun `deleteBoard with comments cascade success`() {
            // given
            val savedBoard = createAndSaveBoard()
            val comment = createAndSaveComment(board = savedBoard)
            val commentId = comment.id
            flushAndClear()

            // 댓글이 정상적으로 저장되었는지 확인
            assertThat(commentRepository.existsById(commentId)).isTrue()

            // when
            boardRepository.deleteById(savedBoard.id) // 게시글 삭제
            flushAndClear()

            // then
            // 게시글 삭제 확인
            assertThat(boardRepository.existsById(savedBoard.id)).isFalse()
            // CascadeType.ALL, orphanRemoval=true 설정에 의해 댓글도 삭제되었는지 확인
            assertThat(commentRepository.existsById(commentId)).isFalse()
        }
    }

    @Nested
    @DisplayName("게시글 통계 및 존재 여부 테스트")
    inner class BoardStatisticsTest {
        @Test
        @DisplayName("작성자별 게시글 수를 계산할 수 있다")
        fun `countByAuthor success`() {
            // given
            createAndSaveBoard(author = "userA")
            createAndSaveBoard(author = "userA")
            createAndSaveBoard(author = "userB")

            // when
            val countA = boardRepository.countByAuthor("userA")
            val countB = boardRepository.countByAuthor("userB")
            val countC = boardRepository.countByAuthor("userC") // 없는 사용자

            // then
            assertThat(countA).isEqualTo(2)
            assertThat(countB).isEqualTo(1)
            assertThat(countC).isEqualTo(0)
        }

        @Test
        @DisplayName("작성자와 제목으로 게시글 존재 여부를 확인할 수 있다")
        fun `existsByAuthorAndTitle success`() {
            // given
            val author = "checker"
            val title = "Unique Title"
            createAndSaveBoard(author = author, title = title)

            // when
            val exists1 = boardRepository.existsByAuthorAndTitle(author, title)
            val exists2 = boardRepository.existsByAuthorAndTitle(author, "Different Title")
            val exists3 = boardRepository.existsByAuthorAndTitle("anotherUser", title)

            // then
            assertThat(exists1).isTrue()
            assertThat(exists2).isFalse()
            assertThat(exists3).isFalse()
        }
    }

    @Nested
    @DisplayName("게시글 상세 조회 테스트 (Fetch Join)")
    inner class BoardDetailQueryTest {
        @Test
        @DisplayName("findAllWithDetails: 댓글과 카테고리를 Fetch Join 하여 조회한다")
        fun `findAllWithDetails fetch join success`() {
            // given
            val board1 = createAndSaveBoard(title = "Board 1")
            val board2 = createAndSaveBoard(title = "Board 2")
            createAndSaveComment(board = board1, content = "B1 C1")
            createAndSaveComment(board = board1, content = "B1 C2")
            createAndSaveComment(board = board2, content = "B2 C1")
            flushAndClear()

            // when
            val boardsWithDetailsPage =
                executeInTransaction {
                    // 동일 트랜잭션 내에서 Fetch 확인
                    boardRepository.findAllWithDetails(PageRequest.of(0, 10))
                }
            val boardsWithDetails = boardsWithDetailsPage.content

            // then
            assertThat(boardsWithDetails).hasSize(2)
            boardsWithDetails.forEach { board ->

                if (board.id == board1.id) {
                    assertThat(board.comments).hasSize(2)
                } else if (board.id == board2.id) {
                    assertThat(board.comments).hasSize(1)
                }
            }
        }

        @Nested
        @DisplayName("커서 기반 페이징 테스트")
        inner class CursorPaginationTest {
            private lateinit var boardsSortedByIdDesc: List<Board>

            @BeforeEach
            fun setupCursorData() {
                // 테스트 데이터 생성 (ID는 자동 증가 가정)
                val b1 = createAndSaveBoard(title = "Board 1")
                val b2 = createAndSaveBoard(title = "Board 2")
                val b3 = createAndSaveBoard(title = "Board 3")
                val b4 = createAndSaveBoard(title = "Board 4")
                val b5 = createAndSaveBoard(title = "Board 5")
                // ID 내림차순으로 정렬된 리스트 준비
                boardsSortedByIdDesc = listOf(b1, b2, b3, b4, b5).sortedByDescending { it.id }
            }

            @Test
            @DisplayName("findBoardsAfter: ID 내림차순으로 커서 기반 페이징 조회")
            fun `findBoardsAfter success`() {
                val limit = 2
                val allIds = boardsSortedByIdDesc.map { it.id } // 예상 ID 순서 (내림차순)

                // 첫 페이지 조회 (lastBoardId = null)
                val page1 = boardRepository.findBoardsAfter(null, limit)
                assertThat(page1).hasSize(limit)
                assertThat(page1.map { it.id }).containsExactlyElementsOf(allIds.subList(0, limit))

                // 두 번째 페이지 조회 (lastBoardId = page1의 마지막 ID)
                val lastIdPage1 = page1.last().id
                val page2 = boardRepository.findBoardsAfter(lastIdPage1, limit)
                assertThat(page2).hasSize(limit)
                assertThat(page2.map { it.id }).containsExactlyElementsOf(allIds.subList(limit, limit * 2))

                // 세 번째 페이지 조회
                val lastIdPage2 = page2.last().id
                val page3 = boardRepository.findBoardsAfter(lastIdPage2, limit)
                assertThat(page3).hasSize(allIds.size % limit) // 나머지 개수
                assertThat(page3.map { it.id }).containsExactlyElementsOf(allIds.subList(limit * 2, allIds.size))

                // 마지막 페이지 다음 조회
                val lastIdPage3 = page3.last().id
                val page4 = boardRepository.findBoardsAfter(lastIdPage3, limit)
                assertThat(page4).isEmpty()
            }

            @Test
            @DisplayName("findBoardsByCategoryAfter: 카테고리별 커서 기반 페이징 조회")
            fun `findBoardsByCategoryAfter success`() {
                // given
                val category1 = testCategory
                val category2 = saveAndFlush(CategoryFixture.aCategory().withName("CAT2").create()) { categoryRepository.save(it) }
                // 카테고리별로 추가 데이터 생성
                val c1bNew1 = createAndSaveBoard(category = category1, title = "C1 New 1")
                val c2bNew1 = createAndSaveBoard(category = category2, title = "C2 New 1")
                val c1bNew2 = createAndSaveBoard(category = category1, title = "C1 New 2")

                // 카테고리 1에 속하는 모든 게시글 ID (내림차순)
                val category1AllBoards =
                    (boardsSortedByIdDesc.filter { it.category.id == category1.id } + listOf(c1bNew1, c1bNew2))
                        .sortedByDescending { it.id }
                val category1AllIds = category1AllBoards.map { it.id }
                val limit = 2

                // when: Category 1 조회 - Page 1
                val c1page1 = boardRepository.findBoardsByCategoryAfter(category1.id, null, limit)
                assertThat(c1page1).hasSize(limit)
                assertThat(c1page1.map { it.id }).containsExactlyElementsOf(category1AllIds.subList(0, limit))

                // when: Category 1 조회 - Page 2
                val c1lastId = c1page1.last().id
                val c1page2 = boardRepository.findBoardsByCategoryAfter(category1.id, c1lastId, limit)
                assertThat(c1page2).hasSize(limit) // 개수가 limit보다 작거나 같을 수 있음
                assertThat(c1page2.map { it.id }).containsExactlyElementsOf(category1AllIds.subList(limit, limit * 2))

                // when: Category 2 조회 (게시글 1개만 존재)
                val c2page1 = boardRepository.findBoardsByCategoryAfter(category2.id, null, limit)
                assertThat(c2page1).hasSize(1)
                assertThat(c2page1[0].id).isEqualTo(c2bNew1.id)
            }

            @Test
            @DisplayName("findBoardsByAuthorAfter: 작성자별 커서 기반 페이징 조회")
            fun `findBoardsByAuthorAfter success`() {
                // given
                val authorA = "AuthorA"
                val authorB = "AuthorB"
                // 기존 데이터 외 추가
                val a1 = createAndSaveBoard(author = authorA, title = "A1")
                val b1 = createAndSaveBoard(author = authorB, title = "B1")
                val a2 = createAndSaveBoard(author = authorA, title = "A2")

                val authorAAllBoards =
                    (boardsSortedByIdDesc.filter { it.author == authorA } + listOf(a1, a2))
                        .sortedByDescending { it.id }
                val authorAAllIds = authorAAllBoards.map { it.id }
                val limit = 1

                // when: Author A 조회 - Page 1
                val aPage1 = boardRepository.findBoardsByAuthorAfter(authorA, null, limit)
                assertThat(aPage1).hasSize(limit)
                assertThat(aPage1.map { it.id }).containsExactlyElementsOf(authorAAllIds.subList(0, limit))

                // when: Author A 조회 - Page 2
                val aLastId1 = aPage1.last().id
                val aPage2 = boardRepository.findBoardsByAuthorAfter(authorA, aLastId1, limit)
                assertThat(aPage2).hasSize(limit)
                assertThat(aPage2.map { it.id }).containsExactlyElementsOf(authorAAllIds.subList(limit, limit * 2))

                // when: Author A 조회 - Page 3
                val aLastId2 = aPage2.last().id
                val aPage3 = boardRepository.findBoardsByAuthorAfter(authorA, aLastId2, limit)
                assertThat(aPage3).isEmpty() // 작성자 A의 글은 2개 뿐

                // when: Author B 조회
                val bPage1 = boardRepository.findBoardsByAuthorAfter(authorB, null, limit)
                assertThat(bPage1).hasSize(1)
                assertThat(bPage1[0].id).isEqualTo(b1.id)
            }

            @Test
            @DisplayName("searchBoardsAfter: 검색 결과 커서 기반 페이징 조회 (기대값 수정)")
            fun `searchBoardsAfter success corrected expectation`() {
                // given
                // setupCursorData로 초기 데이터 생성 (Board 1 ~ 5 가정)
                val initialBoards =
                    boardRepository
                        .findAll(
                            PageRequest.of(0, 5, Sort.by("id").descending()),
                        ).content // ID 내림차순으로 정렬된 게시글 리스트
                val board6 = createAndSaveBoard(title = "Another Board") // 이 게시물도 "Board" 키워드를 포함한다고 가정 (로그 기반)
                println("==> 데이터 준비: 초기 ${initialBoards.size}개 + Another Board(ID: ${board6.id}) 생성 완료")

                // 모든 관련 게시물을 가져와 ID 내림차순으로 정렬하여 기대값 생성
                val allBoardIds =
                    (initialBoards + board6)
                        .map { it.id }
                        .sortedDescending()

                val keyword = "Board"
                val limit = 2
                val searchResultIds = allBoardIds

                println("==> 예상되는 전체 검색 결과 ID (내림차순): $searchResultIds")
                println("==> 검색 키워드: '$keyword', 페이지 크기: $limit")
                assertThat(searchResultIds).hasSize(initialBoards.size + 1)

                // when: Page 1
                val lastId0: Long? = null
                println("\n==> Page 1 요청: lastBoardId = $lastId0, limit = $limit")
                val page1 = boardRepository.searchBoardsAfter(keyword, lastId0, limit)
                val page1Ids = page1.map { it.id }
                println("==> Page 1 실제 결과 ID: $page1Ids")
                val expectedPage1Ids = searchResultIds.subList(0, min(limit, searchResultIds.size))
                println("==> Page 1 예상 결과 ID: $expectedPage1Ids")

                // then: Page 1
                assertThat(page1).hasSize(expectedPage1Ids.size)
                assertThat(page1Ids).containsExactlyElementsOf(expectedPage1Ids)

                // when: Page 2
                if (page1.isNotEmpty() && searchResultIds.size > limit) {
                    val lastId1 = page1.last().id
                    println("\n==> Page 2 요청: lastBoardId = $lastId1, limit = $limit")
                    val page2 = boardRepository.searchBoardsAfter(keyword, lastId1, limit)
                    val page2Ids = page2.map { it.id }
                    println("==> Page 2 실제 결과 ID: $page2Ids")

                    val expectedPage2StartIndex = limit
                    val expectedPage2EndIndex = min(limit * 2, searchResultIds.size)
                    val expectedPage2Ids = searchResultIds.subList(expectedPage2StartIndex, expectedPage2EndIndex)
                    println("==> Page 2 예상 결과 ID: $expectedPage2Ids")

                    // then: Page 2
                    assertThat(page2).hasSize(expectedPage2Ids.size)
                    assertThat(page2Ids).containsExactlyElementsOf(expectedPage2Ids)

                    // when: Page 3
                    if (page2.isNotEmpty() && searchResultIds.size > limit * 2) {
                        val lastId2 = page2.last().id
                        println("\n==> Page 3 요청: lastBoardId = $lastId2, limit = $limit")
                        val page3 = boardRepository.searchBoardsAfter(keyword, lastId2, limit)
                        val page3Ids = page3.map { it.id }
                        println("==> Page 3 실제 결과 ID: $page3Ids")

                        val expectedPage3StartIndex = limit * 2
                        val expectedPage3EndIndex = min(limit * 3, searchResultIds.size)
                        val expectedPage3Ids = searchResultIds.subList(expectedPage3StartIndex, expectedPage3EndIndex)
                        println("==> Page 3 예상 결과 ID: $expectedPage3Ids")

                        // then: Page 3
                        assertThat(page3).hasSize(expectedPage3Ids.size)
                        assertThat(page3Ids).containsExactlyElementsOf(expectedPage3Ids)
                    } else {
                        println("\n==> Page 2 결과가 비어있거나 더 이상 데이터가 없어 Page 3 조회 스킵 또는 결과 없음 확인")
                        if (searchResultIds.size <= limit * 2) {
                            val lastId2 = if (page2.isNotEmpty()) page2.last().id else null
                            if (lastId2 != null) {
                                val page3 = boardRepository.searchBoardsAfter(keyword, lastId2, limit)
                                assertThat(page3).isEmpty()
                                println("==> Page 3 예상대로 비어 있음.")
                            }
                        }
                    }
                } else {
                    println("\n==> Page 1 결과가 비어있거나 더 이상 데이터가 없어 이후 페이지 조회 스킵")
                    if (searchResultIds.size <= limit) {
                        val lastId1 = if (page1.isNotEmpty()) page1.last().id else null
                        if (lastId1 != null) {
                            val page2 = boardRepository.searchBoardsAfter(keyword, lastId1, limit)
                            assertThat(page2).isEmpty()
                            println("==> Page 2 예상대로 비어 있음.")
                        }
                    }
                }
            }
        }

        @Nested
        @DisplayName("랭킹 쿼리 테스트")
        inner class RankingQueryTest {
            private val now: LocalDateTime = LocalDateTime.now()
            private lateinit var rankedBoards: Map<String, Board>
            private lateinit var expectedLikesOrderIds: List<Long>
            private lateinit var expectedViewsOrderIds: List<Long>

            @BeforeEach
            fun setupRankingData() {
                rankedBoards =
                    mapOf(
                        "R5" to createAndSaveBoard(title = "Rank 5", likeCount = 1, viewCount = 5),
                        "R3" to createAndSaveBoard(title = "Rank 3", likeCount = 5, viewCount = 1),
                        "R4" to createAndSaveBoard(title = "Rank 4", likeCount = 3, viewCount = 3),
                        "R1" to createAndSaveBoard(title = "Rank 1", likeCount = 4, viewCount = 2),
                        "R2" to createAndSaveBoard(title = "Rank 2", likeCount = 2, viewCount = 4),
                    )
                flushAndClear()

                // likeCount DESC, id DESC 순서로 기대값 ID 리스트 생성
                expectedLikesOrderIds =
                    rankedBoards.values
                        .sortedWith(
                            compareByDescending<Board> { it.likeCount }.thenByDescending { it.id },
                        ).map { it.id }

                // viewCount DESC, id DESC 순서로 기대값 ID 리스트 생성
                expectedViewsOrderIds =
                    rankedBoards.values
                        .sortedWith(
                            compareByDescending<Board> { it.viewCount }.thenByDescending { it.id },
                        ).map { it.id }

                println("==> 좋아요 랭킹 순서 (ID): $expectedLikesOrderIds")
                println("==> 조회수 랭킹 순서 (ID): $expectedViewsOrderIds")
            }

            // getBoardId Helper (기존 유지)
            private fun getBoardId(key: String): Long = rankedBoards[key]?.id ?: throw IllegalStateException("Board '$key' not found in rankedBoards map")

            // getBoard Helper (스코어 조회용)
            private fun getBoard(id: Long): Board = boardRepository.findById(id) // 테스트 내에서 필요시 조회

            @Test
            @DisplayName("findBoardIdsRankedByLikes: 좋아요 순으로 ID 랭킹 조회 (Offset)")
            fun `findBoardIdsRankedByLikes offset success`() {
                // given
                val startTime = now.minusDays(1)
                val endTime = now.plusDays(1)
                val pageable = PageRequest.of(0, 3) // 상위 3개

                // when
                val rankedIdsPage = boardRepository.findBoardIdsRankedByLikes(startTime, endTime, pageable)

                // then
                assertThat(rankedIdsPage.content).hasSize(3)
                assertThat(rankedIdsPage.totalElements).isEqualTo(5)
                // 생성된 expectedLikesOrderIds 리스트의 앞 3개와 비교
                assertThat(rankedIdsPage.content).containsExactlyElementsOf(expectedLikesOrderIds.subList(0, 3))
            }

            @Test
            @DisplayName("findBoardIdsRankedByViews: 조회수 순으로 ID 랭킹 조회 (Offset)")
            fun `findBoardIdsRankedByViews offset success`() {
                // given
                val startTime = now.minusDays(1)
                val endTime = now.plusDays(1)
                val pageable = PageRequest.of(0, 3) // 상위 3개

                // when
                val rankedIdsPage = boardRepository.findBoardIdsRankedByViews(startTime, endTime, pageable)

                // then
                assertThat(rankedIdsPage.content).hasSize(3)
                assertThat(rankedIdsPage.totalElements).isEqualTo(5)
                assertThat(rankedIdsPage.content).containsExactlyElementsOf(expectedViewsOrderIds.subList(0, 3))
            }

            @Test
            @DisplayName("findBoardIdsRankedByLikesAfter: 좋아요 순으로 ID 랭킹 조회 (Cursor - Keyset)")
            fun `findBoardIdsRankedByLikesAfter cursor keyset success`() {
                // given
                val startTime = now.minusDays(1)
                val endTime = now.plusDays(1)
                val limit = 2

                // when: Page 1
                println("\n==> Likes Cursor Page 1 요청: lastScore=null, lastId=null, limit=$limit")
                val page1Ids = boardRepository.findBoardIdsRankedByLikesAfter(startTime, endTime, null, null, limit)
                println("==> Likes Cursor Page 1 결과: $page1Ids")
                assertThat(page1Ids).hasSize(limit)
                assertThat(page1Ids).containsExactlyElementsOf(expectedLikesOrderIds.subList(0, limit))

                // when: Page 2
                if (page1Ids.isNotEmpty() && expectedLikesOrderIds.size > limit) {
                    val lastIdPage1 = page1Ids.last()
                    val lastBoardPage1 = getBoard(lastIdPage1)
                    val lastScorePage1 = lastBoardPage1.likeCount

                    println("\n==> Likes Cursor Page 2 요청: lastScore=$lastScorePage1, lastId=$lastIdPage1, limit=$limit")
                    val page2Ids = boardRepository.findBoardIdsRankedByLikesAfter(startTime, endTime, lastScorePage1, lastIdPage1, limit)
                    val expectedPage2Ids = expectedLikesOrderIds.subList(limit, min(limit * 2, expectedLikesOrderIds.size))
                    println("==> Likes Cursor Page 2 결과: $page2Ids")
                    println("==> Likes Cursor Page 2 예상: $expectedPage2Ids")

                    assertThat(page2Ids).hasSize(expectedPage2Ids.size)
                    assertThat(page2Ids).containsExactlyElementsOf(expectedPage2Ids)

                    // when: Page 3
                    if (page2Ids.isNotEmpty() && expectedLikesOrderIds.size > limit * 2) {
                        val lastIdPage2 = page2Ids.last()
                        val lastBoardPage2 = getBoard(lastIdPage2)
                        val lastScorePage2 = lastBoardPage2.likeCount

                        println("\n==> Likes Cursor Page 3 요청: lastScore=$lastScorePage2, lastId=$lastIdPage2, limit=$limit")
                        val page3Ids = boardRepository.findBoardIdsRankedByLikesAfter(startTime, endTime, lastScorePage2, lastIdPage2, limit)
                        val expectedPage3Ids = expectedLikesOrderIds.subList(limit * 2, min(limit * 3, expectedLikesOrderIds.size))
                        println("==> Likes Cursor Page 3 결과: $page3Ids")
                        println("==> Likes Cursor Page 3 예상: $expectedPage3Ids")

                        assertThat(page3Ids).hasSize(expectedPage3Ids.size)
                        assertThat(page3Ids).containsExactlyElementsOf(expectedPage3Ids)

                        // when: Page 4 (마지막 다음)
                        if (page3Ids.isNotEmpty()) {
                            val lastIdPage3 = page3Ids.last()
                            val lastBoardPage3 = getBoard(lastIdPage3)
                            val lastScorePage3 = lastBoardPage3.likeCount
                            println("\n==> Likes Cursor Page 4 요청: lastScore=$lastScorePage3, lastId=$lastIdPage3, limit=$limit")
                            val page4Ids = boardRepository.findBoardIdsRankedByLikesAfter(startTime, endTime, lastScorePage3, lastIdPage3, limit)
                            println("==> Likes Cursor Page 4 결과: $page4Ids")
                            assertThat(page4Ids).isEmpty()
                        }
                    }
                }
            }

            @Test
            @DisplayName("findBoardIdsRankedByViewsAfter: 조회수 순으로 ID 랭킹 조회 (Cursor - Keyset)")
            fun `findBoardIdsRankedByViewsAfter cursor keyset success`() {
                // given
                val startTime = now.minusDays(1)
                val endTime = now.plusDays(1)
                val limit = 2

                // when: Page 1
                println("\n==> Views Cursor Page 1 요청: lastScore=null, lastId=null, limit=$limit")
                val page1Ids = boardRepository.findBoardIdsRankedByViewsAfter(startTime, endTime, null, null, limit)
                println("==> Views Cursor Page 1 결과: $page1Ids")
                assertThat(page1Ids).hasSize(limit)
                assertThat(page1Ids).containsExactlyElementsOf(expectedViewsOrderIds.subList(0, limit))

                // when: Page 2
                if (page1Ids.isNotEmpty() && expectedViewsOrderIds.size > limit) {
                    val lastIdPage1 = page1Ids.last()
                    val lastBoardPage1 = getBoard(lastIdPage1)
                    val lastScorePage1 = lastBoardPage1.viewCount

                    println("\n==> Views Cursor Page 2 요청: lastScore=$lastScorePage1, lastId=$lastIdPage1, limit=$limit")
                    val page2Ids = boardRepository.findBoardIdsRankedByViewsAfter(startTime, endTime, lastScorePage1, lastIdPage1, limit)
                    val expectedPage2Ids = expectedViewsOrderIds.subList(limit, min(limit * 2, expectedViewsOrderIds.size))
                    println("==> Views Cursor Page 2 결과: $page2Ids")
                    println("==> Views Cursor Page 2 예상: $expectedPage2Ids")

                    assertThat(page2Ids).hasSize(expectedPage2Ids.size)
                    assertThat(page2Ids).containsExactlyElementsOf(expectedPage2Ids)

                    // when: Page 3
                    if (page2Ids.isNotEmpty() && expectedViewsOrderIds.size > limit * 2) {
                        val lastIdPage2 = page2Ids.last()
                        val lastBoardPage2 = getBoard(lastIdPage2)
                        val lastScorePage2 = lastBoardPage2.viewCount

                        println("\n==> Views Cursor Page 3 요청: lastScore=$lastScorePage2, lastId=$lastIdPage2, limit=$limit")
                        val page3Ids = boardRepository.findBoardIdsRankedByViewsAfter(startTime, endTime, lastScorePage2, lastIdPage2, limit)
                        val expectedPage3Ids = expectedViewsOrderIds.subList(limit * 2, min(limit * 3, expectedViewsOrderIds.size))
                        println("==> Views Cursor Page 3 결과: $page3Ids")
                        println("==> Views Cursor Page 3 예상: $expectedPage3Ids")

                        assertThat(page3Ids).hasSize(expectedPage3Ids.size)
                        assertThat(page3Ids).containsExactlyElementsOf(expectedPage3Ids)

                        // when: Page 4 (마지막 다음)
                        if (page3Ids.isNotEmpty()) {
                            val lastIdPage3 = page3Ids.last()
                            val lastBoardPage3 = getBoard(lastIdPage3)
                            val lastScorePage3 = lastBoardPage3.viewCount
                            println("\n==> Views Cursor Page 4 요청: lastScore=$lastScorePage3, lastId=$lastIdPage3, limit=$limit")
                            val page4Ids = boardRepository.findBoardIdsRankedByViewsAfter(startTime, endTime, lastScorePage3, lastIdPage3, limit)
                            println("==> Views Cursor Page 4 결과: $page4Ids")
                            assertThat(page4Ids).isEmpty()
                        }
                    }
                }
            }

            @Test
            @DisplayName("findBoardRankingsByLikes: 좋아요 순으로 랭킹 결과(ID, 점수) 조회 (Offset)")
            fun `findBoardRankingsByLikes offset success`() {
                // given
                val startTime = now.minusDays(1)
                val endTime = now.plusDays(1)
                val pageable = PageRequest.of(0, 3)

                // when
                val rankingsPage = boardRepository.findBoardRankingsByLikes(startTime, endTime, pageable)

                // then
                assertThat(rankingsPage.content).hasSize(3)
                assertThat(rankingsPage.totalElements).isEqualTo(5)

                val results = rankingsPage.content
                val expectedIds = expectedLikesOrderIds.subList(0, 3)
                assertThat(results.map { it.boardId }).containsExactlyElementsOf(expectedIds)
                assertThat(results[0].score).isEqualTo(getBoard(expectedIds[0]).likeCount) // 5
                assertThat(results[1].score).isEqualTo(getBoard(expectedIds[1]).likeCount) // 4
                assertThat(results[2].score).isEqualTo(getBoard(expectedIds[2]).likeCount) // 3
            }

            @Test
            @DisplayName("findBoardRankingsByViews: 조회수 순으로 랭킹 결과(ID, 점수) 조회 (Offset)")
            fun `findBoardRankingsByViews offset success`() {
                // given
                val startTime = now.minusDays(1)
                val endTime = now.plusDays(1)
                val pageable = PageRequest.of(0, 3)

                // when
                val rankingsPage = boardRepository.findBoardRankingsByViews(startTime, endTime, pageable)

                // then
                assertThat(rankingsPage.content).hasSize(3)
                assertThat(rankingsPage.totalElements).isEqualTo(5)

                val results = rankingsPage.content
                val expectedIds = expectedViewsOrderIds.subList(0, 3)
                assertThat(results.map { it.boardId }).containsExactlyElementsOf(expectedIds)
                assertThat(results[0].score).isEqualTo(getBoard(expectedIds[0]).viewCount) // 5
                assertThat(results[1].score).isEqualTo(getBoard(expectedIds[1]).viewCount) // 4
                assertThat(results[2].score).isEqualTo(getBoard(expectedIds[2]).viewCount) // 3
            }
        }
    }
}
