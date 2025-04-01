package com.example.jobstat.community.usecase

import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.comment.usecase.DeleteBoard
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("DeleteBoard Usecase 테스트")
class DeleteBoardTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardServiceImpl
    private lateinit var deleteBoard: DeleteBoard
    private lateinit var testCategory: com.example.jobstat.community.board.entity.BoardCategory
    private lateinit var passwordUtil: FakePasswordUtil
    private lateinit var securityUtils: SecurityUtils

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        passwordUtil = FakePasswordUtil()
        securityUtils = mock() // SecurityUtils 목 생성
        deleteBoard = DeleteBoard(boardService, passwordUtil, securityUtils, Validation.buildDefaultValidatorFactory().validator)
        testCategory =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Category 1")
                .withDescription("Desc 1")
                .create()
        categoryRepository.save(testCategory)
    }

    @Test
    @DisplayName("게시글 삭제 (비밀번호로 생성된 게시글) 에 성공한다")
    fun deleteBoardSuccessfully() {
        // 게스트 게시글: 비밀번호가 설정되어 있으므로 userId는 null이어야 함.
        val board =
            boardService.createBoard(
                title = "Delete Me",
                content = "Content",
                author = "testUser",
                categoryId = testCategory.id,
                password = passwordUtil.encode("1234"),
            )
        // 이 경우 securityUtils는 사용되지 않음.
        val testBoard = boardService.getBoardById(board.id)
        println(testBoard.password)

        val request = DeleteBoard.Request(password = "1234")
        val response = deleteBoard(request.of(board.id))

        assertTrue(response.success)
        assertFailsWith<EntityNotFoundException> { boardService.getBoardById(board.id) }
    }

    @Test
    @DisplayName("로그인한 사용자는 비밀번호 없이 게시글을 삭제할 수 있다")
    fun deleteBoardWithoutPasswordByLoggedInUser() {
        // 로그인한 사용자의 경우 securityUtils.getCurrentUserId() 값이 board 생성 시 할당됨.
        whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
        val board =
            boardService.createBoard(
                title = "Delete Me 2",
                content = "Content 2",
                author = "testUser",
                categoryId = testCategory.id,
                password = null,
                userId = 1L,
            )

        val request = DeleteBoard.Request(password = null)
        val response = deleteBoard(request.of(board.id))

        assertTrue(response.success)
        assertFailsWith<EntityNotFoundException> { boardService.getBoardById(board.id) }
    }

    @Test
    @DisplayName("비로그인 사용자가 비밀번호 없이 게시글 삭제 시도하면 실패한다")
    fun deleteBoardWithoutPasswordAndWithoutUserIdFails() {
        whenever(securityUtils.getCurrentUserId()).thenReturn(null)
        val board =
            boardService.createBoard(
                title = "Delete Me 3",
                content = "Content 3",
                author = "testUser",
                categoryId = testCategory.id,
                password = passwordUtil.encode("1234"),
            )

        val request = DeleteBoard.Request(password = null)
        assertFailsWith<AppException> {
            deleteBoard(request.of(board.id))
        }.also {
            assertEquals(ErrorCode.AUTHENTICATION_FAILURE, it.errorCode)
        }
    }
}
