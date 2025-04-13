package com.example.jobstat.core.event.dlt

import com.example.jobstat.core.base.AuditableEntitySnow
import jakarta.persistence.*

/**
 * Dead Letter Queue 이벤트 엔티티
 * - Board 엔티티 스타일 적용 (internal class, private constructor, companion object factory)
 * - DLT 이벤트는 생성 후 변경되지 않는 불변(Immutable) 객체로 설계
 */
@Entity
@Table(name = "dead_letter_topic")
// 1. internal class로 변경
class DeadLetterTopicEvent private constructor( // 2. private constructor 사용 (companion object 에서만 호출)

    // 3. 모든 비즈니스 필드를 val로 선언 (불변)
    @Column(nullable = false, length = 255, name = "event_id")
    val eventId: String,

    @Column(nullable = false, length = 100, name = "event_type")
    val eventType: String,

    @Column(nullable = false, name = "retry_count")
    val retryCount: Int,

    @Column(nullable = false, length = 50, name = "failure_source")
    val failureSource: String,

    @Column(nullable = true, length = 2000, name = "last_error")
    val lastError: String?,
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT", name = "payload")
    val payload: String,
) : AuditableEntitySnow() {

    companion object {
        /**
         * DeadLetterTopicEvent 엔티티를 생성합니다. ID는 자동 생성됩니다.
         */
        fun create(
            eventId: String,
            eventType: String,
            retryCount: Int,
            failureSource: String,
            lastError: String?,
            payload: String
        ): DeadLetterTopicEvent {
            // 필요 시 입력값 유효성 검사 추가
            require(eventId.isNotBlank()) { "eventId는 비어 있을 수 없습니다." }
            require(eventType.isNotBlank()) { "eventType은 비어 있을 수 없습니다." }
            require(failureSource.isNotBlank()) { "failureSource는 비어 있을 수 없습니다." }
            require(payload.isNotBlank()) { "payload는 비어 있을 수 없습니다." }

            // private 생성자 호출
            return DeadLetterTopicEvent(
                eventId = eventId,
                eventType = eventType,
                retryCount = retryCount,
                failureSource = failureSource,
                lastError = lastError?.take(2000), // DB 컬럼 길이에 맞게 잘라내기 (방어적 코딩)
                payload = payload,
            )
        }
    }
}