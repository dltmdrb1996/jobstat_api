package com.wildrew.app.community.comment.repository

import com.wildrew.jobstat.community.board.entity.Board
import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.board.fixture.BoardFixture
import com.wildrew.jobstat.community.board.fixture.CategoryFixture
import com.wildrew.jobstat.community.board.repository.BoardRepository
import com.wildrew.jobstat.community.board.repository.CategoryRepository
import com.wildrew.jobstat.community.comment.entity.Comment
import com.wildrew.jobstat.community.comment.fixture.CommentFixture
import com.wildrew.jobstat.community.comment.utils.CommentConstants
import com.wildrew.jobstat.utils.base.JpaIntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException

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

    // 테스트 데이터 정리
    override fun cleanupTestData() {
        executeInTransaction {
            commentRepository.findAll(PageRequest.of(0, Int.MAX_VALUE)).content.forEach { commentRepository.deleteById(it.id) }
            boardRepository.findAll(PageRequest.of(0, Int.MAX_VALUE)).content.forEach { boardRepository.deleteById(it.id) }
            categoryRepository.findAll().forEach { categoryRepository.deleteById(it.id) }
        }
        flushAndClear()
    }

    @BeforeEach
    fun setUp() {
        cleanupTestData()
        testCategory = createAndSaveCategory("COMMENT_TEST_CAT", "댓글 테스트용")
        testBoard = createAndSaveBoard(testCategory, "댓글 테스트 게시글")
    }

    @AfterEach
    override fun tearDown() {
        cleanupTestData()
    }

    private fun createAndSaveCategory(
        name: String,
        displayName: String,
    ): BoardCategory {
        val category =
            CategoryFixture
                .aCategory()
                .withName(name)
                .withDisplayName(displayName)
                .create()
        return saveAndFlush(category) { categoryRepository.save(it) }
    }

    private fun createAndSaveBoard(
        category: BoardCategory,
        title: String,
    ): Board {
        val board =
            BoardFixture
                .aBoard()
                .withCategory(category)
                .withTitle(title)
                .create()
        return saveAndFlush(board) { boardRepository.save(it) }
    }

    private fun createAndSaveComment(
        board: Board,
        author: String = "tester",
        content: String = "Test comment content",
        userId: Long? = 1L, // 회원 댓글 기본
        password: String? = null, // 비회원 댓글용
    ): Comment {
        val comment =
            CommentFixture
                .aComment()
                .withBoard(board)
                .withAuthor(author)
                .withContent(content)
                .withUserId(userId)
                .withPassword(password)
                .create()
        val savedComment = saveAndFlush(comment) { commentRepository.save(it) }
        return savedComment
    }

    @Nested
    @DisplayName("댓글 생성/수정/삭제 테스트")
    inner class CreateUpdateDeleteCommentTest {
        @Test
        @DisplayName("회원 댓글을 성공적으로 생성하고 저장한다")
        fun `save success - member comment`() {
            // given
            val author = "memberUser"
            val content = "회원이 작성한 댓글"
            val userId = 100L
            val commentToSave =
                CommentFixture
                    .aComment()
                    .withBoard(testBoard)
                    .withAuthor(author)
                    .withContent(content)
                    .withUserId(userId)
                    .withPassword(null) // 회원 댓글은 password null
                    .create()

            // when
            val savedComment = saveAndFlush(commentToSave) { commentRepository.save(it) }

            // then
            assertThat(savedComment.id).isNotNull()
            assertThat(savedComment.author).isEqualTo(author)
            assertThat(savedComment.content).isEqualTo(content)
            assertThat(savedComment.userId).isEqualTo(userId)
            assertThat(savedComment.password).isNull()
            assertThat(savedComment.board.id).isEqualTo(testBoard.id)
            assertThat(savedComment.createdAt).isNotNull()
        }

        @Test
        @DisplayName("비회원 댓글을 성공적으로 생성하고 저장한다")
        fun `save success - guest comment`() {
            // given
            val author = "guestUser"
            val content = "비회원이 작성한 댓글"
            val password = "guestPassword" // 비회원 댓글은 password 사용
            val commentToSave =
                CommentFixture
                    .aComment()
                    .withBoard(testBoard)
                    .withAuthor(author)
                    .withContent(content)
                    .withUserId(null) // 비회원 댓글은 userId null
                    .withPassword(password)
                    .create()

            // when
            val savedComment = saveAndFlush(commentToSave) { commentRepository.save(it) }

            // then
            assertThat(savedComment.id).isNotNull()
            assertThat(savedComment.author).isEqualTo(author)
            assertThat(savedComment.content).isEqualTo(content)
            assertThat(savedComment.userId).isNull()
            assertThat(savedComment.password).isEqualTo(password)
            assertThat(savedComment.board.id).isEqualTo(testBoard.id)
        }

        @Test
        @DisplayName("댓글 내용(content)이 최대 길이를 초과하면 IllegalArgumentException 예외가 발생한다")
        fun `create fail - content too long`() {
            // given
            val longContent = "a".repeat(CommentConstants.MAX_CONTENT_LENGTH + 1)

            // when & then
            assertThrows<IllegalArgumentException> {
                Comment.create(longContent, "user", null, testBoard, 1L)
            }
        }

        @Test
        @DisplayName("댓글 내용(content)이 비어있거나 공백이면 IllegalArgumentException 예외가 발생한다")
        fun `create fail - content blank`() {
            assertThrows<IllegalArgumentException> {
                Comment.create("", "user", null, testBoard, 1L)
            }
            assertThrows<IllegalArgumentException> {
                Comment.create("   ", "user", null, testBoard, 1L)
            }
        }

        @Test
        @DisplayName("댓글 작성자(author)가 비어있거나 공백이면 IllegalArgumentException 예외가 발생한다")
        fun `create fail - author blank`() {
            assertThrows<IllegalArgumentException> {
                Comment.create("Valid Content", "", null, testBoard, 1L)
            }
            assertThrows<IllegalArgumentException> {
                Comment.create("Valid Content", "   ", null, testBoard, 1L)
            }
        }

        @Test
        @DisplayName("댓글 내용을 성공적으로 수정한다 (엔티티 메소드 + save)")
        fun `update success - content`() {
            // given
            val savedComment = createAndSaveComment(testBoard, content = "수정 전 내용")
            val newContent = "댓글 내용이 수정되었습니다."

            // when
            val foundComment = commentRepository.findById(savedComment.id)
            foundComment.updateContent(newContent)
            saveAndFlush(foundComment) { commentRepository.save(it) }
            flushAndClear()

            val updatedComment = commentRepository.findById(savedComment.id)

            // then
            assertThat(updatedComment.content).isEqualTo(newContent)
            assertThat(updatedComment.updatedAt).isAfter(savedComment.createdAt)
        }

        @Test
        @DisplayName("댓글 내용 수정 시 유효하지 않은 내용(길이 초과/빈 값)이면 IllegalArgumentException 예외 발생")
        fun `update fail - invalid content`() {
            // given
            val savedComment = createAndSaveComment(testBoard)
            val longContent = "a".repeat(CommentConstants.MAX_CONTENT_LENGTH + 1)

            // when & then
            val foundComment = commentRepository.findById(savedComment.id)
            assertThrows<IllegalArgumentException>("길이 초과") {
                foundComment.updateContent(longContent)
            }
            assertThrows<IllegalArgumentException>("빈 값") {
                foundComment.updateContent("")
            }
            assertThrows<IllegalArgumentException>("공백") {
                foundComment.updateContent("   ")
            }
        }

        @Test
        @DisplayName("존재하지 않는 댓글 ID로 삭제 시도 시 오류가 발생하지 않는다")
        fun `deleteById fail - not found no error`() {
            // given
            val nonExistentId = 9999L

            // when & then
            assertDoesNotThrow {
                commentRepository.deleteById(nonExistentId)
                flushAndClear()
            }
        }

        @Test
        @DisplayName("게시글 ID로 해당 게시글의 모든 댓글을 삭제한다")
        fun `deleteByBoardId success`() {
            // given
            val board1 = testBoard
            val board2 = createAndSaveBoard(testCategory, "다른 게시글")
            val c1b1 = createAndSaveComment(board1, content = "Board1 Comment1")
            val c2b1 = createAndSaveComment(board1, content = "Board1 Comment2")
            val c1b2 = createAndSaveComment(board2, content = "Board2 Comment1")
            flushAndClear()

            assertThat(commentRepository.countByBoardId(board1.id)).isEqualTo(2)
            assertThat(commentRepository.countByBoardId(board2.id)).isEqualTo(1)

            // when
            executeInTransaction {
                commentRepository.deleteByBoardId(board1.id)
            }
            flushAndClear()

            // then
            assertThat(commentRepository.countByBoardId(board1.id)).isEqualTo(0)
            assertThat(commentRepository.existsById(c1b1.id)).isFalse()
            assertThat(commentRepository.existsById(c2b1.id)).isFalse()
            assertThat(commentRepository.countByBoardId(board2.id)).isEqualTo(1)
            assertThat(commentRepository.existsById(c1b2.id)).isTrue()
        }

        @Test
        @DisplayName("댓글이 없는 게시글 ID로 삭제 시도 시 오류가 발생하지 않는다")
        fun `deleteByBoardId success - no comments to delete`() {
            // given
            val boardIdWithNoComments = testBoard.id
            assertThat(commentRepository.countByBoardId(boardIdWithNoComments)).isEqualTo(0)

            // when & then
            assertDoesNotThrow {
                executeInTransaction {
                    commentRepository.deleteByBoardId(boardIdWithNoComments)
                }
                flushAndClear()
            }
            assertThat(commentRepository.countByBoardId(boardIdWithNoComments)).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("댓글 조회 테스트 (기본 및 조건별)")
    inner class ReadCommentTest {
        private lateinit var board1: Board
        private lateinit var board2: Board
        private lateinit var c1b1: Comment // board1, userA
        private lateinit var c2b1: Comment // board1, userB
        private lateinit var c3b1: Comment // board1, userA
        private lateinit var c1b2: Comment // board2, userA

        @BeforeEach
        fun setupReadData() {
            board1 = testBoard
            board2 = createAndSaveBoard(testCategory, "다른 게시글")
            c1b1 = createAndSaveComment(board1, author = "userA", content = "A-B1-C1")
            c2b1 = createAndSaveComment(board1, author = "userB", content = "B-B1-C1")
            c3b1 = createAndSaveComment(board1, author = "userA", content = "A-B1-C2") // 시간차를 위해 나중에 생성
            c1b2 = createAndSaveComment(board2, author = "userA", content = "A-B2-C1")
            flushAndClear()
        }

        @Test
        @DisplayName("ID로 댓글을 성공적으로 조회한다")
        fun `findById success`() {
            // when
            val foundComment = commentRepository.findById(c1b1.id)
            // then
            assertThat(foundComment.id).isEqualTo(c1b1.id)
            assertThat(foundComment.content).isEqualTo(c1b1.content)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 댓글 조회 시 JpaObjectRetrievalFailureException 예외가 발생한다")
        fun `findById fail - not found`() {
            assertThrows<JpaObjectRetrievalFailureException> {
                commentRepository.findById(9999L)
            }
        }

        @Test
        @DisplayName("ID 목록으로 여러 댓글을 성공적으로 조회한다")
        fun `findAllByIds success`() {
            // given
            val idsToFind = listOf(c1b1.id, c1b2.id, c3b1.id)
            // when
            val foundComments = commentRepository.findAllByIds(idsToFind)
            // then
            assertThat(foundComments).hasSize(3)
            assertThat(foundComments).extracting("id").containsExactlyInAnyOrder(c1b1.id, c1b2.id, c3b1.id)
        }

        @Test
        @DisplayName("ID 목록 조회 시 존재하지 않는 ID가 포함되면 있는 것만 반환한다")
        fun `findAllByIds success - with non-existent id`() {
            // given
            val idsToFind = listOf(c1b1.id, 9999L, c3b1.id)
            // when
            val foundComments = commentRepository.findAllByIds(idsToFind)
            // then
            assertThat(foundComments).hasSize(2)
            assertThat(foundComments).extracting("id").containsExactlyInAnyOrder(c1b1.id, c3b1.id)
        }

        @Test
        @DisplayName("댓글 ID 존재 여부를 확인한다")
        fun `existsById success`() {
            assertThat(commentRepository.existsById(c1b1.id)).isTrue()
            assertThat(commentRepository.existsById(9999L)).isFalse()
        }

        @Test
        @DisplayName("특정 게시글(boardId)의 댓글 목록을 페이지네이션하여 조회한다")
        fun `findByBoardId success - pagination`() {
            // given
            val boardId = board1.id
            val pageable = PageRequest.of(0, 2, Sort.by("id").ascending()) // ID 오름차순, 2개씩

            // when: Page 1
            val page1 = commentRepository.findByBoardId(boardId, pageable)

            // then: Page 1
            assertThat(page1.totalElements).isEqualTo(3) // board1의 댓글 총 3개
            assertThat(page1.totalPages).isEqualTo(2)
            assertThat(page1.content).hasSize(2)
            assertThat(page1.content).extracting("id").containsExactly(c1b1.id, c2b1.id)

            // when: Page 2
            val page2 = commentRepository.findByBoardId(boardId, pageable.next())

            // then: Page 2
            assertThat(page2.content).hasSize(1)
            assertThat(page2.content[0].id).isEqualTo(c3b1.id)
        }

        @Test
        @DisplayName("댓글이 없는 게시글 ID로 조회 시 빈 페이지를 반환한다")
        fun `findByBoardId success - empty result`() {
            // given
            val boardWithNoComments = createAndSaveBoard(testCategory, "댓글 없는 게시글")
            val pageable = PageRequest.of(0, 10)

            // when
            val page = commentRepository.findByBoardId(boardWithNoComments.id, pageable)

            // then
            assertThat(page.isEmpty).isTrue()
            assertThat(page.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("특정 게시글(boardId)의 최근 댓글 목록을 조회한다 (createdAt 내림차순)")
        fun `findRecentComments success`() {
            // given
            val boardId = board1.id
            val pageable = PageRequest.of(0, 2)

            // when
            val recentComments = commentRepository.findRecentComments(boardId, pageable)

            // then
            assertThat(recentComments).hasSize(2)
            assertThat(recentComments).extracting("id").containsExactly(c3b1.id, c2b1.id)
        }

        @Test
        @DisplayName("특정 작성자(author)의 댓글 목록을 페이지네이션하여 조회한다")
        fun `findByAuthor success - pagination`() {
            // given
            val author = "userA"
            val pageable = PageRequest.of(0, 1, Sort.by("id").ascending())

            // when: Page 1
            val page1 = commentRepository.findByAuthor(author, pageable)

            // then: Page 1
            assertThat(page1.totalElements).isEqualTo(3)
            assertThat(page1.totalPages).isEqualTo(3)
            assertThat(page1.content).hasSize(1)
            assertThat(page1.content[0].id).isEqualTo(c1b1.id)

            // when: Page 2
            val page2 = commentRepository.findByAuthor(author, pageable.next())
            // then: Page 2
            assertThat(page2.content).hasSize(1)
            assertThat(page2.content[0].id).isEqualTo(c3b1.id)

            // when: Page 3
            val page3 = commentRepository.findByAuthor(author, pageable.next().next())
            // then: Page 3
            assertThat(page3.content).hasSize(1)
            assertThat(page3.content[0].id).isEqualTo(c1b2.id)
        }

        @Test
        @DisplayName("특정 게시글(boardId) 내 특정 작성자(author)의 댓글 목록을 조회한다")
        fun `findByBoardIdAndAuthor success`() {
            // given
            val boardId = board1.id
            val author = "userA"
            val pageable = PageRequest.of(0, 10)

            // when
            val page = commentRepository.findByBoardIdAndAuthor(boardId, author, pageable)

            // then
            assertThat(page.totalElements).isEqualTo(2)
            assertThat(page.content).hasSize(2)
            assertThat(page.content).extracting("id").containsExactlyInAnyOrder(c1b1.id, c3b1.id)
            assertThat(page.content).allMatch { it.board.id == boardId && it.author == author }
        }

        @Test
        @DisplayName("특정 게시글(boardId) 내 특정 작성자(author)의 댓글 존재 여부를 확인한다")
        fun `existsByBoardIdAndAuthor success`() {
            assertThat(commentRepository.existsByBoardIdAndAuthor(board1.id, "userA")).isTrue()
            assertThat(commentRepository.existsByBoardIdAndAuthor(board1.id, "userB")).isTrue()
            assertThat(commentRepository.existsByBoardIdAndAuthor(board1.id, "nonexistent")).isFalse()
            assertThat(commentRepository.existsByBoardIdAndAuthor(board2.id, "userA")).isTrue()
            assertThat(commentRepository.existsByBoardIdAndAuthor(board2.id, "userB")).isFalse()
            assertThat(commentRepository.existsByBoardIdAndAuthor(999L, "userA")).isFalse()
        }
    }

    @Nested
    @DisplayName("댓글 조회 테스트 (커서 기반)")
    inner class ReadCommentCursorTest {
        private lateinit var board: Board
        private lateinit var commentsDesc: List<Comment>
        private lateinit var commentsByAuthorADesc: List<Comment>

        @BeforeEach
        fun setupCursorData() {
            board = testBoard
            val c1 = createAndSaveComment(board, author = "userA", content = "C1")
            val c2 = createAndSaveComment(board, author = "userB", content = "C2")
            val c3 = createAndSaveComment(board, author = "userA", content = "C3")
            val c4 = createAndSaveComment(board, author = "userC", content = "C4")
            val c5 = createAndSaveComment(board, author = "userA", content = "C5")
            flushAndClear()

            commentsDesc = listOf(c1, c2, c3, c4, c5).sortedByDescending { it.id }
            commentsByAuthorADesc = listOf(c1, c3, c5).sortedByDescending { it.id }
        }

        @Test
        @DisplayName("특정 게시글(boardId)의 댓글 목록을 커서 기반으로 조회한다 (ID 내림차순)")
        fun `findCommentsByBoardIdAfter success`() {
            // given
            val boardId = board.id
            val limit = 2
            val allIds = commentsDesc.map { it.id }

            // when: Page 1
            val page1 = commentRepository.findCommentsByBoardIdAfter(boardId, null, limit)
            // then: Page 1
            assertThat(page1).hasSize(limit)
            assertThat(page1.map { it.id }).containsExactlyElementsOf(allIds.subList(0, limit))

            // when: Page 2
            val lastId1 = page1.last().id
            val page2 = commentRepository.findCommentsByBoardIdAfter(boardId, lastId1, limit)
            // then: Page 2
            assertThat(page2).hasSize(limit)
            assertThat(page2.map { it.id }).containsExactlyElementsOf(allIds.subList(limit, limit * 2))

            // when: Page 3
            val lastId2 = page2.last().id
            val page3 = commentRepository.findCommentsByBoardIdAfter(boardId, lastId2, limit)
            // then: Page 3
            val expectedSize3 = allIds.size - (limit * 2)
            assertThat(page3).hasSize(expectedSize3)
            assertThat(page3.map { it.id }).containsExactlyElementsOf(allIds.subList(limit * 2, allIds.size))

            // when: Page 4 (마지막 다음)
            val lastId3 = page3.last().id
            val page4 = commentRepository.findCommentsByBoardIdAfter(boardId, lastId3, limit)
            // then: Page 4
            assertThat(page4).isEmpty()
        }

        @Test
        @DisplayName("특정 작성자(author)의 댓글 목록을 커서 기반으로 조회한다 (ID 내림차순)")
        fun `findCommentsByAuthorAfter success`() {
            // given
            val author = "userA"
            val limit = 1
            val allAuthorAIds = commentsByAuthorADesc.map { it.id }

            // when: Page 1
            val page1 = commentRepository.findCommentsByAuthorAfter(author, null, limit)
            // then: Page 1
            assertThat(page1).hasSize(limit)
            assertThat(page1.map { it.id }).containsExactlyElementsOf(allAuthorAIds.subList(0, limit))

            // when: Page 2
            val lastId1 = page1.last().id
            val page2 = commentRepository.findCommentsByAuthorAfter(author, lastId1, limit)
            // then: Page 2
            assertThat(page2).hasSize(limit)
            assertThat(page2.map { it.id }).containsExactlyElementsOf(allAuthorAIds.subList(limit, limit * 2))

            // when: Page 3
            val lastId2 = page2.last().id
            val page3 = commentRepository.findCommentsByAuthorAfter(author, lastId2, limit)
            // then: Page 3
            val expectedSize3 = allAuthorAIds.size - (limit * 2)
            assertThat(page3).hasSize(expectedSize3)
            assertThat(page3.map { it.id }).containsExactlyElementsOf(allAuthorAIds.subList(limit * 2, allAuthorAIds.size))

            // when: Page 4 (마지막 다음)
            val lastId3 = page3.last().id
            val page4 = commentRepository.findCommentsByAuthorAfter(author, lastId3, limit)
            // then: Page 4
            assertThat(page4).isEmpty()
        }
    }

    @Nested
    @DisplayName("댓글 통계 테스트")
    inner class CommentStatisticsTest {
        @Test
        @DisplayName("특정 게시글(boardId)의 댓글 수를 정확히 계산한다")
        fun `countByBoardId success`() {
            // given
            val board1 = testBoard
            val board2 = createAndSaveBoard(testCategory, "다른 게시글")
            createAndSaveComment(board1, content = "B1 C1")
            createAndSaveComment(board1, content = "B1 C2")
            createAndSaveComment(board2, content = "B2 C1")
            flushAndClear()

            // when
            val count1 = commentRepository.countByBoardId(board1.id)
            val count2 = commentRepository.countByBoardId(board2.id)
            val count3 = commentRepository.countByBoardId(9999L) // 없는 게시글

            // then
            assertThat(count1).isEqualTo(2)
            assertThat(count2).isEqualTo(1)
            assertThat(count3).isEqualTo(0)
        }
    }
}
