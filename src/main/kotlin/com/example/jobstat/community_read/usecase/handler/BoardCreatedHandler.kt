package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.payload.board.BoardCreatedEventPayload
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 게시판 생성 이벤트 핸들러
 */
@Component
class BoardCreatedHandler(
    private val communityReadService: CommunityReadService
) : EventHandlingUseCase<EventType, BoardCreatedEventPayload, BoardCreatedHandler.Response>() {

    /**
     * 이 핸들러가 처리하는 이벤트 타입
     */
    override val eventType: EventType = EventType.BOARD_CREATED

    /**
     * 페이로드 유효성 검사
     */
    override fun validatePayload(payload: BoardCreatedEventPayload) {
        require(payload.boardId > 0) { "게시글 ID는 0보다 커야 합니다" }
    }

    /**
     * 비즈니스 로직 실행
     */
    @Transactional
    override fun execute(payload: BoardCreatedEventPayload): Response {
        val boardReadModel = createBoardReadModel(payload)
        communityReadService.saveBoardModel(boardReadModel)
        return Response(success = true)
    }

    private fun createBoardReadModel(payload: BoardCreatedEventPayload): BoardReadModel {
        return BoardReadModel(
            id = payload.boardId,
            title = payload.title,
            content = payload.content,
            author = payload.author,
            categoryId = payload.categoryId,
            viewCount = 0,
            likeCount = 0,
            commentCount = 0,
            createdAt = payload.createdAt,
            updatedAt = payload.createdAt,
            isDeleted = false
        )
    }

    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean
    )
}