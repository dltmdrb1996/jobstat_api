package com.example.jobstat.community.board.usecase.command

import com.example.jobstat.community.CommunityEventPublisher
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.extension.toEpochMilli
import com.example.jobstat.core.usecase.impl.ValidUseCase
import com.example.jobstat.core.global.utils.SecurityUtils
import jakarta.validation.Validator
import jakarta.validation.constraints.Positive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
    private val communityEventPublisher: CommunityEventPublisher,
    validator: Validator,
) : ValidUseCase<UnlikeBoard.Request, UnlikeBoard.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 좋아요 취소 요청 처리
     */
    override fun execute(request: Request): Response {
        log.info("게시글 좋아요 취소 요청: boardId={}", request.boardId)

        // 사용자 인증 정보 확인
        val userId = getUserIdOrThrow()
        val userIdStr = userId.toString()

        // 게시글 존재 여부 확인 및 조회 - DB에서 한 번만 조회
        val board = validateBoardExists(request.boardId)

        // 엔티티에서 DB 값 직접 전달
        val likeCount = counterService.decrementLikeCount(
            boardId = board.id,
            userId = userIdStr,
            dbLikeCount = board.likeCount
        )

        // 5) ReadModel 이벤트 발행
        communityEventPublisher.publishBoardLiked(
            boardId = board.id,
            createdAt = board.createdAt,
            userId = userId,
            eventTs = board.updatedAt.toEpochMilli(),
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
    data class Request(
        @field:Positive(message = "게시글 ID는 양수여야 합니다")
        val boardId: Long,
    )

    /**
     * 응답 모델
     */
    data class Response(
        val likeCount: Int,
    )
}