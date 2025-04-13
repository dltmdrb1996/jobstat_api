package com.example.jobstat.core.event.publisher

import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType

interface EventPublisher {
    /**
     * 이벤트 발행 함수
     * @param type 이벤트 타입
     * @param payload 이벤트 페이로드
     * @param sharedKey 샤딩 키
     */
    fun publish(type: EventType, payload: EventPayload)

    /**
     * 이 퍼블리셔가 지원하는 이벤트 타입들을 반환
     * @return 지원하는 이벤트 타입 집합
     */
    fun getSupportedEventTypes(): Set<EventType>
}