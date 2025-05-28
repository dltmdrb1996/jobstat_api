package com.wildrew.jobstat.community_read.service

import com.wildrew.jobstat.community_read.model.BoardReadModel
import com.wildrew.jobstat.community_read.model.CommentReadModel
import com.wildrew.jobstat.community_read.repository.*
import com.wildrew.jobstat.community_read.repository.impl.RedisBoardDetailRepository
import com.wildrew.jobstat.community_read.repository.impl.RedisBoardIdListRepository
import com.wildrew.jobstat.community_read.repository.impl.RedisCommentDetailRepository
import com.wildrew.jobstat.core.core_event.model.payload.board.*
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentDeletedEventPayload
import com.wildrew.jobstat.core.core_event.model.payload.comment.CommentUpdatedEventPayload
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class CommunityEventHandlerVerPipe(
    private val redisTemplate: StringRedisTemplate,
    private val boardIdListRepository: BoardIdListRepository,
    private val boardDetailRepository: BoardDetailRepository,
    private val boardCountRepository: BoardCountRepository,
    private val commentIdListRepository: CommentIdListRepository,
    private val commentDetailRepository: CommentDetailRepository,
    private val commentCountRepository: CommentCountRepository,
) : CommunityEventHandler {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val EVENT_TS_KEY_PREFIX = "community-read::event-ts::"
        private const val RANKING_EVENT_TS_KEY_FORMAT = "ranking:%s:%s" // 지표:기간
        const val EVENT_TS_TTL_DAYS = 1L
        const val EVENT_TS_TTL_SECONDS = EVENT_TS_TTL_DAYS * 24 * 60 * 60
    }

    override fun handleBoardCreated(payload: BoardCreatedEventPayload) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val eventTsKey = "board:${payload.boardId}"

            if (!checkEventTs(stringConn, eventTsKey, payload.eventTs)) {
                return@executePipelined null
            }

            // 1. 상세 정보 저장 (JSON 직렬화 방식)
            with(BoardReadModel.fromPayload(payload)) {
                val score = eventTs.toDouble()
                // JSON 문자열 SET 명령어로 저장
                boardDetailRepository.saveBoardDetailInPipeline(stringConn, this)
                // 2. 최신순 목록 갱신
                boardIdListRepository.addBoardInPipeline(stringConn, id, score)
                // 3. 카테고리별 목록 갱신 (카테고리 존재 시)
                boardIdListRepository.addBoardToCategoryInPipeline(stringConn, id, categoryId, score)
                // 4. 전체 게시글 수 증가
                boardCountRepository.applyCountInPipeline(stringConn, 1)
            }

            updateEventTs(stringConn, eventTsKey, payload.eventTs)
            null
        }
    }

    override fun handleBoardUpdated(payload: BoardUpdatedEventPayload) {
        // 1. (Read) 파이프라인 시작 전에 현재 데이터를 읽음
        val currentBoard = boardDetailRepository.findBoardDetail(payload.boardId)

        if (currentBoard == null) {
            log.warn("게시글 수정 처리 실패: 원본 데이터를 찾을 수 없음. boardId=${payload.boardId}")
            return // 원본이 없으면 업데이트 불가
        }

        // 2. (Modify) 읽어온 데이터 기반으로 새 객체 생성 (Kotlin copy 활용)
        val updatedBoard =
            currentBoard.copy(
                title = payload.title,
                content = payload.content,
                eventTs = payload.eventTs,
            )

        // 3. (Write) 파이프라인 내에서 업데이트된 객체 저장
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val eventTsKey = "board:${payload.boardId}"

            // 이벤트 타임스탬프 검증
            if (!checkEventTs(stringConn, eventTsKey, payload.eventTs)) {
                return@executePipelined null
            }

            // 업데이트된 전체 JSON 문자열을 SET 명령어로 덮어씀
            boardDetailRepository.saveBoardDetailInPipeline(stringConn, updatedBoard)

            // 이벤트 타임스탬프 업데이트
            updateEventTs(stringConn, eventTsKey, payload.eventTs)
            log.debug("게시글 내용 업데이트 완료 (Read-Modify-Write): boardId=${payload.boardId}")
            null
        }
    }

    override fun handleBoardDeleted(payload: BoardDeletedEventPayload) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val eventTsKey = "board:${payload.boardId}"

            if (!checkEventTs(stringConn, eventTsKey, payload.eventTs)) {
                return@executePipelined null
            }

            // 1. 최신순 목록 및 카테고리 목록에서 제거
            boardIdListRepository.removeBoardInPipeline(stringConn, payload.boardId, payload.categoryId)

            // 2. 상세 정보 삭제 (직렬화된 JSON key 삭제)
            stringConn.del(RedisBoardDetailRepository.detailKey(payload.boardId))
            stringConn.del(RedisBoardDetailRepository.detailStateKey(payload.boardId))

            // 3. eventTs 키 삭제 (삭제 이벤트는 키 자체를 삭제)
            stringConn.del(EVENT_TS_KEY_PREFIX + eventTsKey)

            // 4. 전체 게시글 수 감소
            boardCountRepository.applyCountInPipeline(stringConn, -1)

            log.debug("게시글 및 관련 키 삭제됨: boardId=${payload.boardId}")
            null
        }
    }

    override fun handleBoardLiked(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardLikedEventPayload) {
        with(payload) {
            val currentBoard = boardDetailRepository.findBoardDetail(boardId)
            if (currentBoard == null) {
                log.warn("게시글 좋아요 처리 실패: 원본 데이터를 찾을 수 없음. boardId=$boardId")
                return
            }

            val updatedBoard =
                currentBoard.copy(
                    likeCount = likeCount, // 페이로드의 최종 likeCount 사용
                    eventTs = eventTs,
                )

            redisTemplate.executePipelined { conn ->
                val stringConn = conn as StringRedisConnection
                val eventTsKey = "board:$boardId"

                if (!checkEventTs(stringConn, eventTsKey, eventTs)) {
                    return@executePipelined null
                }

                // 상세 정보 JSON 업데이트
                boardDetailRepository.saveBoardDetailInPipeline(stringConn, updatedBoard)

                updateEventTs(stringConn, eventTsKey, eventTs)
                log.debug("게시글 좋아요 수 업데이트 완료 (Read-Modify-Write): boardId=$boardId, likeCount=$likeCount")
                null
            }
        }
    }

    override fun handleBoardViewed(payload: BoardViewedEventPayload) {
        val currentBoard = boardDetailRepository.findBoardDetail(payload.boardId)
        if (currentBoard == null) {
            log.warn("게시글 조회수 처리 실패: 원본 데이터를 찾을 수 없음. boardId=${payload.boardId}")
            return
        }

        val updatedBoard =
            currentBoard.copy(
                viewCount = payload.viewCount, // 페이로드의 최종 viewCount 사용
                eventTs = payload.eventTs,
            )

        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val eventTsKey = "board:${payload.boardId}"

            if (!checkEventTs(stringConn, eventTsKey, payload.eventTs)) {
                return@executePipelined null
            }

            // 상세 정보 JSON 업데이트
            boardDetailRepository.saveBoardDetailInPipeline(stringConn, updatedBoard)

            updateEventTs(stringConn, eventTsKey, payload.eventTs)
            log.debug("게시글 조회수 업데이트 완료 (Read-Modify-Write): boardId=${payload.boardId}, viewCount=${payload.viewCount}")
            null
        }
    }

    override fun handleBoardRankingUpdated(payload: com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload) {
        val rankingKey = RedisBoardIdListRepository.getRankingKey(payload.metric, payload.period)
        if (rankingKey == null) {
            log.error("랭킹 업데이트에 잘못된 지표/기간 조합이 사용됨: {}", payload)
            return
        }

        val eventTsEntityKey =
            RANKING_EVENT_TS_KEY_FORMAT.format(
                payload.metric.name.lowercase(),
                payload.period.name.lowercase(),
            )

        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection

            if (!checkEventTs(stringConn, eventTsEntityKey, payload.eventTs)) {
                log.warn(
                    "오래된 랭킹 업데이트 이벤트 건너뜀 - {}. 현재 타임스탬프가 이벤트 타임스탬프보다 최신: {}",
                    eventTsEntityKey,
                    payload.eventTs,
                )
                return@executePipelined null
            }

            boardIdListRepository.replaceRankingListInPipeline(stringConn, rankingKey, payload.rankings)

            updateEventTs(stringConn, eventTsEntityKey, payload.eventTs)

            log.debug("랭킹 리스트 '{}' 업데이트 완료 - {}개 항목", rankingKey, payload.rankings.size)
            null
        }
    }

    override fun handleCommentCreated(payload: com.wildrew.jobstat.core.core_event.model.payload.comment.CommentCreatedEventPayload) {
        val currentBoard = boardDetailRepository.findBoardDetail(payload.boardId)
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val commentEventTsKey = "comment:${payload.commentId}"

            if (!checkEventTs(stringConn, commentEventTsKey, payload.eventTs)) {
                log.debug("댓글 생성 이벤트 타임스탬프가 이전보다 작음: commentId=${payload.commentId}, eventTs=${payload.eventTs}")
                return@executePipelined null
            }

            // 1. 댓글 상세 정보 저장
            val comment = CommentReadModel.fromPayload(payload)
            commentDetailRepository.saveCommentDetailInPipeline(stringConn, comment)
            // 2. 게시글의 댓글 목록에 추가
            commentIdListRepository.addCommentInPipeline(stringConn, payload.boardId, payload.commentId, payload.eventTs.toDouble())
            // 3. 게시글의 댓글 수 증가 (카운터)
            commentCountRepository.applyBoardCommentCountInPipeline(stringConn, payload.boardId, 1)
            // 4. 전체 댓글 수 증가 (카운터)
            commentCountRepository.applyTotalCountInPipeline(stringConn, 1)

            // 5. 게시글 상세 정보의 댓글 수 업데이트 (Read-Modify-Write)
            if (currentBoard != null) {
                val updatedBoard = currentBoard.copy(commentCount = currentBoard.commentCount + 1)
                boardDetailRepository.saveBoardDetailInPipeline(stringConn, updatedBoard)
            } else {
                log.warn("댓글 생성 시 게시글 정보 업데이트 실패: 원본 게시글 데이터를 찾을 수 없음. boardId=${payload.boardId}")
            }

            updateEventTs(stringConn, commentEventTsKey, payload.eventTs)
            log.debug("댓글 생성 및 관련 데이터 업데이트됨: commentId=${payload.commentId}, boardId=${payload.boardId}")
            null
        }
    }

    override fun handleCommentUpdated(payload: CommentUpdatedEventPayload) {
        val currentComment = commentDetailRepository.findCommentDetail(payload.commentId)

        if (currentComment == null) {
            log.warn("댓글 수정 처리 실패: 원본 댓글 데이터를 찾을 수 없음. commentId=${payload.commentId}")
            return
        }

        val updatedComment =
            currentComment.copy(
                content = payload.content,
                updatedAt = payload.updatedAt,
                eventTs = payload.eventTs,
            )

        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val eventTsKey = "comment:${payload.commentId}"

            if (!checkEventTs(stringConn, eventTsKey, payload.eventTs)) {
                log.debug("댓글 수정 이벤트 타임스탬프가 이전보다 작음: commentId=${payload.commentId}, eventTs=${payload.eventTs}")
                return@executePipelined null
            }

            commentDetailRepository.saveCommentDetailInPipeline(stringConn, updatedComment)

            updateEventTs(stringConn, eventTsKey, payload.eventTs)
            log.debug("댓글 내용 업데이트 완료 (Read-Modify-Write): commentId=${payload.commentId}")
            null
        }
    }

    override fun handleCommentDeleted(payload: CommentDeletedEventPayload) {
        val currentBoard = boardDetailRepository.findBoardDetail(payload.boardId)
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val commentEventTsKey = "comment:${payload.commentId}"

            if (!checkEventTs(stringConn, commentEventTsKey, payload.eventTs)) {
                return@executePipelined null
            }

            // 1. 게시글의 댓글 목록에서 제거
            commentIdListRepository.removeCommentInPipeline(stringConn, payload.boardId, payload.commentId)
            // 2. 댓글 상세 정보 삭제
            stringConn.del(RedisCommentDetailRepository.detailKey(payload.commentId))
            stringConn.del(RedisCommentDetailRepository.detailStateKey(payload.commentId))
            // 3. 댓글 카운트 감소
            commentCountRepository.applyBoardCommentCountInPipeline(stringConn, payload.boardId, -1)
            commentCountRepository.applyTotalCountInPipeline(stringConn, -1)

            // 4. 게시글 상세 정보의 댓글 수 업데이트
            if (currentBoard != null) {
                val updatedBoard = currentBoard.copy(commentCount = (currentBoard.commentCount - 1).coerceAtLeast(0))
                boardDetailRepository.saveBoardDetailInPipeline(stringConn, updatedBoard)
            } else {
                log.warn("댓글 삭제 시 게시글 정보 업데이트 실패: 원본 게시글 데이터를 찾을 수 없음. boardId=${payload.boardId}")
            }

            // 5. 이벤트 타임스탬프 키 삭제
            stringConn.del(EVENT_TS_KEY_PREFIX + commentEventTsKey)
            log.debug("댓글 삭제 및 관련 데이터 업데이트됨: commentId=${payload.commentId}, boardId=${payload.boardId}")
            null
        }
    }

    override fun handleBulkBoardIncrements(payload: BulkBoardIncrementsPayload) {
    }

    private fun checkEventTs(
        conn: StringRedisConnection,
        key: String,
        eventTs: Long,
    ): Boolean {
        val eventTsKey = EVENT_TS_KEY_PREFIX + key
        val currentTs = conn.hGet(eventTsKey, "ts")?.toLongOrNull() ?: 0
        return eventTs > currentTs
    }

    private fun updateEventTs(
        conn: StringRedisConnection,
        key: String,
        eventTs: Long,
    ) {
        val eventTsKey = EVENT_TS_KEY_PREFIX + key
        conn.hSet(eventTsKey, "ts", eventTs.toString())
        // TTL 설정 (초 단위)
        conn.expire(eventTsKey, EVENT_TS_TTL_DAYS * 24 * 60 * 60)
    }
}
