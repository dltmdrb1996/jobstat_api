package com.wildrew.jobstat.core.core_event.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wildrew.jobstat.core.core_coroutine.CoroutineModuleConfig
import com.wildrew.jobstat.core.core_event.consumer.*
import com.wildrew.jobstat.core.core_event.dlt.DLTConsumer
import com.wildrew.jobstat.core.core_event.dlt.DeadLetterTopicRepository
import com.wildrew.jobstat.core.core_event.outbox.*
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
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.StringRedisTemplate
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
        CoroutineModuleConfig::class,
        RedisAutoConfiguration::class,
    ],
)
@ConditionalOnClass(KafkaOperations::class)
@ConditionalOnProperty(name = ["spring.kafka.bootstrap-servers"])
@ConditionalOnBean(ConsumerFactory::class)
@EnableConfigurationProperties(KafkaConsumersConfiguration::class)
class CoreEventAutoConfiguration {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Bean("coreKafkaListenerContainerFactory")
    @Primary
    @ConditionalOnMissingBean(name = ["coreKafkaListenerContainerFactory"])
    fun coreKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String?, String?>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = consumerFactory
            this.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        }

    @Bean("outboxKafkaTemplate")
    @Primary
    fun outboxKafkaTemplate(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${jobstat.core.event.kafka.producer.acks-config:all}") acksConfig: String,
        @Value("\${jobstat.core.event.kafka.producer.idempotence.enabled:true}") idempotentProducerEnabled: Boolean,
        @Value("\${jobstat.core.event.kafka.producer.transactional-id}") transactionalId: String,
    ): KafkaTemplate<String, String> {
        val configProps =
            HashMap<String, Any>().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.ACKS_CONFIG, acksConfig)
                put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
                put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId)
            }
        return KafkaTemplate(DefaultKafkaProducerFactory(configProps))
    }

    @Bean
    @ConditionalOnMissingBean(EventHandlerRegistryService::class)
    fun coreEventHandlerRegistry(
        handlers: List<EventHandlingUseCase<*, *, *>>,
    ): EventHandlerRegistryService {
        log.info("실제 EventHandlerRegistry 빈 생성: ${handlers.size}개의 핸들러와 함께.")
        return EventHandlerRegistry(handlers)
    }

    @Bean
    @ConditionalOnMissingBean(OutboxEventPublisher::class)
    fun coreOutboxEventPublisher(
        applicationEventPublisher: ApplicationEventPublisher,
        dataSerializer: DataSerializer,
        eventIdGenerator: SnowflakeGenerator,
    ): OutboxEventPublisher {
        log.info(
            "[{}] OutboxEventPublisher 빈 생성: applicationEventPublisher={}, dataSerializer={}, eventIdGenerator={}",
            this::class.simpleName,
            applicationEventPublisher,
            dataSerializer,
            eventIdGenerator,
        )
        return KafkaOutboxEventPublisher(applicationEventPublisher, dataSerializer, eventIdGenerator)
    }

    @Bean
    @ConditionalOnMissingBean(DLTConsumer::class)
    @ConditionalOnClass(DeadLetterTopicRepository::class)
    @ConditionalOnProperty(name = ["jobstat.core.event.dlt.consumer.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreDltConsumer(
        deadLetterTopicRepository: DeadLetterTopicRepository,
        objectMapper: ObjectMapper,
    ): DLTConsumer =
        DLTConsumer(
            deadLetterTopicRepository,
            objectMapper,
        )

    @Bean
    @ConditionalOnMissingBean(OutboxProcessor::class)
    @ConditionalOnClass(OutboxRepository::class)
    @ConditionalOnProperty(name = ["jobstat.core.event.outbox.processor.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreOutboxProcessor(
        outboxRepository: OutboxRepository,
        outboxKafkaTemplate: KafkaTemplate<String, String>,
        objectMapper: ObjectMapper,
        @Value("\${jobstat.core.event.outbox.processor.kafka-send-timeout-seconds:5}") kafkaSendTimeoutSeconds: Long,
        @Value("\${jobstat.core.event.outbox.processor.max-retry-count:3}") maxRetryCount: Int,
        @Value("\${jobstat.core.event.kafka.consumer.common.dlt-suffix:.DLT}") dltSuffix: String,
    ): OutboxProcessor =
        OutboxProcessor(
            outboxRepository,
            outboxKafkaTemplate,
            objectMapper,
            kafkaSendTimeoutSeconds,
            maxRetryCount,
            dltSuffix,
        )

    @Bean
    @ConditionalOnMissingBean(OutboxMessageRelay::class)
    @ConditionalOnBean(
        OutboxRepository::class,
        KafkaTemplate::class,
        OutboxProcessor::class,
        CoroutineScope::class,
    )
    @ConditionalOnProperty(name = ["jobstat.core.event.outbox.relay.enabled"], havingValue = "true", matchIfMissing = true)
    fun coreOutboxMessageRelay(
        outboxRepository: OutboxRepository,
        outboxKafkaTemplate: KafkaTemplate<String, String>,
        outboxProcessor: OutboxProcessor,
        @Qualifier("coreCoroutineScope") coroutineScope: CoroutineScope,
        @Value("\${jobstat.core.event.outbox.relay.kafka-send-timeout-seconds:3}") kafkaSendTimeoutSeconds: Long,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.cutoff-seconds:10}") schedulerCutoffSeconds: Long,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.batch-size:100}") schedulerBatchSize: Int,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.max-retry-count:3}") maxRetryCountForScheduler: Int,
        @Value("\${jobstat.core.event.outbox.relay.scheduler.enabled:true}") schedulerEnabled: Boolean,
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
            schedulerEnabled,
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
            schedulerEnabled,
        )
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyChecker::class)
    @ConditionalOnBean(StringRedisTemplate::class)
    fun redisIdempotencyChecker(
        stringRedisTemplate: StringRedisTemplate,
    ): IdempotencyChecker {
        log.info("CoreEventAutoConfiguration: RedisIdempotencyChecker 빈을 명시적으로 생성합니다.")
        return RedisIdempotencyChecker(stringRedisTemplate)
    }
}
