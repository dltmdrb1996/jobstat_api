package com.example.jobstat.community_read.service

import com.example.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload
import com.example.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.example.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import com.example.jobstat.community_read.repository.BoardDetailRepository // Read 용도로 필요
import com.example.jobstat.community_read.repository.CommentDetailRepository // Read 용도로 필요
import com.example.jobstat.community_read.repository.CommunityEventUpdateRepository // 신규 의존성
import com.example.jobstat.core.core_error.model.AppException
import com.example.jobstat.core.core_error.model.ErrorCode
import com.example.jobstat.core.core_event.model.payload.board.*
import com.example.jobstat.core.core_serializer.DataSerializer // 직렬화기 필요
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommunityEventHandlerVerLua(
    private val communityEventUpdateRepository: CommunityEventUpdateRepository,
    private val boardDetailRepository: BoardDetailRepository,
    private val commentDetailRepository: CommentDetailRepository,
    private val dataSerializer: DataSerializer,
) : CommunityEventHandler {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun handleBoardCreated(payload: BoardCreatedEventPayload) {
        try {
            val success = communityEventUpdateRepository.applyBoardCreation(payload)
            logOutcome(success, "게시글 생성", "boardId=${payload.boardId}")
        } catch (e: Exception) {
            handleException(e, "게시글 생성", "boardId=${payload.boardId}")
        }
    }

    override fun handleBoardUpdated(payload: BoardUpdatedEventPayload) {
        try {
            val currentBoard =
                boardDetailRepository.findBoardDetail(payload.boardId) ?: run {
                    log.warn("게시글 수정 처리 실패: 원본 데이터 없음. boardId=${payload.boardId}")
                    return
                }
            val updatedBoard =
                currentBoard.copy(
                    title = payload.title,
                    content = payload.content,
                    eventTs = payload.eventTs,
                )
            val updatedBoardJson =
                dataSerializer.serialize(updatedBoard) ?: run {
                    log.error("게시글 수정 데이터 직렬화 실패: boardId={}", payload.boardId)
                    return
                }
            val success = communityEventUpdateRepository.applyBoardUpdate(payload, updatedBoardJson)
            logOutcome(success, "게시글 수정", "boardId=${payload.boardId}")
        } catch (e: Exception) {
            handleException(e, "게시글 수정", "boardId=${payload.boardId}")
        }
    }

    override fun handleBoardDeleted(payload: BoardDeletedEventPayload) {
        try {
            val success = communityEventUpdateRepository.applyBoardDeletion(payload)
            logOutcome(success, "게시글 삭제", "boardId=${payload.boardId}")
        } catch (e: Exception) {
            handleException(e, "게시글 삭제", "boardId=${payload.boardId}")
        }
    }

    override fun handleBoardLiked(payload: BoardLikedEventPayload) {
        try {
            val currentBoard =
                boardDetailRepository.findBoardDetail(payload.boardId) ?: run {
                    log.warn("게시글 좋아요 처리 실패: 원본 데이터 없음. boardId=${payload.boardId}")
                    return
                }
            val updatedBoard = currentBoard.copy(likeCount = payload.likeCount, eventTs = payload.eventTs)
            val updatedBoardJson =
                dataSerializer.serialize(updatedBoard) ?: run {
                    log.error("게시글 좋아요 데이터 직렬화 실패: boardId={}", payload.boardId)
                    return
                }
            val success = communityEventUpdateRepository.applyBoardLikeUpdate(payload, updatedBoardJson)
            logOutcome(success, "게시글 좋아요 업데이트", "boardId=${payload.boardId}, likeCount=${payload.likeCount}")
        } catch (e: Exception) {
            handleException(e, "게시글 좋아요 업데이트", "boardId=${payload.boardId}")
        }
    }

    override fun handleBoardViewed(payload: BoardViewedEventPayload) {
        try {
            val currentBoard =
                boardDetailRepository.findBoardDetail(payload.boardId) ?: run {
                    log.warn("게시글 조회수 처리 실패: 원본 데이터 없음. boardId=${payload.boardId}")
                    return
                }
            val updatedBoard = currentBoard.copy(viewCount = payload.viewCount, eventTs = payload.eventTs)
            val updatedBoardJson =
                dataSerializer.serialize(updatedBoard) ?: run {
                    log.error("게시글 조회수 데이터 직렬화 실패: boardId={}", payload.boardId)
                    return
                }
            val success = communityEventUpdateRepository.applyBoardViewUpdate(payload, updatedBoardJson)
            logOutcome(success, "게시글 조회수 업데이트", "boardId=${payload.boardId}, viewCount=${payload.viewCount}")
        } catch (e: Exception) {
            handleException(e, "게시글 조회수 업데이트", "boardId=${payload.boardId}")
        }
    }

    override fun handleBoardRankingUpdated(payload: BoardRankingUpdatedEventPayload) {
        try {
            val success = communityEventUpdateRepository.applyBoardRankingUpdate(payload)
            logOutcome(success, "게시글 랭킹 업데이트", "metric=${payload.metric}, period=${payload.period}")
        } catch (e: Exception) {
            handleException(e, "게시글 랭킹 업데이트", "metric=${payload.metric}, period=${payload.period}")
        }
    }

    override fun handleCommentCreated(payload: CommentCreatedEventPayload) {
        try {
            val currentBoard = boardDetailRepository.findBoardDetail(payload.boardId)
            val updatedBoardJson: String? =
                if (currentBoard != null) {
                    dataSerializer.serialize(
                        currentBoard.copy(
                            commentCount = currentBoard.commentCount + 1,
                            eventTs = payload.eventTs,
                        ),
                    )
                } else {
                    log.warn("댓글 생성 시 게시글 정보 업데이트 실패: 원본 게시글 없음. boardId=${payload.boardId}")
                    null
                }
            val commentJson =
                dataSerializer.serialize(payload.toReadModel()) ?: run {
                    log.error("댓글 생성 데이터 직렬화 실패: commentId={}", payload.commentId)
                    return
                }
            val success = communityEventUpdateRepository.applyCommentCreation(payload, commentJson, updatedBoardJson)
            logOutcome(success, "댓글 생성", "commentId=${payload.commentId}, boardId=${payload.boardId}")
        } catch (e: Exception) {
            handleException(e, "댓글 생성", "commentId=${payload.commentId}")
        }
    }

    override fun handleCommentUpdated(payload: CommentUpdatedEventPayload) {
        try {
            val currentComment =
                commentDetailRepository.findCommentDetail(payload.commentId) ?: run {
                    log.warn("댓글 수정 처리 실패: 원본 댓글 없음. commentId=${payload.commentId}")
                    return
                }
            val updatedComment =
                currentComment.copy(
                    content = payload.content,
                    updatedAt = payload.updatedAt,
                    eventTs = payload.eventTs,
                )
            val updatedCommentJson =
                dataSerializer.serialize(updatedComment) ?: run {
                    log.error("댓글 수정 데이터 직렬화 실패: commentId={}", payload.commentId)
                    return
                }
            val success = communityEventUpdateRepository.applyCommentUpdate(payload, updatedCommentJson)
            logOutcome(success, "댓글 수정", "commentId=${payload.commentId}")
        } catch (e: Exception) {
            handleException(e, "댓글 수정", "commentId=${payload.commentId}")
        }
    }

    override fun handleCommentDeleted(payload: CommentDeletedEventPayload) {
        try {
            val currentBoard = boardDetailRepository.findBoardDetail(payload.boardId)
            val updatedBoardJson: String? =
                if (currentBoard != null) {
                    dataSerializer.serialize(
                        currentBoard.copy(
                            commentCount = (currentBoard.commentCount - 1).coerceAtLeast(0),
                            eventTs = payload.eventTs,
                        ),
                    )
                } else {
                    log.warn("댓글 삭제 시 게시글 정보 업데이트 실패: 원본 게시글 없음. boardId=${payload.boardId}")
                    null
                }
            val success = communityEventUpdateRepository.applyCommentDeletion(payload, updatedBoardJson)
            logOutcome(success, "댓글 삭제", "commentId=${payload.commentId}, boardId=${payload.boardId}")
        } catch (e: Exception) {
            handleException(e, "댓글 삭제", "commentId=${payload.commentId}")
        }
    }

    private fun logOutcome(
        success: Boolean,
        operation: String,
        context: String,
    ) {
        if (success) {
            log.debug("$operation 처리 완료 (Lua): $context")
        } else {
            log.debug("$operation 처리 건너뜀 - 오래된 이벤트 (Lua): $context")
        }
    }

    private fun handleException(
        e: Exception,
        operation: String,
        context: String,
    ) {
        if (e is AppException && e.errorCode == ErrorCode.REDIS_OPERATION_FAILED) {
            log.error("$operation 처리 중 Redis 오류 발생 (Lua): $context")
        } else {
            log.error("$operation 처리 중 예상치 못한 오류 발생: $context", e)
        }

        throw e
    }
}
