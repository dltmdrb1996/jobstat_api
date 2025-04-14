package com.example.jobstat.core.event.dlt

import com.example.jobstat.core.base.AuditableEntitySnow
import jakarta.persistence.*

/**
 * 데드 레터 토픽 이벤트 엔티티
 * - Board 엔티티 스타일 적용 (internal class, private constructor, companion object factory)
 * - DLT 이벤트는 생성 후 변경되지 않는 불변(Immutable) 객체로 설계
 */
@Entity
@Table(name = "dead_letter_topic")
class DeadLetterTopicEvent private constructor( // private constructor 사용 (companion object에서만 호출)
    // 모든 비즈니스 필드를 val로 선언 (불변성 보장)
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
    // ===================================================
    // 정적 팩토리 메소드
    // ===================================================

    companion object {
        /**
         * 데드 레터 토픽 이벤트 엔티티를 생성합니다.
         * ID는 자동 생성됩니다.
         *
         * @param eventId 이벤트 ID
         * @param eventType 이벤트 타입
         * @param retryCount 재시도 횟수
         * @param failureSource 실패 출처
         * @param lastError 마지막 오류 메시지
         * @param payload 이벤트 페이로드
         * @return 생성된 데드 레터 토픽 이벤트
         */
        fun create(
            eventId: String,
            eventType: String,
            retryCount: Int,
            failureSource: String,
            lastError: String?,
            payload: String,
        ): DeadLetterTopicEvent {
            // 입력값 유효성 검사 수행
            require(eventId.isNotBlank()) { "이벤트 ID는 비어 있을 수 없습니다." }
            require(eventType.isNotBlank()) { "이벤트 타입은 비어 있을 수 없습니다." }
            require(failureSource.isNotBlank()) { "실패 출처는 비어 있을 수 없습니다." }
            require(payload.isNotBlank()) { "페이로드는 비어 있을 수 없습니다." }

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
