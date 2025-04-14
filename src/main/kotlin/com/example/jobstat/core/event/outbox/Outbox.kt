package com.example.jobstat.core.event.outbox

import com.example.jobstat.core.base.AuditableEntitySnow
import com.example.jobstat.core.event.EventType
import jakarta.persistence.*

/**
 * 아웃박스 엔티티
 * 이벤트를 일시적으로 저장하고 비동기적으로 처리하기 위한 테이블
 */
@Entity
@Table(name = "outbox")
class Outbox protected constructor(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val eventType: EventType,
    @Column(name = "payload", columnDefinition = "TEXT")
    val event: String,
) : AuditableEntitySnow() {
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        internal set

    // ===================================================
    // 재시도 관련 메소드
    // ===================================================

    /**
     * 재시도 횟수를 증가시킵니다.
     * @return 증가된 재시도 횟수
     */
    fun incrementRetryCount(): Int {
        this.retryCount++
        return this.retryCount
    }

    /**
     * 최대 재시도 횟수를 초과했는지 확인합니다.
     * @param maxRetries 최대 재시도 횟수
     * @return 최대 재시도 횟수 초과 여부
     */
    fun isMaxRetryExceeded(maxRetries: Int): Boolean = retryCount >= maxRetries

    // ===================================================
    // 객체 복사 메소드
    // ===================================================

    /**
     * 현재 객체를 복사하여 새로운 Outbox 객체를 생성합니다.
     * @param eventType 이벤트 타입 (기본값: 현재 객체의 eventType)
     * @param event 이벤트 내용 (기본값: 현재 객체의 event)
     * @param retryCount 재시도 횟수 (기본값: 현재 객체의 retryCount)
     * @return 복사된 Outbox 객체
     */
    fun copy(
        eventType: EventType = this.eventType,
        event: String = this.event,
        retryCount: Int = this.retryCount,
    ): Outbox =
        Outbox(
            eventType = eventType,
            event = event,
        ).apply {
            this.retryCount = retryCount
        }

    // ===================================================
    // 정적 팩토리 메소드
    // ===================================================

    companion object {
        /**
         * 새로운 Outbox 객체를 생성합니다.
         * @param eventType 이벤트 타입
         * @param event 이벤트 내용 (JSON 형식)
         * @return 생성된 Outbox 객체
         */
        fun create(
            eventType: EventType,
            event: String,
        ): Outbox {
            require(event.isNotBlank()) { "이벤트 페이로드는 비어 있을 수 없습니다." }

            return Outbox(
                eventType = eventType,
                event = event,
            )
        }
    }
}
