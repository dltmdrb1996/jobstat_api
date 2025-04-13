package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 게시글 좋아요 취소 유스케이스 (최적화 버전)
 * - DB 중복 호출 최적화 적용
 * - 영속성 컨텍스트 활용하여 성능 향상
 */
@Service
internal class UnlikeBoard(
    private val counterService: CounterService,
    private val securityUtils: SecurityUtils,
    private val boardService: BoardService,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
    validator: Validator,
) : ValidUseCase<UnlikeBoard.Request, UnlikeBoard.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional
    override fun invoke(request: Request): Response {
        return super.invoke(request)
    }

    override fun execute(request: Request): Response {
        log.info("게시글 좋아요 취소 요청: boardId={}", request.boardId)

        // 사용자 인증 정보 확인
        val userId = getUserIdOrThrow()
        val userIdStr = userId.toString()
        val boardId = request.boardId

        // 게시글 존재 여부 확인 및 조회 - DB에서 한 번만 조회
        val board = validateBoardExists(boardId)

        // 엔티티에서 DB 값 직접 전달
        val likeCount = counterService.decrementLikeCount(
            boardId = board.id,
            userId = userIdStr,
            dbLikeCount = board.likeCount
        )

        // 5) ReadModel 이벤트 발행
        communityCommandEventPublisher.publishBoardLiked(
            boardId = boardId,
            createdAt = board.createdAt,
            userId = userId,
            likeCount = likeCount
        )

        log.info("게시글 좋아요 취소 성공: boardId={}, userId={}, likeCount={}",
            request.boardId, userId, likeCount)

        return Response(likeCount = likeCount)
    }

    /**
     * 사용자 ID 확인
     */
    private fun getUserIdOrThrow(): Long =
        securityUtils.getCurrentUserId()
            ?: throw AppException.fromErrorCode(
                ErrorCode.AUTHENTICATION_FAILURE,
                "좋아요 취소를 위해서는 로그인이 필요합니다"
            )

    /**
     * 게시글 존재 여부 확인 및 엔티티 반환
     */
    private fun validateBoardExists(boardId: Long) = try {
        boardService.getBoard(boardId)
    } catch (e: Exception) {
        throw AppException.fromErrorCode(
            ErrorCode.RESOURCE_NOT_FOUND,
            "좋아요를 취소할 게시글을 찾을 수 없습니다",
            "boardId: $boardId"
        )
    }

    /**
     * 요청 모델
     */
    @Schema(
        name = "UnlikeBoardRequest",
        description = "게시글 좋아요 취소 요청 모델"
    )
    data class Request(
        @field:Schema(
            description = "좋아요 취소 대상 게시글 ID", 
            example = "1",
            required = true
        )
        val boardId: Long,
    )

    /**
     * 응답 모델
     */
    @Schema(
        name = "UnlikeBoardResponse",
        description = "게시글 좋아요 취소 응답 모델"
    )
    data class Response(
        @field:Schema(
            description = "좋아요 취소 후 총 좋아요 수", 
            example = "41",
            minimum = "0"
        )
        val likeCount: Int,
    )
}