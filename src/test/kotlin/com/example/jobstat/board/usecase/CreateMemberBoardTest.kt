package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.CategoryFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.BoardServiceImpl
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("CreateMemberBoard Usecase 테스트")
class CreateMemberBoardTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardServiceImpl
    private lateinit var createMemberBoard: CreateMemberBoard
    private var testCategoryId by Delegates.notNull<Long>()

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository, commentRepository)
        createMemberBoard = CreateMemberBoard(boardService, Validation.buildDefaultValidatorFactory().validator)
        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Category 1")
                .withDescription("Desc 1")
                .create()
        testCategoryId = categoryRepository.save(category).id
    }

    @Test
    @DisplayName("회원 게시글 생성에 성공한다")
    fun createMemberBoardSuccessfully() {
        val request =
            CreateMemberBoard.Request(
                title = "Member Post",
                content = "Member Content",
                author = "memberUser",
                categoryId = testCategoryId,
            )
        val response = createMemberBoard(request)
        assertTrue(response.id > 0)
        assertEquals("Member Post", response.title)
        assertNotNull(response.createdAt)
    }
}
