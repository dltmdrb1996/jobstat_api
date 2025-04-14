package com.example.jobstat.core.event.publisher

import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType

/**
 * 이벤트 발행자 인터페이스
 * 모든 이벤트 발행자는 이 인터페이스를 구현해야 합니다.
 */
interface EventPublisher {
    /**
     * 이벤트를 발행합니다.
     *
     * @param type 이벤트 타입
     * @param payload 이벤트 페이로드
     */
    fun publish(
        type: EventType,
        payload: EventPayload,
    )

    /**
     * 이 발행자가 지원하는 이벤트 타입 목록을 반환합니다.
     *
     * @return 지원하는 이벤트 타입 집합
     */
    fun getSupportedEventTypes(): Set<EventType>
}
