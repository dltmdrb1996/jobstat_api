package com.example.jobstat.core.event.publisher

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class DLQPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val serializer: DataSerializer
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    // DLQ 토픽 이름 (필요시 application.properties에서 주입받도록 수정 가능)
    private val dlqTopic = "community-dlq"

    /**
     * 재시도 후에도 실패한 이벤트를 DLQ로 전송
     */
    fun publishToDLQ(event: Event<out EventPayload>, handler: EventHandlingUseCase<*, *, *>) {
        val dlqMessage = buildDLQMessage(event, handler)
        try {
            kafkaTemplate.send(dlqTopic, dlqMessage).get()
            log.info("이벤트 {} (handler: {}) DLQ 전송 성공", event.id, handler.javaClass.simpleName)
        } catch (e: Exception) {
            log.error("이벤트 {}를 DLQ 전송 실패: {}", event.id, e.message)
        }
    }

    /**
     * DLQ에 전송할 메시지를 생성 (이벤트 ID, 타입, 핸들러 정보, 페이로드, 타임스탬프 포함)
     */
    private fun buildDLQMessage(event: Event<out EventPayload>, handler: EventHandlingUseCase<*, *, *>): String {
        val dlqPayload = mapOf(
            "eventId" to event.id,
            "eventType" to event.type.name,
            "handler" to handler.javaClass.simpleName,
            "payload" to event.payload,
            "timestamp" to System.currentTimeMillis()
        )
        return serializer.serialize(dlqPayload) ?: ""
    }
}
