package com.example.jobstat.board.usecase

import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.fake.repository.FakeCommentRepository
import com.example.jobstat.board.internal.service.BoardService
import com.example.jobstat.board.internal.service.BoardServiceImpl
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("ValidateBoardTitle Usecase 테스트")
class ValidateBoardTitleTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var validateBoardTitle: ValidateBoardTitle

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository, commentRepository)
        validateBoardTitle = ValidateBoardTitle(boardService, Validation.buildDefaultValidatorFactory().validator)

        // 기존 게시글 생성
        boardService.createBoard(
            title = "Existing Title",
            content = "Content",
            author = "testUser",
            categoryId =
                categoryRepository
                    .save(
                        com.example.jobstat.board.fake.CategoryFixture
                            .aCategory()
                            .withName("CAT1")
                            .withDisplayName("Cat1")
                            .withDescription("Desc")
                            .create(),
                    ).id,
            password = null,
        )
    }

    @Test
    @DisplayName("동일 작성자의 중복 제목은 사용할 수 없다")
    fun rejectDuplicateTitleFromSameAuthor() {
        val request = ValidateBoardTitle.Request(author = "testUser", title = "Existing Title")
        val response = validateBoardTitle(request)
        assertFalse(response.isAvailable)
    }

    @Test
    @DisplayName("새로운 제목은 사용할 수 있다")
    fun allowNewTitle() {
        val request = ValidateBoardTitle.Request(author = "testUser", title = "New Title")
        val response = validateBoardTitle(request)
        assertTrue(response.isAvailable)
    }
}
