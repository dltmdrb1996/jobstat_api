// package com.example.jobstat.community.usecase
//
// import com.example.jobstat.community.board.service.BoardService
// import com.example.jobstat.community.board.service.BoardServiceImpl
// import com.example.jobstat.community.board.usecase.command.LikeBoard
// import com.example.jobstat.community.fake.repository.FakeBoardRepository
// import com.example.jobstat.community.fake.repository.FakeCategoryRepository
// import com.example.jobstat.community.fake.repository.FakeCommentRepository
// import jakarta.validation.Validation
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.DisplayName
// import org.junit.jupiter.api.Test
// import kotlin.test.assertEquals
//
// @DisplayName("LikeBoard Usecase 테스트")
// class LikeBoardTest {
//    private lateinit var boardRepository: FakeBoardRepository
//    private lateinit var categoryRepository: FakeCategoryRepository
//    private lateinit var commentRepository: FakeCommentRepository
//    private lateinit var boardService: BoardService
//    private lateinit var likeBoard: LikeBoard
//    private var testBoardId: Long = 0L
//
//    @BeforeEach
//    fun setUp() {
//        boardRepository = FakeBoardRepository()
//        categoryRepository = FakeCategoryRepository()
//        commentRepository = FakeCommentRepository()
//        boardService = BoardServiceImpl(boardRepository, categoryRepository)
//        likeBoard = LikeBoard(boardService, Validation.buildDefaultValidatorFactory().validator)
//
//        // 카테고리 인스턴스 생성 후 반드시 저장하여 올바른 id가 할당되도록 함
//        val category =
//            com.example.jobstat.community.fake.CategoryFixture
//                .aCategory()
//                .withName("CAT1")
//                .withDisplayName("Cat1")
//                .withDescription("Desc")
//                .create()
//        val savedCategory = categoryRepository.save(category) // 저장 후 할당된 id 사용
//
//        testBoardId = boardService.createBoard("Like Title", "Like Content", "user", savedCategory.id, null).id
//    }
//
//    @Test
//    @DisplayName("게시글 좋아요 수가 증가한다")
//    fun increaseBoardLikeCount() {
//        val request = LikeBoard.Request(boardId = testBoardId)
//        val response = likeBoard(request)
//        assertEquals(1, response.likeCount)
//    }
// }
