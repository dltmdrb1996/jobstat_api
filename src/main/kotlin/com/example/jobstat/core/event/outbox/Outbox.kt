package com.example.jobstat.core.event.outbox

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import com.example.jobstat.core.base.AuditableEntity
import com.example.jobstat.core.base.BaseIdEntity
import com.example.jobstat.core.event.EventType
import java.time.LocalDateTime

@Table(name = "outbox")
@Entity
class Outbox(
    override val id: Long,
    @Enumerated(EnumType.STRING) val eventType: EventType,
    val payload: String,
    val shardKey: Long
) : BaseIdEntity() {
    companion object {
        fun create(outboxId: Long, eventType: EventType, payload: String, shardKey: Long): Outbox =
            Outbox(outboxId, eventType, payload, shardKey)
    }
}