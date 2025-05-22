package com.wildrew.jobstat.core.core_event.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_coroutine.CoroutineModuleConfig
import com.wildrew.jobstat.core.core_event.consumer.EventHandlerRegistry
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.dlt.DLTConsumer
import com.wildrew.jobstat.core.core_event.dlt.DeadLetterTopicRepository
import com.wildrew.jobstat.core.core_event.outbox.OutboxEventPublisher
import com.wildrew.jobstat.core.core_event.outbox.OutboxMessageRelay
import com.wildrew.jobstat.core.core_event.outbox.OutboxProcessor
import com.wildrew.jobstat.core.core_event.outbox.OutboxRepository
import com.wildrew.jobstat.core.core_jpa_base.id_generator.CoreIdGeneratorAutoConfiguration
import com.wildrew.jobstat.core.core_jpa_base.id_generator.SnowflakeGenerator
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import com.wildrew.jobstat.core.core_serializer.config.CoreSerializerAutoConfiguration
import kotlinx.coroutines.CoroutineScope
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory // 이 import가 있는지 확인
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
@ConditionalOnClass(KafkaOperations::class) // KafkaOperations 클래스가 클래스패스에 있을 때
@ConditionalOnProperty(name = ["spring.kafka.bootstrap-servers"]) // Kafka bootstrap 서버 설정이 있을 때
@ConditionalOnBean(ConsumerFactory::class) // ConsumerFactory 빈이 컨텍스트에 존재할 때 (핵심 추가)
class CoreEventAutoConfiguration {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Bean("coreKafkaListenerContainerFactory")
    @Primary
    @ConditionalOnMissingBean(name = ["coreKafkaListenerContainerFactory"])
    fun coreKafkaListenerContainerFactory(
        // 이 메소드가 호출될 시점에는 ConsumerFactory 빈이 이미 존재함이 클래스 레벨 @ConditionalOnBean에 의해 보장됨
        consumerFactory: ConsumerFactory<String?, String?>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactory
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        }
    }

    // Outbox용 KafkaTemplate
    // 이 KafkaTemplate은 ConsumerFactory에 직접 의존하지 않으므로,
    // @ConditionalOnBean(ConsumerFactory::class)가 클래스 레벨에 있어도 괜찮습니다.
    // 만약 이 빈만 별도로 ConsumerFactory 없이 동작해야 한다면 클래스 레벨 조건을 제거하고 각 빈에 개별 조건을 달아야 합니다.
    // 하지만 보통 Kafka 관련 설정은 함께 움직입니다.
    @Bean("outboxKafkaTemplate")
    @Primary
    fun outboxKafkaTemplate(
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
            }
        }
        return KafkaTemplate(DefaultKafkaProducerFactory(configProps))
    }

    // EventHandlerRegistry
    @Bean
    @ConditionalOnMissingBean(EventHandlerRegistry::class)
    fun coreEventHandlerRegistry(
        handlers: List<EventHandlingUseCase<*, *, *>>
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
        log.info(
            "[{}] OutboxEventPublisher 빈 생성: applicationEventPublisher={}, dataSerializer={}, eventIdGenerator={}",
            this::class.simpleName, applicationEventPublisher, dataSerializer, eventIdGenerator
        )
        return OutboxEventPublisher(applicationEventPublisher, dataSerializer, eventIdGenerator)
    }

    // DLTConsumer (JPA 의존성 있는 경우)
    @Bean
    @ConditionalOnMissingBean(DLTConsumer::class)
    @ConditionalOnClass(DeadLetterTopicRepository::class) // DLT Repo 클래스 존재 여부
    @ConditionalOnProperty(name = ["jobstat.core.event.dlt.consumer.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreDltConsumer(
        deadLetterTopicRepository: DeadLetterTopicRepository, // @EnableJpaRepositories 덕분에 주입 가능
        objectMapper: ObjectMapper,
    ): DLTConsumer {
        return DLTConsumer(
            deadLetterTopicRepository,
            objectMapper,
        )
    }

    // OutboxProcessor (JPA 의존성 있는 경우)
    @Bean
    @ConditionalOnMissingBean(OutboxProcessor::class)
    @ConditionalOnClass(OutboxRepository::class) // Outbox Repo 클래스 존재 여부
    @ConditionalOnProperty(name = ["jobstat.core.event.outbox.processor.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreOutboxProcessor(
        outboxRepository: OutboxRepository, // @EnableJpaRepositories 덕분에 주입 가능
        outboxKafkaTemplate: KafkaTemplate<String, String>,
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
    @ConditionalOnBean( // 필요한 모든 빈들이 존재할 때만 OutboxMessageRelay 빈 생성
        OutboxRepository::class,
        KafkaTemplate::class, // 위에서 kafkaTemplate 빈이 생성됨
        OutboxProcessor::class,
        CoroutineScope::class // CoroutineModuleConfig에서 coreCoroutineScope 빈이 생성되어야 함
    )
    @ConditionalOnProperty(name = ["jobstat.core.event.outbox.relay.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreOutboxMessageRelay(
        outboxRepository: OutboxRepository, // @EnableJpaRepositories 덕분에 주입 가능
        outboxKafkaTemplate: KafkaTemplate<String, String>,
        outboxProcessor: OutboxProcessor,
        @Qualifier("coreCoroutineScope") coroutineScope: CoroutineScope, // CoroutineModuleConfig에서 주입
        @Value("\${jobstat.core.event.outbox.relay.kafka-send-timeout-seconds:3}") kafkaSendTimeoutSeconds: Long,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.cutoff-seconds:10}") schedulerCutoffSeconds: Long,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.batch-size:100}") schedulerBatchSize: Int,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.max-retry-count:3}") maxRetryCountForScheduler: Int,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.enabled:true}") schedulerEnabled: Boolean
    ): OutboxMessageRelay {

        log.info(
            "[{}] OutboxMessageRelay 빈 생성: outboxRepository={}, outboxKafkaTemplate={}, outboxProcessor={}, coroutineScope={}, kafkaSendTimeoutSeconds={}, schedulerCutoffSeconds={}, schedulerBatchSize={}, maxRetryCountForScheduler={}, schedulerEnabled={}",
            this::class.simpleName,
            outboxRepository,
            outboxKafkaTemplate,
            outboxProcessor,
            coroutineScope,
            kafkaSendTimeoutSeconds,
            schedulerCutoffSeconds,
            schedulerBatchSize,
            maxRetryCountForScheduler,
            schedulerEnabled
        )
        return OutboxMessageRelay(
            outboxRepository,
            outboxKafkaTemplate,
            outboxProcessor,
            coroutineScope,
            kafkaSendTimeoutSeconds,
            schedulerCutoffSeconds,
            schedulerBatchSize,
            maxRetryCountForScheduler,
            schedulerEnabled
        )
    }
}