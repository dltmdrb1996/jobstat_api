package com.example.jobstat.community_read.event

import com.example.jobstat.core.event.consumer.AbstractEventConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.support.Acknowledgment
import org.springframework.retry.annotation.Backoff
import org.springframework.stereotype.Component

@Component
class CommunityReadEventConsumer : AbstractEventConsumer() {
    @Value("\${kafka.consumer.community-read.topic:community-read}")
    private lateinit var topic: String

    @Value("\${kafka.consumer.community-read.group-id:community-read-group}")
    private lateinit var groupId: String

    @RetryableTopic(
        attempts = "\${kafka.consumer.community-read.retry.attempts:3}",
        backoff =
            Backoff(
                delayExpression = "\${kafka.consumer.community-read.retry.delay-ms:1000}",
                multiplierExpression = "\${kafka.consumer.community-read.retry.multiplier:2.0}",
            ),
        dltTopicSuffix = "\${kafka.consumer.common.dlt-suffix:.DLT}",
        autoCreateTopics = "\${kafka.consumer.common.auto-create-dlt:false}",
    )
    @KafkaListener(
        topics = ["\${kafka.consumer.community-read.topic:community-read}"],
        groupId = "\${kafka.consumer.community-read.group-id:community-read-group}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun receiveEvent(
        event: String,
        ack: Acknowledgment,
    ) {
        log.info(
            "[{}] Kafka 메시지 수신 시도: topic=$topic, groupId=$groupId",
            this::class.simpleName,
        )
        super.consumeEvent(event, ack)
    }
}
