package com.example.jobstat.core.event.outbox.relay

import com.example.jobstat.core.event.outbox.Outbox
import com.example.jobstat.core.event.outbox.OutboxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
class OutboxMessageRelay(
    private val outboxRepository: OutboxRepository,
//    private val outboxCoordinator: OutboxCoordinator,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>
) {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveOutbox(outbox: Outbox) {
        outboxRepository.save(outbox)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishEvent(outbox: Outbox) {
        coroutineScope.launch {
            publishEventExecute(outbox)
        }
    }

    private fun publishEventExecute(outbox: Outbox) {
        try {
            outboxKafkaTemplate.send(
                outbox.eventType.topic,
                outbox.payload
            ).get(1, TimeUnit.SECONDS)
            
            outboxRepository.delete(outbox)
        } catch (e: Exception) {
            log.error("[OutboxMessageRelay.publishEvent] outbox={}", outbox, e)
            throw RuntimeException(e)
        }
    }

    @Scheduled(
        fixedDelay = 10,
        initialDelay = 5,
        timeUnit = TimeUnit.SECONDS
    )
    fun publishPendingEvents() {
        log.info("[OutboxMessageRelay.publishPendingEvents]")
        val outboxes = outboxRepository.findAllByShardKeyAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            LocalDateTime.now().minusSeconds(10),
            Pageable.ofSize(100)
        )
        outboxes.forEach { outbox ->
            publishEvent(outbox)
        }
    }
}