package com.example.jobstat.community_read

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.consumer.AbstractEventConsumer
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * 커뮤니티 Read 모델을 위한 이벤트 소비자
 * 제네릭 타입 기반 전략 패턴과 Kafka 리스너를 통합
 */
@Component
class CommunityReadEventConsumer : AbstractEventConsumer() {
    companion object {
        private const val TOPIC = EventType.Topic.COMMUNITY_READ
        private const val GROUP_ID = EventType.GROUP_ID.COMMUNITY_READ
    }

    /**
     * 이 컨슈머가 처리하는 토픽 반환
     */
    override fun getTopic(): String = TOPIC

    /**
     * 이 컨슈머의 그룹 ID 반환
     */
    override fun getGroupId(): String = GROUP_ID

    /**
     * 이 컨슈머가 지원하는 이벤트 타입
     */
    override fun getSupportedEventTypes(): Set<EventType> {
        return handlerRegistry.getSupportedEventTypes()
    }

    /**
     * Kafka에서 이벤트를 수신하는 리스너 메서드
     */
    @KafkaListener(
        topics = [TOPIC],
        groupId = GROUP_ID,
    )
    fun receiveEvent(event: Event<out EventPayload>) {
        consumeEvent(event)
    }
}