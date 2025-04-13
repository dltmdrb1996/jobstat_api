//package com.example.jobstat.community.usecase
//
//import com.example.jobstat.community.board.service.BoardService
//import com.example.jobstat.community.board.service.BoardServiceImpl
//import com.example.jobstat.community.board.usecase.get.GetBoardDetail
//import com.example.jobstat.comment.service.CommentService
//import com.example.jobstat.comment.service.CommentServiceImpl
//import com.example.jobstat.community.fake.BoardFixture
//import com.example.jobstat.community.fake.repository.FakeBoardRepository
//import com.example.jobstat.community.fake.repository.FakeCategoryRepository
//import com.example.jobstat.community.fake.repository.FakeCommentRepository
//import jakarta.validation.Validation
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull
//
//@DisplayName("GetBoardDetail Usecase 테스트")
//class GetBoardDetailByIdTest {
//    private lateinit var boardRepository: FakeBoardRepository
//    private lateinit var categoryRepository: FakeCategoryRepository
//    private lateinit var commentRepository: FakeCommentRepository
//    private lateinit var boardService: BoardService
//    private lateinit var commentService: CommentService
//    private lateinit var getBoardDetail: GetBoardDetail
//    private var testBoardId: Long = 0L
//
//    @BeforeEach
//    fun setUp() {
//        boardRepository = FakeBoardRepository()
//        categoryRepository = FakeCategoryRepository()
//        commentRepository = FakeCommentRepository()
//        boardService = BoardServiceImpl(boardRepository, categoryRepository)
//        commentService = CommentServiceImpl(commentRepository, boardRepository)
//        getBoardDetail = GetBoardDetail(boardService, commentService, Validation.buildDefaultValidatorFactory().validator)
//
//        // 게시글 생성
//        val board = BoardFixture.aBoard().create()
//        testBoardId =
//            boardService
//                .createBoard(
//                    title = board.title,
//                    content = board.content,
//                    author = board.author,
//                    categoryId =
//                        categoryRepository
//                            .save(
//                                com.example.jobstat.community.fake.CategoryFixture
//                                    .aCategory()
//                                    .withName("CAT1")
//                                    .withDisplayName("Cat1")
//                                    .withDescription("Desc")
//                                    .create(),
//                            ).id,
//                    password = board.password,
//                ).id
//
//        // 댓글 3개 추가
//        repeat(3) { idx ->
//            commentService.createComment(
//                boardId = testBoardId,
//                content = "댓글 $idx",
//                author = "댓글작성자",
//                password = null,
//                userId = null,
//            )
//        }
//    }
//
//    @Test
//    @DisplayName("게시글 상세 조회시 조회수가 증가하고 댓글이 함께 조회된다")
//    fun retrieveBoardDetailWithViewCountIncreaseAndComments() {
//        val request = GetBoardDetail.Request(boardId = testBoardId, commentPage = 0)
//        val response = getBoardDetail(request)
//        assertEquals(testBoardId, response.id)
//        assertNotNull(response.title)
//        // 조회수는 1 증가되었어야 함
//        assertEquals(1, response.viewCount)
//        // 댓글 페이지의 totalElements는 3
//        assertEquals(3, response.commentTotalCount)
//    }
//}
