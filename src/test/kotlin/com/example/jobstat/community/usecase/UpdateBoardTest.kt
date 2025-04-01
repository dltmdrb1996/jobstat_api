package com.example.jobstat.community.usecase

import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.board.usecase.command.UpdateBoard
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.SecurityUtils
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
    private lateinit var securityUtils: SecurityUtils
    private var testBoardId: Long = 0L
    private var testCategoryId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        passwordUtil = FakePasswordUtil()
        securityUtils = mock()
        updateBoard = UpdateBoard(boardService, passwordUtil, securityUtils, Validation.buildDefaultValidatorFactory().validator)

        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Cat1")
                .withDescription("Desc")
                .create()
        val savedCategory = categoryRepository.save(category)
        testCategoryId = savedCategory.id

        // 게스트 게시글: 비밀번호로 생성 → password branch가 사용됨.
        testBoardId =
            boardService
                .createBoard(
                    title = "Original Title",
                    content = "Original Content",
                    author = "user",
                    categoryId = savedCategory.id,
                    password = passwordUtil.encode("pass123"),
                ).id
    }

    @Test
    @DisplayName("게시글 수정 (비밀번호로 생성된 게시글) 에 성공한다")
    fun updateBoardSuccessfully() {
        val request =
            UpdateBoard.ExecuteRequest(
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
            UpdateBoard.ExecuteRequest(
                boardId = testBoardId,
                title = "New Title",
                content = "New Content",
                password = "wrongPass",
            )
        assertFailsWith<AppException> {
            updateBoard(request)
        }
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시도시 실패한다")
    fun failToUpdateNonExistentBoard() {
        val request =
            UpdateBoard.ExecuteRequest(
                boardId = 999L,
                title = "New Title",
                content = "New Content",
                password = "pass123",
            )
        assertFailsWith<EntityNotFoundException> {
            updateBoard(request)
        }
    }

    @Test
    @DisplayName("로그인한 사용자는 비밀번호 없이 게시글을 수정할 수 있다")
    fun updateBoardWithoutPasswordByLoggedInUser() {
        // 로그인한 사용자의 경우 securityUtils.getCurrentUserId()가 board 생성 시 할당됨.
        whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
        val board =
            boardService.createBoard(
                title = "Original Title 2",
                content = "Original Content 2",
                author = "user",
                categoryId = testCategoryId,
                password = null,
                userId = 1L,
            )
        val request =
            UpdateBoard.ExecuteRequest(
                boardId = board.id,
                title = "Updated Title",
                content = "Updated Content",
                password = null,
            )
        val response = updateBoard(request)
        assertEquals("Updated Title", response.title)
        assertEquals("Updated Content", response.content)
        assertNotNull(response.updatedAt)
    }

    @Test
    @DisplayName("비로그인 사용자가 비밀번호 없이 게시글 수정 시도시 실패한다")
    fun updateBoardWithoutPasswordAndWithoutUserIdFails() {
        // board가 비밀번호 없이 생성되었으나, securityUtils.getCurrentUserId()가 null이면 인증 실패
        whenever(securityUtils.getCurrentUserId()).thenReturn(null)
        val board =
            boardService.createBoard(
                title = "Original Title 3",
                content = "Original Content 3",
                author = "user",
                categoryId = testCategoryId,
                password = null, // 비밀번호 없음 → board.userId를 할당해야 하는데 현재 로그인 사용자가 없음.
            )
        val request =
            UpdateBoard.ExecuteRequest(
                boardId = board.id,
                title = "Updated Title",
                content = "Updated Content",
                password = null,
            )
        assertFailsWith<AppException> {
            updateBoard(request)
        }.also {
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, it.errorCode)
        }
    }
}
