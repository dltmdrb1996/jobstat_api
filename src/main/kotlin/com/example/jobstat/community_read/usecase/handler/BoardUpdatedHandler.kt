package com.example.jobstat.community_read.usecase.handler

import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.navigator.EventHandler
import com.example.jobstat.core.event.payload.board.BoardUpdatedEventPayload
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 게시판 수정 이벤트 처리 유스케이스
 */
@Component
@EventHandler(eventType = EventType.BOARD_UPDATED)
class HandleBoardUpdatedUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<HandleBoardUpdatedUseCase.Request, HandleBoardUpdatedUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 이벤트 처리 요청 수신 및 유효성 검사
     */
    @Transactional
    fun handle(event: Event<out EventPayload>) {
        try {
            (event.payload as BoardUpdatedEventPayload).let { payload ->
                execute(
                    Request(
                        boardId = payload.boardId,
                        title = payload.title,
                        content = payload.content,
                        author = payload.author,
                        categoryId = payload.categoryId,
                        userId = payload.userId
                    )
                )
            }
        } catch (e: Exception) {
            log.error("[HandleBoardUpdatedUseCase] 게시글 수정 이벤트 처리 실패: {}", e.message)
            throw e
        }
    }
    
    /**
     * 유스케이스 실행
     */
    override fun execute(request: Request): Response = with(request) {
        try {
            communityReadService.getBoardById(boardId)?.let { existingBoard ->
                updateBoard(existingBoard, request)
                    .let { communityReadService.saveBoardModel(it) }
                    .let { Response(success = true, board = it) }
            } ?: Response(success = false, board = null)
        } catch (e: Exception) {
            log.error("[HandleBoardUpdatedUseCase] 게시글 수정 실패: boardId={}, error={}", boardId, e.message)
            Response(success = false, board = null)
        }
    }
    
    private fun updateBoard(existingBoard: BoardReadModel, request: Request): BoardReadModel = with(request) {
        existingBoard.copy(
            title = title ?: existingBoard.title,
            content = content ?: existingBoard.content,
            author = author ?: existingBoard.author,
            categoryId = categoryId ?: existingBoard.categoryId,
            userId = userId ?: existingBoard.userId,
            updatedAt = LocalDateTime.now()
        )
    }
    
    /**
     * 요청 데이터 클래스
     */
    data class Request(
        val boardId: Long,
        val title: String? = null,
        val content: String? = null,
        val author: String? = null,
        val categoryId: Long? = null,
        val userId: Long? = null
    )
    
    /**
     * 응답 데이터 클래스
     */
    data class Response(
        val success: Boolean,
        val board: BoardReadModel? = null
    )
} 