package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import org.slf4j.LoggerFactory

abstract class EventHandlingUseCase<T : EventType, P : EventPayload, R> {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    abstract val eventType: T

    operator fun invoke(event: Event<out EventPayload>): R {
        if (event.type != eventType) {
            throw IllegalArgumentException("이벤트 타입이 일치하지 않습니다: expected=$eventType, actual=${event.type}")
        }
        @Suppress("UNCHECKED_CAST")
        val typedPayload = event.payload as P
        validatePayload(typedPayload)
        return execute(typedPayload)
    }

    protected open fun validatePayload(payload: P) {}

    protected abstract fun execute(payload: P): R
}
