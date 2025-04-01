package com.example.jobstat.core.event.outbox.relay

import com.example.jobstat.core.event.outbox.Outbox
import com.example.jobstat.core.event.outbox.OutboxEvent
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
    private val outboxCoordinator: OutboxCoordinator,
    private val outboxKafkaTemplate: KafkaTemplate<String, String>
) {
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveOutbox(outboxEvent: OutboxEvent) {
        outboxRepository.save(outboxEvent.outbox)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishEvent(outboxEvent: OutboxEvent) {
        coroutineScope.launch {
            publishEvent(outboxEvent.outbox)
        }
    }

    private fun publishEvent(outbox: Outbox) {
        try {
            outboxKafkaTemplate.send(
                outbox.eventType.topic,
                outbox.shardKey.toString(),
                outbox.payload
            ).get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            log.error("[OutboxMessageRelay.publishEvent] outbox={}", outbox, e)
            throw RuntimeException(e)
        }
        outboxRepository.delete(outbox)
    }

    @Scheduled(
        fixedDelay = 10,
        initialDelay = 5,
        timeUnit = TimeUnit.SECONDS
    )
    fun publishPendingEvents() {
        val assignedShard = outboxCoordinator.assignShards()
        log.info("[OutboxMessageRelay.publishPendingEvents] assignedShardSize={}", assignedShard.shards.size)

        assignedShard.shards.forEach { shard ->
            val outboxes = outboxRepository.findAllByShardKeyAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                shard,
                LocalDateTime.now().minusSeconds(10),
                Pageable.ofSize(100)
            )
            outboxes.forEach { outbox ->
                publishEvent(outbox)
            }
        }
    }
}