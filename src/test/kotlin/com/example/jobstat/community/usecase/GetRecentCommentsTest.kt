package com.example.jobstat.community.usecase

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.comment.service.CommentService
import com.example.jobstat.comment.service.CommentServiceImpl
import com.example.jobstat.comment.usecase.GetRecentComments
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("GetRecentComments Usecase 테스트")
class GetRecentCommentsTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService
    private lateinit var getRecentComments: GetRecentComments
    private var testBoardId: Long = 0L

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        getRecentComments = GetRecentComments(commentService, Validation.buildDefaultValidatorFactory().validator)

        val category =
            CategoryFixture
                .aCategory()
                .withName("CAT1")
                .withDisplayName("Cat1")
                .withDescription("Desc")
                .create()
        categoryRepository.save(category)
        testBoardId =
            boardService
                .createBoard(
                    title = "Board for Comments",
                    content = "Content",
                    author = "boardAuthor",
                    categoryId = category.id,
                    password = null,
                ).id

        // 10 댓글 생성
        repeat(10) { idx ->
            commentService.createComment(
                boardId = testBoardId,
                content = "Comment $idx",
                author = "commenter",
                password = null,
                userId = null,
            )
        }
    }

    @Test
    @DisplayName("최근 댓글 5개가 정상적으로 반환된다")
    fun retrieveLatestFiveComments() {
        val response = getRecentComments(GetRecentComments.Request(boardId = testBoardId))
        assertEquals(5, response.items.size)
        // 가장 최근 댓글에는 "Comment 9"가 포함되어 있어야 함
        assert(
            response.items
                .first()
                .content
                .contains("9"),
        )
    }
}
