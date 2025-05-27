package com.wildrew.jobstat.community_read.community_read.repository.fake

import com.wildrew.jobstat.community_read.model.BoardReadModel
import com.wildrew.jobstat.community_read.model.CommentReadModel
import com.wildrew.jobstat.community_read.repository.*
import com.wildrew.jobstat.community_read.repository.impl.*
import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class FakeCommunityEventUpdateRepository(
    private val boardDetailRepository: FakeBoardDetailRepository,
    private val boardIdListRepository: FakeBoardIdListRepository,
    private val boardCountRepository: FakeBoardCountRepository,
    private val commentDetailRepository: FakeCommentDetailRepository,
    private val commentIdListRepository: FakeCommentIdListRepository,
    private val commentCountRepository: FakeCommentCountRepository,
    private val dataSerializer: DataSerializer,
    private val eventTsTtlDays: Long = 1L,
) : CommunityEventUpdateRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    val eventTimestamps = ConcurrentHashMap<String, Long>()

    private fun getCurrentEventTs(key: String): Long = eventTimestamps[key] ?: 0L

    private fun checkAndSetTs(
        key: String,
        incomingEventTs: Long,
    ): Boolean {
        val currentTs = getCurrentEventTs(key)
        if (currentTs >= incomingEventTs) {
            log.debug("[Fake] '{}' 키에 대한 이벤트를 건너뜁니다. 타임스탬프가 더 오래됨 (현재: {}, 수신: {})", key, currentTs, incomingEventTs)
            return false
        }
        eventTimestamps[key] = incomingEventTs
        log.debug("[Fake] '{}' 키의 타임스탬프를 {}로 업데이트합니다", key, incomingEventTs)
        return true
    }

    private fun boardTsKey(id: Long) = RedisCommunityEventUpdateRepository.boardEventTsKey(id)

    private fun commentTsKey(id: Long) = RedisCommunityEventUpdateRepository.commentEventTsKey(id)

    private fun rankingTsKey(
        metric: String,
        period: String,
    ) = RedisCommunityEventUpdateRepository.rankingEventTsKey(metric, period)

    override fun applyBoardCreation(payload: BoardCreatedEventPayload): Boolean {
        val boardId = payload.boardId
        val tsKey = boardTsKey(boardId)
        val eventTs = payload.eventTs

        // 1. 이벤트 타임스탬프 확인 (성공 시 TS 업데이트됨)
        if (!checkAndSetTs(tsKey, eventTs)) return false

        // 2. 데이터 직렬화 및 저장 (실패 시 false 반환하도록 수정)
        val boardModel: BoardReadModel
        val boardJson: String
        try {
            boardModel = BoardReadModel.fromPayload(payload)
            boardJson = dataSerializer.serialize(boardModel) ?: run {
                log.error("[Fake] 게시글 생성 직렬화 결과가 boardId: {}에 대해 null입니다", boardId)
                return false
            }
        } catch (e: Exception) {
            log.error("[Fake] 게시글 생성 직렬화 boardId: {}에 대해 실패했습니다", boardId, e)
            return false
        }

        // 3. Fake 레포지토리 상태 업데이트
        boardDetailRepository.saveBoardDetail(boardModel, eventTs)
        boardIdListRepository.internalAdd(boardIdListRepository.getAllBoardsKey(), eventTs.toDouble(), boardId.toString(), RedisBoardIdListRepository.ALL_BOARD_LIMIT_SIZE)
        boardIdListRepository.internalAdd(RedisBoardIdListRepository.getCategoryKey(payload.categoryId), eventTs.toDouble(), boardId.toString(), RedisBoardIdListRepository.CATEGORY_LIMIT_SIZE)
        boardCountRepository.applyCountInPipeline(mock(), 1)

        return true
    }

    override fun applyBoardUpdate(
        payload: BoardUpdatedEventPayload,
        updatedBoardJson: String,
    ): Boolean {
        val boardId = payload.boardId
        val tsKey = boardTsKey(boardId)
        val eventTs = payload.eventTs

        // 1. 이벤트 타임스탬프 확인 (성공 시 TS 업데이트됨)
        if (!checkAndSetTs(tsKey, eventTs)) return false // 건너뜀

        // 2. 데이터 역직렬화 및 저장 (Fake 에서는 역직렬화하여 사용)
        val boardModel =
            try {
                dataSerializer.deserialize(updatedBoardJson, BoardReadModel::class)
            } catch (e: Exception) {
                log.error("[Fake] 게시글 업데이트 역직렬화 boardId: {}에 대해 실패했습니다", boardId, e)
                return false
            } ?: run {
                log.error("[Fake] 게시글 업데이트 역직렬화 결과가 boardId: {}에 대해 null입니다", boardId)
                return false
            }

        // 3. Fake 레포지토리 상태 업데이트
        boardDetailRepository.saveBoardDetail(boardModel, eventTs)

        return true // 성공
    }

    override fun applyBoardDeletion(payload: BoardDeletedEventPayload): Boolean {
        val boardId = payload.boardId
        val tsKey = boardTsKey(boardId)
        val eventTs = payload.eventTs

        // 1. 이벤트 타임스탬프 확인
        if (getCurrentEventTs(tsKey) >= eventTs) {
            log.debug("[Fake] '{}' 키에 대한 게시글 삭제를 건너뜁니다. 타임스탬프가 더 오래됨 (현재: {}, 수신: {})", tsKey, getCurrentEventTs(tsKey), eventTs)
            return false // 건너뜀
        }

        // 2. Fake 레포지토리 상태 업데이트
        boardDetailRepository.store.remove(RedisBoardDetailRepository.detailKey(boardId))
        boardIdListRepository.internalRemove(boardIdListRepository.getAllBoardsKey(), boardId.toString())
        boardIdListRepository.internalRemove(RedisBoardIdListRepository.getCategoryKey(payload.categoryId), boardId.toString())
        boardCountRepository.applyCountInPipeline(mock(), -1)

        // 3. 타임스탬프 키 자체를 삭제 (Lua 스크립트 로직 반영)
        eventTimestamps.remove(tsKey)
        log.debug("[Fake] 게시글 삭제를 위해 타임스탬프 키 '{}'를 제거했습니다", tsKey)

        return true
    }

    override fun applyBoardLikeUpdate(
        payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload,
        updatedBoardJson: String,
    ): Boolean {
        val boardId = payload.boardId
        val tsKey = boardTsKey(boardId)
        val eventTs = payload.eventTs
        if (!checkAndSetTs(tsKey, eventTs)) return false

        val boardModel =
            try {
                dataSerializer.deserialize(updatedBoardJson, BoardReadModel::class)
            } catch (e: Exception) {
                log.error("[Fake] 좋아요 업데이트 역직렬화 실패", e)
                return false
            } ?: return false
        boardDetailRepository.saveBoardDetail(boardModel, eventTs)
        return true
    }

    override fun applyBoardViewUpdate(
        payload: BoardViewedEventPayload,
        updatedBoardJson: String,
    ): Boolean {
        val boardId = payload.boardId
        val tsKey = boardTsKey(boardId)
        val eventTs = payload.eventTs
        if (!checkAndSetTs(tsKey, eventTs)) return false

        val boardModel =
            try {
                dataSerializer.deserialize(updatedBoardJson, BoardReadModel::class)
            } catch (e: Exception) {
                log.error("[Fake] 조회수 업데이트 역직렬화 실패", e)
                return false
            } ?: return false
        boardDetailRepository.saveBoardDetail(boardModel, eventTs)
        return true
    }

    override fun applyBoardRankingUpdate(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload): Boolean {
        val metricName = payload.metric.name.lowercase()
        val periodName = payload.period.name.lowercase()
        val key = RedisBoardIdListRepository.getRankingKey(payload.metric, payload.period) ?: return false
        val tsKey = rankingTsKey(metricName, periodName)
        val eventTs = payload.eventTs

        // 1. 랭킹 타임스탬프 확인 (성공 시 TS 업데이트됨)
        if (!checkAndSetTs(tsKey, eventTs)) return false // 건너뜀

        // 2. Fake 레포지토리 상태 업데이트
        boardIdListRepository.replaceRankingListInPipeline(mock(), key, payload.rankings)

        return true // 성공
    }

    override fun applyCommentCreation(
        payload: com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload,
        commentJson: String,
        updatedBoardJson: String?,
    ): Boolean {
        val commentId = payload.commentId
        val boardId = payload.boardId
        val commentTsKey = commentTsKey(commentId)
        val eventTs = payload.eventTs

        // 1. 댓글 이벤트 타임스탬프 확인 (성공 시 TS 업데이트됨)
        if (!checkAndSetTs(commentTsKey, eventTs)) return false // 건너뜀

        // 2. 댓글 데이터 저장
        val commentModel =
            try {
                dataSerializer.deserialize(commentJson, CommentReadModel::class)
            } catch (e: Exception) {
                log.error("[Fake] 댓글 생성 역직렬화 실패", e)
                return false
            } ?: return false
        commentDetailRepository.saveCommentDetail(commentModel, eventTs)
        commentIdListRepository.internalAdd(boardId, commentId, eventTs.toDouble())
        commentCountRepository.applyBoardCommentCountInPipeline(mock(), boardId, 1)
        commentCountRepository.applyTotalCountInPipeline(mock(), 1)

        // 3. 게시글 상세 정보 업데이트 (선택적, 타임스탬프 확인 포함!)
        if (updatedBoardJson != null && updatedBoardJson.isNotBlank()) {
            val boardTsKey = boardTsKey(boardId)
            if (eventTs > getCurrentEventTs(boardTsKey)) {
                val boardModel =
                    try {
                        dataSerializer.deserialize(updatedBoardJson, BoardReadModel::class)
                    } catch (e: Exception) {
                        null
                    }
                if (boardModel != null) {
                    boardDetailRepository.saveBoardDetail(boardModel, eventTs)
                    checkAndSetTs(boardTsKey, eventTs)
                    log.debug("[Fake] 댓글 생성으로 인해 '{}' 키의 게시글 정보와 타임스탬프를 업데이트했습니다", boardTsKey)
                } else {
                    log.error("[Fake] 댓글 생성 {}, 게시글 ID {}에 대한 updatedBoardJson 역직렬화 실패", payload.commentId, boardId)
                }
            } else {
                log.debug("[Fake] 댓글 생성 중 '{}' 키에 대한 게시글 업데이트를 건너뜁니다. 타임스탬프가 더 오래됨 (게시글TS: {}, 이벤트TS: {})", boardTsKey, getCurrentEventTs(boardTsKey), eventTs)
            }
        }

        return true
    }

    override fun applyCommentUpdate(
        payload: CommentUpdatedEventPayload,
        updatedCommentJson: String,
    ): Boolean {
        val commentId = payload.commentId
        val tsKey = commentTsKey(commentId)
        val eventTs = payload.eventTs

        // 1. 댓글 이벤트 타임스탬프 확인 (성공 시 TS 업데이트됨)
        if (!checkAndSetTs(tsKey, eventTs)) return false

        // 2. 댓글 데이터 저장
        val commentModel =
            try {
                dataSerializer.deserialize(updatedCommentJson, CommentReadModel::class)
            } catch (e: Exception) {
                log.error("[Fake] 댓글 업데이트 역직렬화 실패", e)
                return false
            } ?: return false
        commentDetailRepository.saveCommentDetail(commentModel, eventTs)

        return true
    }

    override fun applyCommentDeletion(
        payload: CommentDeletedEventPayload,
        updatedBoardJson: String?,
    ): Boolean {
        val commentId = payload.commentId
        val boardId = payload.boardId
        val commentTsKey = commentTsKey(commentId)
        val eventTs = payload.eventTs

        // 1. 댓글 이벤트 타임스탬프 확인 (삭제는 TS 업데이트 안 함)
        if (getCurrentEventTs(commentTsKey) >= eventTs) {
            log.debug("[Fake] '{}' 키에 대한 댓글 삭제를 건너뜁니다. 타임스탬프가 더 오래됨 (현재: {}, 수신: {})", commentTsKey, getCurrentEventTs(commentTsKey), eventTs)
            return false // 건너뜀
        }

        // 2. 댓글 데이터 삭제
        commentDetailRepository.store.remove(RedisCommentDetailRepository.detailKey(commentId))
        commentIdListRepository.internalRemove(boardId, commentId)
        commentCountRepository.applyBoardCommentCountInPipeline(mock(), boardId, -1)
        commentCountRepository.applyTotalCountInPipeline(mock(), -1)

        // 3. 게시글 상세 정보 업데이트 (선택적, 타임스탬프 확인 포함!)
        if (updatedBoardJson != null && updatedBoardJson.isNotBlank()) {
            val boardTsKey = boardTsKey(boardId)
            if (eventTs > getCurrentEventTs(boardTsKey)) {
                val boardModel =
                    try {
                        dataSerializer.deserialize(updatedBoardJson, BoardReadModel::class)
                    } catch (e: Exception) {
                        null
                    }
                if (boardModel != null) {
                    boardDetailRepository.saveBoardDetail(boardModel, eventTs)
                    checkAndSetTs(boardTsKey, eventTs)
                    log.debug("[Fake] 댓글 삭제로 인해 '{}' 키의 게시글 정보와 타임스탬프를 업데이트했습니다", boardTsKey)
                } else {
                    log.error("[Fake] 댓글 삭제 {}, 게시글 ID {}에 대한 updatedBoardJson 역직렬화 실패", payload.commentId, boardId)
                }
            } else {
                log.debug("[Fake] 댓글 삭제 중 '{}' 키에 대한 게시글 업데이트를 건너뜁니다. 타임스탬프가 더 오래됨 (게시글TS: {}, 이벤트TS: {})", boardTsKey, getCurrentEventTs(boardTsKey), eventTs)
            }
        }

        // 4. 댓글 타임스탬프 키 삭제 (Lua 스크립트 로직 반영)
        eventTimestamps.remove(commentTsKey)
        log.debug("[Fake] 댓글 삭제를 위해 타임스탬프 키 '{}'를 제거했습니다", commentTsKey)

        return true
    }

    fun setEventTs(
        key: String,
        timestamp: Long,
    ) {
        eventTimestamps[key] = timestamp
    }

    fun getEventTs(key: String): Long? = getCurrentEventTs(key).takeIf { it > 0L }

    fun clear() {
        eventTimestamps.clear()
    }
}
