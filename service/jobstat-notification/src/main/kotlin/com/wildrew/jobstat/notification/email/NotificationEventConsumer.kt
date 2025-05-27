package com.wildrew.jobstat.notification.email

import com.wildrew.jobstat.core.core_event.consumer.AbstractEventConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.support.Acknowledgment
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Component

@Component
class NotificationEventConsumer : AbstractEventConsumer() {
    @Value("\${kafka.consumer.notification.topic")
    private lateinit var topic: String

    @Value("\${kafka.consumer.notification.group-id")
    private lateinit var groupId: String

    @RetryableTopic(
        attempts = "\${kafka.consumer.notification.retry.attempts:3}",
        backoff =
            Backoff(
                delayExpression = "\${kafka.consumer.notification.retry.delay-ms:1000}",
                multiplierExpression = "\${kafka.consumer.notification.retry.multiplier:2.0}",
            ),
        dltTopicSuffix = "\${kafka.consumer.common.dlt-suffix:.DLT}",
        autoCreateTopics = "\${kafka.consumer.common.auto-create-dlt:true}",
    )
    @KafkaListener(
        topics = ["\${kafka.consumer.notification.topic:notification}"],
        groupId = "\${kafka.consumer.notification.group-id:notification-group}",
        containerFactory = "#{@coreKafkaListenerContainerFactory}",
    )
    fun receiveBoardEvent(
        event: String,
        ack: Acknowledgment,
    ) {
        log.debug(
            "[{}] Kafka 메시지 수신 시도 (Board Command): topic={}, groupId={}",
            this::class.simpleName,
            topic,
            groupId,
        )
        super.consumeEvent(event, ack, this::class.simpleName ?: "error") // consumerIdentifier로 클래스 이름 사용
    }
}
