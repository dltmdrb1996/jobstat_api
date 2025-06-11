package com.wildrew.jobstat.community_read.service

import com.wildrew.jobstat.community_read.model.CommentReadModel
import com.wildrew.jobstat.community_read.repository.BoardDetailRepository
import com.wildrew.jobstat.community_read.repository.CommentDetailRepository
import com.wildrew.jobstat.community_read.repository.CommunityEventUpdateRepository
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.board.item.BoardIncrementItem
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class CommunityEventHandlerVerLua(
    private val communityEventUpdateRepository: CommunityEventUpdateRepository,
    private val boardDetailRepository: BoardDetailRepository,
    private val commentDetailRepository: CommentDetailRepository,
    private val stringRedisTemplate: StringRedisTemplate,
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

    override fun handleBoardLiked(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload) {
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

    override fun handleBoardRankingUpdated(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload) {
        try {
            val success = communityEventUpdateRepository.applyBoardRankingUpdate(payload)
            logOutcome(success, "게시글 랭킹 업데이트", "metric=${payload.metric}, period=${payload.period}")
        } catch (e: Exception) {
            handleException(e, "게시글 랭킹 업데이트", "metric=${payload.metric}, period=${payload.period}")
        }
    }

    override fun handleCommentCreated(payload: com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload) {
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
                dataSerializer.serialize(CommentReadModel.fromPayload(payload)) ?: run {
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

    override fun handleBulkBoardIncrements(payload: BulkBoardIncrementsForReadPayload) {
        if (payload.items.isEmpty()) {
            log.info("캐시 업데이트할 증분 항목이 없습니다. batchId: {}", payload.batchId)
            return
        }

        log.debug("게시글 카운터 증분 정보 수신 (캐시 업데이트용). batchId: {}, 항목 수: {}", payload.batchId, payload.items.size)

        try {
            val results =
                stringRedisTemplate.executePipelined { connection ->
                    payload.items.forEach { item: BoardIncrementItem ->
                        val viewKey = "community:counter:view:${item.boardId}"
                        val likeKey = "community:counter:like:${item.boardId}"

                        if (item.viewIncrement != 0) {
                            log.debug("Pipelining INCRBY: key={}, delta={}", viewKey, item.viewIncrement)
                            connection.stringCommands().incrBy(viewKey.toByteArray(), item.viewIncrement.toLong())
                        }
                        if (item.likeIncrement != 0) {
                            log.debug("Pipelining INCRBY: key={}, delta={}", likeKey, item.likeIncrement)
                            connection.stringCommands().incrBy(likeKey.toByteArray(), item.likeIncrement.toLong())
                        }
                    }
                    null
                }

            log.info("Redis 캐시 카운터 증분 파이프라인 실행 완료. batchId: {}, 처리된 Redis 명령어 수 (추정): {}", payload.batchId, results?.size ?: 0)
        } catch (e: DataAccessException) {
            log.error(
                "Redis 캐시 카운터 증분 처리 중 DataAccessException 발생. batchId: {}. 메시지는 재시도될 수 있습니다.",
                payload.batchId,
                e,
            )
            throw e
        } catch (e: Exception) {
            log.error(
                "Redis 캐시 카운터 증분 처리 중 예상치 못한 오류 발생. batchId: {}.",
                payload.batchId,
                e,
            )
            throw e
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
