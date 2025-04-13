//package com.example.jobstat.community.usecase
//
//import com.example.jobstat.community.board.service.BoardServiceImpl
//import com.example.jobstat.community.board.usecase.command.CreateBoard
//import com.example.jobstat.community.fake.CategoryFixture
//import com.example.jobstat.community.fake.repository.FakeBoardRepository
//import com.example.jobstat.community.fake.repository.FakeCategoryRepository
//import com.example.jobstat.core.error.AppException
//import com.example.jobstat.core.error.ErrorCode
//import com.example.jobstat.core.global.utils.SecurityUtils
//import com.example.jobstat.utils.FakePasswordUtil
//import jakarta.validation.ConstraintViolationException
//import jakarta.validation.Validation
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//import org.mockito.kotlin.mock
//import org.mockito.kotlin.verify
//import org.mockito.kotlin.whenever
//import kotlin.test.*
//
//@DisplayName("CreateBoard UseCase 테스트")
//class CreateBoardTest {
//    private lateinit var boardRepository: FakeBoardRepository
//    private lateinit var categoryRepository: FakeCategoryRepository
//    private lateinit var boardService: BoardServiceImpl
//    private lateinit var createBoard: CreateBoard
//    private lateinit var testCategory: com.example.jobstat.community.board.entity.BoardCategory
//    private lateinit var securityUtils: SecurityUtils
//    private lateinit var passwordUtil: FakePasswordUtil
//
//    @BeforeEach
//    fun setUp() {
//        boardRepository = FakeBoardRepository()
//        categoryRepository = FakeCategoryRepository()
//        boardService = BoardServiceImpl(boardRepository, categoryRepository)
//        passwordUtil = FakePasswordUtil()
//        securityUtils = mock()
//        createBoard =
//            CreateBoard(
//                boardService,
//                securityUtils,
//                passwordUtil,
//                Validation.buildDefaultValidatorFactory().validator,
//            )
//
//        testCategory =
//            CategoryFixture
//                .aCategory()
//                .withName("CAT1")
//                .withDisplayName("Category 1")
//                .withDescription("Desc 1")
//                .create()
//        categoryRepository.save(testCategory)
//    }
//
//    @Nested
//    @DisplayName("게시글 생성 테스트")
//    inner class CreateBoardTests {
//        @Test
//        @DisplayName("비밀번호로 게시글을 생성할 수 있다")
//        fun createBoardWithPassword() {
//            // 비로그인 사용자는 securityUtils.getCurrentUserId()가 null이어야 함
//            whenever(securityUtils.getCurrentUserId()).thenReturn(null)
//            val request =
//                CreateBoard.Request(
//                    title = "제목",
//                    content = "열글자이상의내용입니다",
//                    author = "작성자",
//                    categoryId = testCategory.id,
//                    password = "1234",
//                )
//
//            val response = createBoard(request)
//            val board = boardService.getBoardById(response.id)
//
//            assertTrue(response.id > 0)
//            assertEquals("제목", response.title)
//            assertNull(board.userId)
//            assertNotNull(board.password)
//            assertNotNull(response.createdAt)
//        }
//
//        @Test
//        @DisplayName("로그인한 사용자는 비밀번호 없이 게시글을 생성할 수 있다")
//        fun createBoardWithUserId() {
//            whenever(securityUtils.getCurrentUserId()).thenReturn(1L)
//            val request =
//                CreateBoard.Request(
//                    title = "제목",
//                    content = "열글자이상의내용입니다",
//                    author = "작성자",
//                    categoryId = testCategory.id,
//                    password = null,
//                )
//
//            val response = createBoard(request)
//
//            assertTrue(response.id > 0)
//            assertEquals("제목", response.title)
//            assertNotNull(response.createdAt)
//            // 로그인한 경우 securityUtils.getCurrentUserId()가 호출되어 userId가 board에 할당됨
//            verify(securityUtils).getCurrentUserId()
//        }
//
//        @Test
//        @DisplayName("비로그인 사용자가 비밀번호 없이 게시글을 생성하면 실패한다")
//        fun createBoardWithoutPasswordAndUserIdFails() {
//            whenever(securityUtils.getCurrentUserId()).thenReturn(null)
//            val request =
//                CreateBoard.Request(
//                    title = "제목",
//                    content = "열글자이상의내용입니다",
//                    author = "작성자",
//                    categoryId = testCategory.id,
//                    password = null,
//                )
//
//            assertFailsWith<AppException> {
//                createBoard(request)
//            }.also {
//                assertEquals(ErrorCode.AUTHENTICATION_FAILURE, it.errorCode)
//            }
//        }
//
//        @Test
//        @DisplayName("비밀번호가 4자 미만이면 실패한다")
//        fun passwordTooShortFails() {
//            val request =
//                CreateBoard.Request(
//                    title = "제목",
//                    content = "열글자이상의내용입니다",
//                    author = "작성자",
//                    categoryId = testCategory.id,
//                    password = "123",
//                )
//
//            assertFailsWith<ConstraintViolationException> {
//                createBoard(request)
//            }
//        }
//
//        @Test
//        @DisplayName("비밀번호가 15자 초과면 실패한다")
//        fun passwordTooLongFails() {
//            val request =
//                CreateBoard.Request(
//                    title = "제목",
//                    content = "열다섯글자이상의내용입니다",
//                    author = "작성자",
//                    categoryId = testCategory.id,
//                    password = "1234567890134534",
//                )
//
//            assertFailsWith<ConstraintViolationException> {
//                createBoard(request)
//            }
//        }
//
//        @Test
//        @DisplayName("제목이 100자를 초과하면 생성할 수 없다")
//        fun cannotCreateBoardWithTooLongTitle() {
//            val request =
//                CreateBoard.Request(
//                    title = "a".repeat(101),
//                    content = "열글자이상의내용입니다",
//                    author = "작성자",
//                    categoryId = testCategory.id,
//                    password = "1234",
//                )
//
//            assertFailsWith<ConstraintViolationException> {
//                createBoard(request)
//            }
//        }
//
//        @Test
//        @DisplayName("내용이 5000자를 초과하면 생성할 수 없다")
//        fun cannotCreateBoardWithTooLongContent() {
//            val request =
//                CreateBoard.Request(
//                    title = "제목",
//                    content = "a".repeat(5001),
//                    author = "작성자",
//                    categoryId = testCategory.id,
//                    password = "1234",
//                )
//
//            assertFailsWith<ConstraintViolationException> {
//                createBoard(request)
//            }
//        }
//    }
//}
