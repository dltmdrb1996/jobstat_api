package com.example.jobstat.community.usecase

import com.example.jobstat.community.board.entity.BoardCategory
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.board.usecase.CreateGuestBoard
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("CreateGuestBoard Usecase 테스트")
class CreateGuestBoardTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardServiceImpl
    private lateinit var createGuestBoard: CreateGuestBoard
    private lateinit var testCategory: BoardCategory

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        // FakePasswordUtil는 단순히 접두사 "encoded:"를 붙임 (테스트용)
        val passwordUtil = FakePasswordUtil()
        createGuestBoard = CreateGuestBoard(boardService, passwordUtil, Validation.buildDefaultValidatorFactory().validator)

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
    @DisplayName("게스트 게시글 생성에 성공한다")
    fun createGuestBoardSuccessfully() {
        val request =
            CreateGuestBoard.Request(
                title = "게스트 제목",
                content = "게스트 내용",
                author = "게스트사용자",
                password = "1234",
                categoryId = testCategory.id,
            )
        val response = createGuestBoard(request)
        assertTrue(response.id > 0)
        assertEquals("게스트 제목", response.title)
    }
}
