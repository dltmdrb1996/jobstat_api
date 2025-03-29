package com.example.jobstat.core.outbox

data class OutboxEvent(val outbox: Outbox) {
    companion object {
        fun of(outbox: Outbox): OutboxEvent = OutboxEvent(outbox)
    }
}