// package com.example.jobstat.community_read.usecase.handler
//
// import com.example.jobstat.community_read.model.BoardReadModel
// import com.example.jobstat.community_read.service.CommunityReadService
// import com.example.jobstat.core.event.Event
// import com.example.jobstat.core.event.EventType
// import com.example.jobstat.core.event.payload.board.BoardUpdatedEventPayload
// import jakarta.validation.Validation
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.extension.ExtendWith
// import org.mockito.Mock
// import org.mockito.Mockito.*
// import org.mockito.junit.jupiter.MockitoExtension
// import org.springframework.test.context.ActiveProfiles
// import java.time.LocalDateTime
// import kotlin.test.assertEquals
// import kotlin.test.assertNotNull
// import kotlin.test.assertNull
//
// @ExtendWith(MockitoExtension::class)
// @ActiveProfiles("test")
// class HandleBoardUpdatedUseCaseTest {
//
//    @Mock
//    private lateinit var communityReadService: CommunityReadService
//
//    private lateinit var handleBoardUpdatedUseCase: HandleBoardUpdatedUseCase
//
//    private val validator = Validation.buildDefaultValidatorFactory().validator
//
//    @BeforeEach
//    fun setUp() {
//        handleBoardUpdatedUseCase = HandleBoardUpdatedUseCase(
//            communityReadService = communityReadService,
//            validator = validator
//        )
//    }
//
//    @Test
//    fun `게시글 업데이트 이벤트 처리 - 성공`() {
//        // given
//        val boardId = 1L
//        val title = "업데이트된 제목"
//        val content = "업데이트된 내용"
//        val author = "테스터"
//        val categoryId = 5L
//        val categoryName = "자유게시판"
//        val userId = "user123"
//
//        val originalBoard = BoardReadModel(
//            id = boardId,
//            title = "원래 제목",
//            content = "원래 내용",
//            author = author,
//            categoryId = categoryId,
//            categoryName = categoryName,
//            viewCount = 10,
//            likeCount = 5,
//            commentCount = 3,
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
//        val payload = BoardUpdatedEventPayload(
//            boardId = boardId,
//            title = title,
//            content = content,
//            author = author,
//            categoryId = categoryId,
//            categoryName = categoryName,
//            userId = userId
//        )
//
//        val event = Event(
//            eventId = "event123",
//            type = EventType.BOARD_UPDATED,
//            payload = payload,
//            aggregateId = boardId.toString(),
//            createdAt = System.currentTimeMillis()
//        )
//
//        `when`(communityReadService.getBoardById(boardId)).thenReturn(originalBoard)
//        `when`(communityReadService.saveBoardModel(any())).thenReturn(updatedBoard)
//
//        // when
//        handleBoardUpdatedUseCase.handle(event)
//
//        // then
//        verify(communityReadService).getBoardById(boardId)
//        verify(communityReadService).saveBoardModel(
//            argThat { board ->
//                board.id == boardId &&
//                    board.title == title &&
//                    board.content == content &&
//                    board.author == author &&
//                    board.categoryId == categoryId &&
//                    board.categoryName == categoryName
//            }
//        )
//    }
//
//    @Test
//    fun `게시글 업데이트 이벤트 처리 - 게시글이 존재하지 않는 경우`() {
//        // given
//        val boardId = 1L
//        val title = "업데이트된 제목"
//        val content = "업데이트된 내용"
//        val author = "테스터"
//        val categoryId = 5L
//        val categoryName = "자유게시판"
//        val userId = "user123"
//
//        val payload = BoardUpdatedEventPayload(
//            boardId = boardId,
//            title = title,
//            content = content,
//            author = author,
//            categoryId = categoryId,
//            categoryName = categoryName,
//            userId = userId
//        )
//
//        val event = Event(
//            eventId = "event123",
//            type = EventType.BOARD_UPDATED,
//            payload = payload,
//            aggregateId = boardId.toString(),
//            createdAt = System.currentTimeMillis()
//        )
//
//        `when`(communityReadService.getBoardById(boardId)).thenReturn(null)
//
//        // when
//        val response = handleBoardUpdatedUseCase.execute(
//            HandleBoardUpdatedUseCase.Request(
//                boardId = boardId,
//                title = title,
//                content = content,
//                author = author,
//                categoryId = categoryId,
//                categoryName = categoryName,
//                userId = userId
//            )
//        )
//
//        // then
//        verify(communityReadService).getBoardById(boardId)
//        verify(communityReadService, never()).saveBoardModel(any())
//        assertEquals(false, response.success)
//        assertNull(response.board)
//    }
//
//    @Test
//    fun `게시글 업데이트 이벤트 처리 - 일부 필드만 업데이트`() {
//        // given
//        val boardId = 1L
//        val title = "업데이트된 제목"
//        val author = "테스터"
//
//        val originalBoard = BoardReadModel(
//            id = boardId,
//            title = "원래 제목",
//            content = "원래 내용",
//            author = author,
//            categoryId = 5L,
//            categoryName = "자유게시판",
//            viewCount = 10,
//            likeCount = 5,
//            commentCount = 3,
//            createdAt = LocalDateTime.now().minusDays(1),
//            updatedAt = LocalDateTime.now().minusDays(1)
//        )
//
//        val updatedBoard = originalBoard.copy(
//            title = title,
//            updatedAt = LocalDateTime.now()
//        )
//
//        val payload = BoardUpdatedEventPayload(
//            boardId = boardId,
//            title = title,
//            content = null,
//            author = author,
//            categoryId = null,
//            categoryName = null,
//            userId = null
//        )
//
//        val event = Event(
//            eventId = "event123",
//            type = EventType.BOARD_UPDATED,
//            payload = payload,
//            aggregateId = boardId.toString(),
//            createdAt = System.currentTimeMillis()
//        )
//
//        `when`(communityReadService.getBoardById(boardId)).thenReturn(originalBoard)
//        `when`(communityReadService.saveBoardModel(any())).thenReturn(updatedBoard)
//
//        // when
//        val response = handleBoardUpdatedUseCase.execute(
//            HandleBoardUpdatedUseCase.Request(
//                boardId = boardId,
//                title = title,
//                content = null,
//                author = author,
//                categoryId = null,
//                categoryName = null,
//                userId = null
//            )
//        )
//
//        // then
//        verify(communityReadService).getBoardById(boardId)
//        verify(communityReadService).saveBoardModel(
//            argThat { board ->
//                board.id == boardId &&
//                    board.title == title &&
//                    board.content == originalBoard.content &&
//                    board.author == author &&
//                    board.categoryId == originalBoard.categoryId &&
//                    board.categoryName == originalBoard.categoryName
//            }
//        )
//        assertEquals(true, response.success)
//        assertNotNull(response.board)
//        assertEquals(title, response.board?.title)
//    }
// }
