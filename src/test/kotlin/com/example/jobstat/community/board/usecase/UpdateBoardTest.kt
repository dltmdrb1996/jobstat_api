// package com.example.jobstat.community.board.usecase
//
// import com.example.jobstat.community.event.CommunityEventPublisher
// import com.example.jobstat.community.board.entity.Board
// import com.example.jobstat.community.board.service.BoardService
// import com.example.jobstat.community.board.usecase.command.UpdateBoard
// import com.example.jobstat.core.error.AppException
// import com.example.jobstat.core.security.PasswordUtil
// import com.example.jobstat.core.global.utils.SecurityUtils
// import jakarta.validation.Validation
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.extension.ExtendWith
// import org.mockito.Mock
// import org.mockito.Mockito.*
// import org.mockito.junit.jupiter.MockitoExtension
// import org.springframework.test.context.ActiveProfiles
// import java.time.LocalDateTime
// import java.util.*
// import kotlin.test.assertEquals
// import kotlin.test.assertFailsWith
//
// @ExtendWith(MockitoExtension::class)
// @ActiveProfiles("test")
// class UpdateBoardTest {
//
//    @Mock
//    private lateinit var boardService: BoardService
//
//    @Mock
//    private lateinit var passwordUtil: PasswordUtil
//
//    @Mock
//    private lateinit var securityUtils: SecurityUtils
//
//    @Mock
//    private lateinit var eventPublisher: CommunityEventPublisher
//
//    private lateinit var updateBoard: UpdateBoard
//
//    private val validator = Validation.buildDefaultValidatorFactory().validator
//
//    @BeforeEach
//    fun setUp() {
//        updateBoard = UpdateBoard(
//            boardService = boardService,
//            passwordUtil = passwordUtil,
//            securityUtils = securityUtils,
//            eventPublisher = eventPublisher,
//            validator = validator
//        )
//    }
//
//    @Test
//    fun `회원 게시글 수정 - 성공`() {
//        // given
//        val userId = "user123"
//        val boardId = 1L
//        val title = "업데이트된 제목"
//        val content = "업데이트된 내용"
//
//        val originalBoard = Board(
//            id = boardId,
//            title = "원래 제목",
//            content = "원래 내용",
//            author = "테스터",
//            userId = userId,
//            password = null,
//            createdAt = LocalDateTime.now().minusDays(1),
//            updatedAt = LocalDateTime.now().minusDays(1)
//        )
//
//        val updatedBoard = originalBoard.copy(
//            title = title,
//            content = content,
//            updatedAt = LocalDateTime.now()
//        )
//
//        `when`(boardService.getBoard(boardId)).thenReturn(originalBoard)
//        `when`(securityUtils.getCurrentUserId()).thenReturn(userId)
//        `when`(boardService.updateBoard(boardId, title, content)).thenReturn(updatedBoard)
//
//        // when
//        val request = UpdateBoard.ExecuteRequest(
//            boardId = boardId,
//            title = title,
//            content = content,
//            password = null
//        )
//
//        val response = updateBoard.execute(request)
//
//        // then
//        assertEquals(boardId, response.id)
//        assertEquals(title, response.title)
//        assertEquals(content, response.content)
//
//        verify(boardService).getBoard(boardId)
//        verify(securityUtils).getCurrentUserId()
//        verify(boardService).updateBoard(boardId, title, content)
//        verify(eventPublisher).publishBoardUpdated(
//            board = updatedBoard,
//            userId = userId
//        )
//    }
//
//    @Test
//    fun `게스트 게시글 수정 - 성공`() {
//        // given
//        val boardId = 1L
//        val title = "업데이트된 제목"
//        val content = "업데이트된 내용"
//        val password = "p@ssw0rd"
//        val hashedPassword = "hashed_password"
//
//        val originalBoard = Board(
//            id = boardId,
//            title = "원래 제목",
//            content = "원래 내용",
//            author = "게스트",
//            userId = null,
//            password = hashedPassword,
//            createdAt = LocalDateTime.now().minusDays(1),
//            updatedAt = LocalDateTime.now().minusDays(1)
//        )
//
//        val updatedBoard = originalBoard.copy(
//            title = title,
//            content = content,
//            updatedAt = LocalDateTime.now()
//        )
//
//        `when`(boardService.getBoard(boardId)).thenReturn(originalBoard)
//        `when`(passwordUtil.matches(password, hashedPassword)).thenReturn(true)
//        `when`(boardService.updateBoard(boardId, title, content)).thenReturn(updatedBoard)
//
//        // when
//        val request = UpdateBoard.ExecuteRequest(
//            boardId = boardId,
//            title = title,
//            content = content,
//            password = password
//        )
//
//        val response = updateBoard.execute(request)
//
//        // then
//        assertEquals(boardId, response.id)
//        assertEquals(title, response.title)
//        assertEquals(content, response.content)
//
//        verify(boardService).getBoard(boardId)
//        verify(passwordUtil).matches(password, hashedPassword)
//        verify(boardService).updateBoard(boardId, title, content)
//        verify(eventPublisher).publishBoardUpdated(
//            board = updatedBoard,
//            userId = null
//        )
//    }
//
//    @Test
//    fun `회원 게시글 수정 - 권한 없음`() {
//        // given
//        val userId = "user123"
//        val differentUserId = "user456"
//        val boardId = 1L
//        val title = "업데이트된 제목"
//        val content = "업데이트된 내용"
//
//        val originalBoard = Board(
//            id = boardId,
//            title = "원래 제목",
//            content = "원래 내용",
//            author = "테스터",
//            userId = differentUserId,
//            password = null,
//            createdAt = LocalDateTime.now().minusDays(1),
//            updatedAt = LocalDateTime.now().minusDays(1)
//        )
//
//        `when`(boardService.getBoard(boardId)).thenReturn(originalBoard)
//        `when`(securityUtils.getCurrentUserId()).thenReturn(userId)
//        `when`(securityUtils.isAdmin()).thenReturn(false)
//
//        // when & then
//        val request = UpdateBoard.ExecuteRequest(
//            boardId = boardId,
//            title = title,
//            content = content,
//            password = null
//        )
//
//        assertFailsWith<AppException> {
//            updateBoard.execute(request)
//        }
//
//        verify(boardService).getBoard(boardId)
//        verify(securityUtils).getCurrentUserId()
//        verify(securityUtils).isAdmin()
//        verify(boardService, never()).updateBoard(any(), any(), any())
//        verify(eventPublisher, never()).publishBoardUpdated(any(), any(), any(), any())
//    }
//
//    @Test
//    fun `게스트 게시글 수정 - 비밀번호 불일치`() {
//        // given
//        val boardId = 1L
//        val title = "업데이트된 제목"
//        val content = "업데이트된 내용"
//        val password = "wrong_password"
//        val hashedPassword = "hashed_password"
//
//        val originalBoard = Board(
//            id = boardId,
//            title = "원래 제목",
//            content = "원래 내용",
//            author = "게스트",
//            userId = null,
//            password = hashedPassword,
//            createdAt = LocalDateTime.now().minusDays(1),
//            updatedAt = LocalDateTime.now().minusDays(1)
//        )
//
//        `when`(boardService.getBoard(boardId)).thenReturn(originalBoard)
//        `when`(passwordUtil.matches(password, hashedPassword)).thenReturn(false)
//
//        // when & then
//        val request = UpdateBoard.ExecuteRequest(
//            boardId = boardId,
//            title = title,
//            content = content,
//            password = password
//        )
//
//        assertFailsWith<AppException> {
//            updateBoard.execute(request)
//        }
//
//        verify(boardService).getBoard(boardId)
//        verify(passwordUtil).matches(password, hashedPassword)
//        verify(boardService, never()).updateBoard(any(), any(), any())
//        verify(eventPublisher, never()).publishBoardUpdated(any(), any(), any(), any())
//    }
// }
