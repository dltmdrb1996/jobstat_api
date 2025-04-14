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
    @Value("\${kafka.consumer.community-read.topic:community-read}") // 토픽명 설정 주입
    private lateinit var topic: String

    @Value("\${kafka.consumer.community-read.group-id:community-read-group}") // 그룹 ID 설정 주입
    private lateinit var groupId: String

    /**
     * Kafka에서 이벤트를 수신하는 리스너 메서드입니다.
     * consumeEvent 호출 중 예외가 발생하면 @RetryableTopic 설정에 따라 재시도됩니다.
     * 최종 실패 시 메시지는 DLT 토픽 ({TOPIC}{DLT_SUFFIX})으로 전송됩니다.
     */
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
    fun receiveEvent(event: String, ack: Acknowledgment) {
        log.info(
            "[{}] Kafka 메시지 수신 시도: topic=$topic, groupId=$groupId",
            this::class.simpleName,
        )
        super.consumeEvent(event, ack)
    }
}
