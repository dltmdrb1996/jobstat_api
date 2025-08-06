package com.wildrew.jobstat.community.board.usecase.command

import com.wildrew.jobstat.community.board.service.BoardService
import com.wildrew.jobstat.community.counting.CounterService
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.wildrew.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UnlikeBoard(
    private val counterService: CounterService,
    private val theadContextUtils: TheadContextUtils,
    private val boardService: BoardService,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<UnlikeBoard.Request, UnlikeBoard.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response {
        log.debug("게시글 좋아요 취소 요청: boardId={}", request.boardId)

        val userId = getUserIdOrThrow()
        val userIdStr = userId.toString()
        val boardId = request.boardId

        val board = validateBoardExists(boardId)

        val likeCount =
            counterService.decrementLikeCount(
                boardId = board.id,
                userId = userIdStr,
                dbLikeCount = board.likeCount,
            )

        // 읽기 모델 이벤트 발행
        communityCommandEventPublisher.publishBoardLiked(
            boardId = boardId,
            createdAt = board.createdAt,
            userId = userId,
            likeCount = likeCount,
        )

        log.debug(
            "게시글 좋아요 취소 성공: boardId={}, userId={}, likeCount={}",
            request.boardId,
            userId,
            likeCount,
        )

        return Response(likeCount = likeCount)
    }

    private fun getUserIdOrThrow(): Long = theadContextUtils.getCurrentUserIdOrFail()

    private fun validateBoardExists(boardId: Long) =
        try {
            boardService.getBoard(boardId)
        } catch (e: Exception) {
            throw AppException.fromErrorCode(
                ErrorCode.RESOURCE_NOT_FOUND,
                "좋아요를 취소할 게시글을 찾을 수 없습니다",
                "boardId: $boardId",
            )
        }

    @Schema(
        name = "UnlikeBoardRequest",
        description = "게시글 좋아요 취소 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "좋아요 취소 대상 게시글 ID",
            example = "1",
            required = true,
        )
        val boardId: Long,
    )

    @Schema(
        name = "UnlikeBoardResponse",
        description = "게시글 좋아요 취소 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "좋아요 취소 후 총 좋아요 수",
            example = "41",
            minimum = "0",
        )
        val likeCount: Int,
    )
}
