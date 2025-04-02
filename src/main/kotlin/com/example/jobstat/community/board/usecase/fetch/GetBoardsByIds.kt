package com.example.jobstat.community.board.usecase.fetch

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.service.BoardService
import com.example.jobstat.community.board.usecase.fetch.GetBoardDetail.DetailCommentResponse
import com.example.jobstat.community.counting.CounterService
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.extension.toEpochMilli
import com.example.jobstat.core.global.utils.SecurityUtils
import com.example.jobstat.core.usecase.impl.ValidUseCase
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
internal class GetBoardsByIds(
    private val securityUtils: SecurityUtils,
    private val boardService: BoardService,
    private val counterService: CounterService,
    validator: Validator,
) : ValidUseCase<GetBoardsByIds.Request, GetBoardsByIds.Response>(validator) {

    @Transactional(readOnly = true)
    override fun execute(request: Request): Response {
        val userId = securityUtils.getCurrentUserId()?.toString()
        val boards = boardService.getBoardsByIds(request.boardIds)

        if (boards.isEmpty()) {
            return Response(emptyList())
        }

        val boardIdsWithCounts = boards.map { board ->
            Triple(board.id, board.viewCount, board.likeCount)
        }
        val countersMap = counterService.getBulkBoardCounters(boardIdsWithCounts, userId)
            .associateBy { it.boardId }
        val boardItems = boards.map { board ->
            val counters = countersMap[board.id] ?: throw AppException.fromErrorCode(ErrorCode.REDIS_OPERATION_FAILED)
            mapToBoardItem(
                board,
                viewCount = counters.viewCount,
                likeCount = counters.likeCount,
                userLiked = counters.userLiked
            )
        }

        return Response(boardItems)
    }

    private fun mapToBoardItem(board: Board, viewCount: Int, likeCount: Int, userLiked: Boolean): BoardItem = with(board) {
        BoardItem(
            id = id,
            userId = userId,
            title = title,
            content = content,
            author = author,
            viewCount = viewCount,
            likeCount = likeCount,
            commentCount = commentCount,
            categoryId = category.id,
            createdAt = createdAt,
            userLiked = userLiked,
            eventTs = updatedAt.toEpochMilli()
        )
    }

    data class Request(
        @field:NotEmpty(message = "게시판 ID 목록은 비어있을 수 없습니다") 
        val boardIds: List<Long>
    )

    data class Response(
        val boards: List<BoardItem>
    )

    data class BoardItem(
        val id: Long,
        val userId: Long?,
        val categoryId: Long,
        val title: String,
        val content: String,
        val author: String,
        val viewCount: Int,
        val likeCount: Int,
        val commentCount: Int,
        val userLiked: Boolean,
        val createdAt: LocalDateTime,
        val eventTs: Long,
    )
} 