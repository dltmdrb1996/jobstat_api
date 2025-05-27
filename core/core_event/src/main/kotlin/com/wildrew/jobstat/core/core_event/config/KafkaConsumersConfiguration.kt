package com.wildrew.jobstat.core.core_event.config

import com.wildrew.jobstat.core.core_event.model.ConsumerConfig
import com.wildrew.jobstat.core.core_event.model.EventType // EventType 접근을 위해 import
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kafka")
data class KafkaConsumersConfiguration(
    var consumer: MutableMap<String, ConsumerConfig> = mutableMapOf(),
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @PostConstruct
    fun initializeEventTypeConstants() {
        if (consumer.isEmpty()) {
            log.warn("KafkaConsumersConfiguration is empty. Please check your configuration.")
        } else {
            EventType.Topic.initialize(consumer)
            EventType.GroupId.initialize(consumer) // GroupId도 이 방식으로 관리한다면
            log.info("KafkaConsumersConfiguration initialized. Loaded consumer configs: $consumer")
        }
    }

    // 특정 컨슈머 이름으로 설정을 가져오는 편의 메소드 (선택적)
    fun getConsumerConfig(consumerName: String): ConsumerConfig? = consumer[consumerName]
}
