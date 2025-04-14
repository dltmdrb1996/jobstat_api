package com.example.jobstat.community.event

import com.example.jobstat.core.event.consumer.AbstractEventConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Component

@Component
class CommunityCommandEventConsumer : AbstractEventConsumer() {
    @Value("\${kafka.consumer.community-command.topic:community-command")
    private lateinit var topic: String

    @Value("\${kafka.consumer.community-command.group-id:community-command-consumer-group")
    private lateinit var groupId: String

    /**
     * community-board 토픽에서 이벤트를 수신하는 리스너.
     * AbstractEventConsumer의 consumeEvent를 호출하여 이벤트 처리 위임.
     */
    @RetryableTopic(
        attempts = "\${kafka.consumer.community-command.retry.attempts:3}",
        backoff =
            Backoff(
                delayExpression = "\${kafka.consumer.community-command.retry.delay-ms:1000}",
                multiplierExpression = "\${kafka.consumer.community-command.retry.multiplier:2.0}",
            ),
        dltTopicSuffix = "\${kafka.consumer.common.dlt-suffix:.DLT}",
        autoCreateTopics = "\${kafka.consumer.common.auto-create-dlt:false}",
    )
    @KafkaListener(
        topics = ["\${kafka.consumer.community-command.topic:community-command}"],
        groupId = "\${kafka.consumer.community-command.group-id:community-command-group}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun receiveBoardEvent(event: String) {
        log.info(
            "[{}] Kafka 메시지 수신 시도 (Board Command): topic={}, groupId={}",
            this::class.simpleName,
            topic,
            groupId,
        )
        super.consumeEvent(event)
    }
}
