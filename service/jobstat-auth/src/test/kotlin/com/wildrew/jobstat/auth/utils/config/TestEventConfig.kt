package com.wildrew.jobstat.auth.utils.config

import com.wildrew.jobstat.core.core_event.consumer.EventHandlerRegistryService
import com.wildrew.jobstat.core.core_event.consumer.IdempotencyChecker
import com.wildrew.jobstat.core.core_event.outbox.OutboxEventPublisher
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory

@TestConfiguration
class TestEventConfig {
    @Bean
    @Primary
    fun mockOutboxEventPublisher(): OutboxEventPublisher {
        val mockPublisher = Mockito.mock(OutboxEventPublisher::class.java)
        return mockPublisher
    }

    @Bean
    @Primary
    fun mockConsumerFactory(): ConsumerFactory<*, *> = Mockito.mock(ConsumerFactory::class.java)

    @Bean
    @Primary
    fun mockProducerFactory(): ProducerFactory<*, *> = Mockito.mock(ProducerFactory::class.java)

    @Bean
    @Primary
    fun mockEventHandlerRegistryService(): EventHandlerRegistryService = Mockito.mock(EventHandlerRegistryService::class.java)

    @Bean
    @Primary
    fun mockIdempotencyChecker(): IdempotencyChecker = Mockito.mock(IdempotencyChecker::class.java)
}
