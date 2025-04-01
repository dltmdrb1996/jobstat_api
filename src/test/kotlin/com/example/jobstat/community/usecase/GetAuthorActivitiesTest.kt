package com.example.jobstat.community.usecase

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.service.BoardServiceImpl
import com.example.jobstat.community.board.usecase.fetch.GetAuthorActivities
import com.example.jobstat.comment.service.CommentService
import com.example.jobstat.comment.service.CommentServiceImpl
import com.example.jobstat.community.fake.CategoryFixture
import com.example.jobstat.community.fake.repository.FakeBoardRepository
import com.example.jobstat.community.fake.repository.FakeCategoryRepository
import com.example.jobstat.community.fake.repository.FakeCommentRepository
import jakarta.validation.Validation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

@DisplayName("GetAuthorActivities Usecase 테스트")
class GetAuthorActivitiesTest {
    private lateinit var boardRepository: FakeBoardRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var commentRepository: FakeCommentRepository
    private lateinit var boardService: BoardService
    private lateinit var commentService: CommentService
    private lateinit var getAuthorActivities: GetAuthorActivities

    @BeforeEach
    fun setUp() {
        boardRepository = FakeBoardRepository()
        categoryRepository = FakeCategoryRepository()
        commentRepository = FakeCommentRepository()
        boardService = BoardServiceImpl(boardRepository, categoryRepository)
        commentService = CommentServiceImpl(commentRepository, boardRepository)
        getAuthorActivities = GetAuthorActivities(boardService, commentService, Validation.buildDefaultValidatorFactory().validator)

        // 중복 저장 방지를 위해 카테고리를 한 번만 생성 및 저장
        val savedCategory =
            categoryRepository.save(
                CategoryFixture.aCategory().withName("test").create(),
            )

        // 작성자 "author1"의 게시글 3개 생성 (모두 동일한 카테고리 사용)
        repeat(3) { idx ->
            boardService.createBoard(
                title = "Title $idx",
                content = "Content $idx",
                author = "author1",
                categoryId = savedCategory.id,
                password = null,
                userId = null,
            )
        }

        // 댓글 2개 생성
        val firstBoardId =
            boardRepository
                .findAll(PageRequest.of(0, 10))
                .content
                .first()
                .id
        repeat(2) { idx ->
            commentService.createComment(
                boardId = firstBoardId,
                content = "Comment $idx",
                author = "author1",
                password = null,
                userId = null,
            )
        }
    }

    @Test
    @DisplayName("작성자의 활동 내역 조회에 성공한다")
    fun retrieveAuthorActivitiesSuccessfully() {
        val request = GetAuthorActivities.Request(author = "author1", page = 0)
        val response = getAuthorActivities(request)
        // 게시글 3개, 댓글 2개
        assertEquals(3, response.boards.totalElements)
        assertEquals(2, response.comments.totalElements)
    }
}
