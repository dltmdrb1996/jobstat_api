package com.example.jobstat.core.event

import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.CommentCreatedEventPayload
import com.example.jobstat.core.event.payload.comment.CommentDeletedEventPayload
import com.example.jobstat.core.event.payload.comment.CommentUpdatedEventPayload
import org.slf4j.LoggerFactory

/**
 * 이벤트 타입 열거형
 * 시스템에서 사용되는 모든 이벤트 타입을 정의합니다.
 */
enum class EventType(
    val payloadClass: Class<out EventPayload>, // 페이로드 클래스 타입
    val topic: String, // Kafka 토픽 이름
) {
    // ===================================================
    // 게시글 관련 이벤트 타입
    // ===================================================

    // 게시글 생성, 수정, 삭제
    BOARD_CREATED(BoardCreatedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_UPDATED(BoardUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_DELETED(BoardDeletedEventPayload::class.java, Topic.COMMUNITY_READ),

    // 게시글 상호작용 (좋아요, 조회 등)
    BOARD_LIKED(BoardLikedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_UNLIKED(BoardUnlikedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_VIEWED(BoardViewedEventPayload::class.java, Topic.COMMUNITY_READ),
    BOARD_RANKING_UPDATED(BoardRankingUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),

    // 게시글 카운터 증가 명령
    BOARD_INC_VIEW(IncViewEventPayload::class.java, Topic.COMMUNITY_COMMAND),

    // ===================================================
    // 댓글 관련 이벤트 타입
    // ===================================================

    COMMENT_CREATED(CommentCreatedEventPayload::class.java, Topic.COMMUNITY_READ),
    COMMENT_UPDATED(CommentUpdatedEventPayload::class.java, Topic.COMMUNITY_READ),
    COMMENT_DELETED(CommentDeletedEventPayload::class.java, Topic.COMMUNITY_READ),
    ;

    // ===================================================
    // 유틸리티 메소드
    // ===================================================

    companion object {
        private val log by lazy { LoggerFactory.getLogger(this::class.java) }

        /**
         * 문자열에서 이벤트 타입 열거형으로 변환합니다.
         *
         * @param type 이벤트 타입 문자열
         * @return 해당하는 이벤트 타입 또는 변환 실패 시 null
         */
        fun from(type: String): EventType? =
            try {
                valueOf(type)
            } catch (e: Exception) {
                log.error("[이벤트 타입 변환 실패] type=$type", e)
                null
            }
    }

    // ===================================================
    // 상수 정의
    // ===================================================

    /**
     * Kafka 토픽 상수
     */
    object Topic {
        const val COMMUNITY_COMMAND = "community-command"
        const val COMMUNITY_READ = "community-read"
    }

    /**
     * Kafka 컨슈머 그룹 ID 상수
     */
    object GroupId {
        const val COMMUNITY_COMMAND = "community-command-consumer-group"
        const val COMMUNITY_READ = "community-read-consumer-group"
    }
}
