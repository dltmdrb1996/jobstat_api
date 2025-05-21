package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_security.util.context_util.TheadContextUtils
import com.example.jobstat.core.core_usecase.base.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class LikeBoard(
    private val counterService: CounterService,
    private val theadContextUtils: TheadContextUtils,
    private val boardService: BoardService,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<LikeBoard.Request, LikeBoard.Response>(validator) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: Request): Response = super.invoke(request)

    override fun execute(request: Request): Response {
        log.debug("게시글 좋아요 요청: boardId={}", request.boardId)

        val userId = getUserIdOrThrow()
        val userIdStr = userId.toString()
        val boardId = request.boardId

        val board = validateBoardExists(boardId)

        val likeCount =
            counterService.incrementLikeCount(
                boardId = boardId,
                userId = userIdStr,
                dbLikeCount = board.likeCount,
            )

        communityCommandEventPublisher.publishBoardLiked(
            boardId = board.id,
            createdAt = board.createdAt,
            userId = userId,
            likeCount = likeCount,
        )

        log.debug(
            "게시글 좋아요 성공: boardId={}, userId={}, likeCount={}",
            request.boardId,
            userId,
            likeCount,
        )

        return Response(likeCount = likeCount)
    }

    private fun getUserIdOrThrow(): Long =
        theadContextUtils.getCurrentUserId()
            ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "좋아요를 누르려면 로그인이 필요합니다",
            )

    private fun validateBoardExists(boardId: Long) =
        try {
            boardService.getBoard(boardId)
        } catch (e: Exception) {
            throw AppException.fromErrorCode(
                ErrorCode.RESOURCE_NOT_FOUND,
                "좋아요를 누를 게시글을 찾을 수 없습니다",
                "boardId: $boardId",
            )
        }

    @Schema(
        name = "LikeBoardRequest",
        description = "게시글 좋아요 요청 모델",
    )
    data class Request(
        @field:Schema(
            description = "좋아요 대상 게시글 ID",
            example = "1",
            required = true,
        )
        val boardId: Long,
    )

    @Schema(
        name = "LikeBoardResponse",
        description = "게시글 좋아요 응답 모델",
    )
    data class Response(
        @field:Schema(
            description = "좋아요 처리 후 총 좋아요 수",
            example = "42",
            minimum = "0",
        )
        val likeCount: Int,
    )
}
