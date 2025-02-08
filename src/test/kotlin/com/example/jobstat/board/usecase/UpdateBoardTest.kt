package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.CategoryFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.BoardServiceImpl
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DisplayName("UpdateBoard Usecase 테스트")
class UpdateBoardTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardServiceImpl
    private lateinit var updateBoard: UpdateBoard
    private lateinit var passwordUtil: FakePasswordUtil
    private var testBoardId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        passwordUtil = FakePasswordUtil()
        updateBoard = UpdateBoard(boardService, passwordUtil, Validation.buildDefaultValidatorFactory().validator)

        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Cat1")
                .withDescription("Desc")
                .create()
        val savedCategory = categoryRepository.save(category)
        // 생성 시 비밀번호 포함 (암호화된 값)
        testBoardId = boardService.createBoard("Original Title", "Original Content", "user", savedCategory.id, passwordUtil.encode("pass123")).id
    }

    @Test
    @DisplayName("게시글 수정에 성공한다")
    fun updateBoardSuccessfully() {
        val request =
            UpdateBoard.Request(
                boardId = testBoardId,
                title = "New Title",
                content = "New Content",
                password = "pass123",
            )
        val response = updateBoard(request)
        assertEquals("New Title", response.title)
        assertEquals("New Content", response.content)
        assertNotNull(response.updatedAt)
    }

    @Test
    @DisplayName("잘못된 비밀번호로 수정 시도시 실패한다")
    fun failToUpdateWithWrongPassword() {
        val request =
            UpdateBoard.Request(
                boardId = testBoardId,
                title = "New Title",
                content = "New Content",
                password = "wrongPass",
            )
        assertFailsWith<IllegalArgumentException> {
            updateBoard(request)
        }
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시도시 실패한다")
    fun failToUpdateNonExistentBoard() {
        val request =
            UpdateBoard.Request(
                boardId = 999L,
                title = "New Title",
                content = "New Content",
                password = "pass123",
            )
        assertFailsWith<EntityNotFoundException> {
            updateBoard(request)
        }
    }
}
