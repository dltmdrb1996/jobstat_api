package com.example.jobstat.core.core_event.config // 패키지 변경

import com.example.jobstat.core.core_coroutine.CoroutineModuleConfig
import com.example.jobstat.core.core_event.consumer.EventHandlerRegistry
import com.example.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.example.jobstat.core.core_event.dlt.DLTConsumer
import com.example.jobstat.core.core_event.dlt.DeadLetterTopicRepository
import com.example.jobstat.core.core_event.outbox.OutboxEventPublisher
import com.example.jobstat.core.core_event.outbox.OutboxMessageRelay
import com.example.jobstat.core.core_event.outbox.OutboxProcessor
import com.example.jobstat.core.core_event.outbox.OutboxRepository
import com.example.jobstat.core.core_jpa_base.id_generator.CoreIdGeneratorAutoConfiguration
import com.example.jobstat.core.core_jpa_base.id_generator.SnowflakeGenerator
import com.example.jobstat.core.core_serializer.DataSerializer
import com.example.jobstat.core.core_serializer.config.CoreSerializerAutoConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties

@AutoConfiguration
@AutoConfigureAfter(
    value = [
        KafkaAutoConfiguration::class,
        JpaRepositoriesAutoConfiguration::class,
        CoreSerializerAutoConfiguration::class,
        CoreIdGeneratorAutoConfiguration::class,
        CoroutineModuleConfig::class
    ]
)
@ConditionalOnClass(KafkaOperations::class)
@ConditionalOnProperty(name = ["spring.kafka.bootstrap-servers"])
// JPA Repository 스캔 경로 지정 (core-event-jpa-persistence 모듈의 레포지토리)
//@EnableJpaRepositories(basePackages = ["com.example.jobstat.core.core_event"])
class CoreEventAutoConfiguration {

    // Kafka Listener Container Factory
    @Bean("coreKafkaListenerContainerFactory")
    @Primary
    @ConditionalOnMissingBean(name = ["coreKafkaListenerContainerFactory"])
    fun coreKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String?, String?>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactory
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        }
    }

    // Outbox용 KafkaTemplate
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = ["kafkaTemplate"])
    fun kafkaTemplate(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${jobstat.core.event.kafka.producer.acks-config:all}") acksConfig: String,
        @Value("\${jobstat.core.event.kafka.producer.idempotence.enabled:true}") idempotentProducerEnabled: Boolean
    ): KafkaTemplate<String, String> {
        val configProps = HashMap<String, Any>().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.ACKS_CONFIG, acksConfig)
            if (idempotentProducerEnabled) {
                put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
                // 멱등성 프로듀서는 retries, max.in.flight.requests.per.connection 등 자동 설정
            }
        }
        return KafkaTemplate(DefaultKafkaProducerFactory(configProps))
    }

    // EventHandlerRegistry
    @Bean
    @ConditionalOnMissingBean(EventHandlerRegistry::class)
    fun coreEventHandlerRegistry(
        handlers: List<EventHandlingUseCase<*, *, *>> // 서비스에서 정의한 핸들러 포함
    ): EventHandlerRegistry {
        return EventHandlerRegistry(handlers)
    }

    // OutboxEventPublisher
    @Bean
    @ConditionalOnMissingBean(OutboxEventPublisher::class)
    fun coreOutboxEventPublisher(
        applicationEventPublisher: ApplicationEventPublisher,
        dataSerializer: DataSerializer,
        eventIdGenerator: SnowflakeGenerator
    ): OutboxEventPublisher {
        return OutboxEventPublisher(applicationEventPublisher, dataSerializer, eventIdGenerator)
    }

    // DLTConsumer (JPA 의존성 있는 경우)
    @Bean
    @ConditionalOnMissingBean(DLTConsumer::class)
    @ConditionalOnClass(DeadLetterTopicRepository::class) // JPA DLT 저장소 클래스 존재 여부
    @ConditionalOnProperty(name = ["jobstat.core.event.dlt.consumer.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreDltConsumer(
        deadLetterTopicRepository: DeadLetterTopicRepository, // 자동 주입 (core-event-jpa-persistence)
        objectMapper: ObjectMapper, // 자동 주입 (core-serializer)
    ): DLTConsumer {
        // DLTConsumer 클래스 내 @KafkaListener의 SpEL이 이 값들을 사용하도록 설정
        // 또는, DLTConsumer 생성자에 이 값들을 넘겨주고, @KafkaListener에서 프로퍼티 직접 참조
        return DLTConsumer(
            deadLetterTopicRepository,
            objectMapper,
        )
    }

    // OutboxProcessor (JPA 의존성 있는 경우)
    @Bean
    @ConditionalOnMissingBean(OutboxProcessor::class)
    @ConditionalOnClass(OutboxRepository::class) // JPA Outbox 저장소 클래스 존재 여부
    @ConditionalOnProperty(name = ["jobstat.core.event.outbox.processor.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreOutboxProcessor(
        outboxRepository: OutboxRepository, // 자동 주입 (core-event-jpa-persistence)
        @Qualifier("kafkaTemplate") outboxKafkaTemplate: KafkaTemplate<String, String>,
        objectMapper: ObjectMapper,
        @Value("\${jobstat.core.event.outbox.processor.kafka-send-timeout-seconds:5}") kafkaSendTimeoutSeconds: Long,
        @Value("\${jobstat.core.event.outbox.processor.max-retry-count:3}") maxRetryCount: Int,
        @Value("\${jobstat.core.event.kafka.consumer.common.dlt-suffix:.DLT}") dltSuffix: String
    ): OutboxProcessor {
        return OutboxProcessor(
            outboxRepository, outboxKafkaTemplate, objectMapper,
            kafkaSendTimeoutSeconds, maxRetryCount, dltSuffix
        )
    }

    // OutboxMessageRelay (JPA 의존성 있는 경우)
    @Bean
    @ConditionalOnMissingBean(OutboxMessageRelay::class)
    @ConditionalOnBean(
        OutboxRepository::class,
        KafkaTemplate::class, // 또는 @Qualifier("outboxKafkaTemplate") KafkaTemplate::class
        OutboxProcessor::class,
        CoroutineScope::class // 또는 @Qualifier("coreCoroutineScope") CoroutineScope::class
    )
    @ConditionalOnProperty(name = ["jobstat.core.event.outbox.relay.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreOutboxMessageRelay(
        outboxRepository: OutboxRepository,
        @Qualifier("kafkaTemplate") outboxKafkaTemplate: KafkaTemplate<String, String>, // 이름이 여러 개 있을 수 있으므로 명시
        outboxProcessor: OutboxProcessor,
        @Qualifier("coreCoroutineScope") coroutineScope: CoroutineScope, // 명시적으로 지정
        @Value("\${jobstat.core.event.outbox.relay.kafka-send-timeout-seconds:3}") kafkaSendTimeoutSeconds: Long,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.cutoff-seconds:10}") schedulerCutoffSeconds: Long,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.batch-size:100}") schedulerBatchSize: Int,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.max-retry-count:3}") maxRetryCountForScheduler: Int,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.enabled:true}") schedulerEnabled: Boolean
    ): OutboxMessageRelay {
        return OutboxMessageRelay(
            outboxRepository,
            outboxKafkaTemplate,
            outboxProcessor,
            coroutineScope, // 주입된 CoroutineScope 사용
            kafkaSendTimeoutSeconds,
            schedulerCutoffSeconds,
            schedulerBatchSize,
            maxRetryCountForScheduler,
            schedulerEnabled
        )
    }
}