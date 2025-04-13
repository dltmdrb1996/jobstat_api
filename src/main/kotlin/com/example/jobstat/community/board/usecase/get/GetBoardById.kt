// file: src/main/kotlin/com/example/jobstat/community/board/usecase/get/GetBoardDetail.kt
package com.example.jobstat.community.board.usecase.get

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.core.global.extension.toEpochMilli
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.transaction.annotation.Transactional
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 게시글 상세 조회 유스케이스 (최적화 버전)
 * - DB 중복 호출 최적화 적용
 * - 영속성 컨텍스트 활용하여 성능 향상
 */
@Service
internal class GetBoardById(
    private val boardService: BoardService,
    private val counterService: CounterService,
    private val securityUtils: SecurityUtils,
    validator: Validator,
) : ValidUseCase<GetBoardById.Request, GetBoardById.Response>(validator) {

    @Transactional(readOnly = true)
    override fun invoke(request: Request): Response {
        return super.invoke(request)
    }

    override fun execute(request: Request): Response {
        val board = boardService.getBoard(request.boardId)
        val userId = securityUtils.getCurrentUserId()?.toString()

        val likeCount = counterService.getLikeCount(board.id, board.likeCount)
        val viewCount = counterService.getViewCount(boardId = board.id, dbViewCount = board.viewCount)
        val userLiked = userId?.let { counterService.hasUserLiked(board.id, it) } ?: false

        return Response.from(board, viewCount, likeCount, userLiked)
    }

    @Schema(
        name = "GetBoardDetailRequest",
        description = "게시글 상세 조회 요청 모델"
    )
    data class Request(
        @field:Schema(
            description = "조회할 게시글 ID",
            example = "1",
            required = true
        )
        val boardId: Long,
    )

    @Schema(
        name = "GetBoardDetailResponse",
        description = "게시글 상세 조회 응답 모델"
    )
    data class Response(
        @field:Schema(
            description = "게시글 ID",
            example = "1"
        )
        val id: String,

        @field:Schema(
            description = "작성자 ID",
            example = "100",
            nullable = true
        )
        val userId: Long?,

        @field:Schema(
            description = "카테고리 ID",
            example = "2"
        )
        val categoryId: Long,

        @field:Schema(
            description = "게시글 제목",
            example = "안녕하세요, 첫 게시글입니다"
        )
        val title: String,

        @field:Schema(
            description = "게시글 내용",
            example = "게시글 내용입니다. 반갑습니다!"
        )
        val content: String,

        @field:Schema(
            description = "작성자",
            example = "홍길동"
        )
        val author: String,

        @field:Schema(
            description = "조회수",
            example = "42",
            minimum = "0"
        )
        val viewCount: Int,

        @field:Schema(
            description = "좋아요 수",
            example = "15",
            minimum = "0"
        )
        val likeCount: Int,

        @field:Schema(
            description = "댓글 수",
            example = "7",
            minimum = "0"
        )
        val commentCount: Int,

        @field:Schema(
            description = "현재 사용자의 좋아요 여부",
            example = "false"
        )
        val userLiked: Boolean,

        @field:Schema(
            description = "생성 일시",
            example = "2023-05-10T14:30:15.123456"
        )
        val createdAt: LocalDateTime,

        @field:Schema(
            description = "이벤트 타임스탬프 (밀리초)",
            example = "1683727815123"
        )
        val eventTs: Long,
    ) {
        companion object {
            fun from(
                board: Board,
                viewCount: Int,
                likeCount: Int,
                userLiked: Boolean,
            ): Response = with(board) {
                Response(
                    id = id.toString(),
                    title = title,
                    userId = userId,
                    content = content,
                    author = author,
                    viewCount = viewCount, // 서비스에서 계산된 값 사용
                    likeCount = likeCount, // 서비스에서 계산된 값 사용
                    commentCount = commentCount, // Board 엔티티의 값 사용 (필요시 CounterService에서 가져오도록 수정 가능)
                    userLiked = userLiked, // 서비스에서 계산된 값 사용
                    categoryId = category.id,
                    createdAt = createdAt,
                    eventTs = updatedAt.toEpochMilli(),
                )
            }
        }
    }
}