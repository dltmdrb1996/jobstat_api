package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.CategoryFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.entity.BoardCategory
import com.example.jobstat.board.internal.service.BoardServiceImpl
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("DeleteBoard Usecase 테스트")
class DeleteBoardTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardServiceImpl
    private lateinit var deleteBoard: DeleteBoard
    private lateinit var testCategory: BoardCategory
    private lateinit var passwordUtil: FakePasswordUtil

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        passwordUtil = FakePasswordUtil()
        deleteBoard = DeleteBoard(boardService, passwordUtil, Validation.buildDefaultValidatorFactory().validator)
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
    @DisplayName("게시글 삭제에 성공한다")
    fun deleteBoardSuccessfully() {
        val board =
            boardService.createBoard(
                title = "Delete Me",
                content = "Content",
                author = "testUser",
                categoryId = testCategory.id,
                password = passwordUtil.encode("1234"),
            )
        val request = DeleteBoard.Request(boardId = board.id, password = "1234")
        val response = deleteBoard(request)
        assertTrue(response.success)
        assertFailsWith<EntityNotFoundException> { boardService.getBoardById(board.id) }
    }
}
