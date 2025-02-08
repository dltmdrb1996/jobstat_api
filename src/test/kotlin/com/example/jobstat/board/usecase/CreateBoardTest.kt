package com.example.jobstat.board.usecase

import CreateBoard
import com.example.jobstat.board.fake.CategoryFixture
import com.example.jobstat.board.fake.repository.FakeBoardRepository
import com.example.jobstat.board.fake.repository.FakeCategoryRepository
import com.example.jobstat.board.internal.entity.BoardCategory
import com.example.jobstat.board.internal.model.BoardType
import com.example.jobstat.board.internal.service.BoardServiceImpl
import com.example.jobstat.utils.FakePasswordUtil
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.*

@DisplayName("CreateBoard UseCase 테스트")
class CreateBoardTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var boardService: BoardServiceImpl
    private lateinit var createBoard: CreateBoard
    private lateinit var testCategory: BoardCategory

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        val passwordUtil = FakePasswordUtil()
        createBoard =
            CreateBoard(
                boardService,
                passwordUtil,
                Validation.buildDefaultValidatorFactory().validator,
            )

        testCategory =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Category 1")
                .withDescription("Desc 1")
                .create()
        categoryRepository.save(testCategory)
    }

    @Nested
    @DisplayName("게스트 게시글 생성")
    inner class GuestBoardTests {
        @Test
        @DisplayName("유효한 정보로 게스트 게시글을 생성할 수 있다")
        fun createValidGuestBoard() {
            val request =
                CreateBoard.Request(
                    title = "게스트 제목",
                    content = "게스트 내용",
                    author = "게스트사용자",
                    categoryId = testCategory.id,
                    boardOption = CreateBoard.BoardOption.Guest(password = "1234"),
                )

            val response = createBoard(request)

            assertTrue(response.id > 0)
            assertEquals("게스트 제목", response.title)
            assertEquals(BoardType.GUEST, response.type)
            assertNotNull(response.createdAt)
        }

        @Test
        @DisplayName("비밀번호가 유효하지 않으면 게스트 게시글을 생성할 수 없다")
        fun cannotCreateGuestBoardWithInvalidPassword() {
            val request =
                CreateBoard.Request(
                    title = "게스트 제목",
                    content = "게스트 내용",
                    author = "게스트사용자",
                    categoryId = testCategory.id,
                    boardOption = CreateBoard.BoardOption.Guest(password = "123"), // 4자 미만
                )

            assertFailsWith<ConstraintViolationException> {
                createBoard(request)
            }
        }
    }

    @Nested
    @DisplayName("회원 게시글 생성")
    inner class MemberBoardTests {
        @Test
        @DisplayName("유효한 정보로 회원 게시글을 생성할 수 있다")
        fun createValidMemberBoard() {
            val userId = 1L
            val request =
                CreateBoard.Request(
                    title = "회원 제목",
                    content = "회원 내용",
                    author = "회원사용자",
                    categoryId = testCategory.id,
                    boardOption = CreateBoard.BoardOption.Member(userId = userId),
                )

            val response = createBoard(request)

            assertTrue(response.id > 0)
            assertEquals("회원 제목", response.title)
            assertEquals(BoardType.MEMBER, response.type)
            assertNotNull(response.createdAt)
        }
    }

    @Nested
    @DisplayName("공통 검증")
    inner class CommonValidationTests {
        @Test
        @DisplayName("제목이 100자를 초과하면 생성할 수 없다")
        fun cannotCreateBoardWithTooLongTitle() {
            val request =
                CreateBoard.Request(
                    title = "a".repeat(101),
                    content = "내용",
                    author = "작성자",
                    categoryId = testCategory.id,
                    boardOption = CreateBoard.BoardOption.Guest(password = "1234"),
                )

            assertFailsWith<ConstraintViolationException> {
                createBoard(request)
            }
        }

        @Test
        @DisplayName("내용이 5000자를 초과하면 생성할 수 없다")
        fun cannotCreateBoardWithTooLongContent() {
            val request =
                CreateBoard.Request(
                    title = "제목",
                    content = "a".repeat(5001),
                    author = "작성자",
                    categoryId = testCategory.id,
                    boardOption = CreateBoard.BoardOption.Guest(password = "1234"),
                )

            assertFailsWith<ConstraintViolationException> {
                createBoard(request)
            }
        }
    }
}
