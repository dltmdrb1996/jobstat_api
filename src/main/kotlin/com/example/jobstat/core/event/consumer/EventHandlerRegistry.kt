package com.example.jobstat.core.event.consumer

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import com.example.jobstat.core.event.publisher.DLQPublisher
import com.example.jobstat.core.usecase.impl.EventHandlingUseCase
import org.slf4j.LoggerFactory
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

@Component
class EventHandlerRegistry(
    private val handlers: List<EventHandlingUseCase<*, *, *>>,
    private val retryTemplate: RetryTemplate,
    private val dlqPublisher: DLQPublisher
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val handlerMap: MutableMap<EventType, MutableList<EventHandlingUseCase<*, *, *>>> = mutableMapOf()

    @PostConstruct
    fun init() {
        handlers.forEach { handler ->
            registerHandler(handler)
        }
        logRegisteredHandlers()
    }

    private fun registerHandler(handler: EventHandlingUseCase<*, *, *>) {
        val eventType = handler.eventType
        val handlerList = handlerMap.getOrPut(eventType) { mutableListOf() }
        handlerList.add(handler)
        log.info("이벤트 핸들러 등록: type={}, handler={}", eventType, handler.javaClass.simpleName)
    }

    fun getHandlersForEventType(eventType: EventType): List<EventHandlingUseCase<*, *, *>> {
        return handlerMap[eventType] ?: emptyList()
    }

    fun getSupportedEventTypes(): Set<EventType> {
        return handlerMap.keys
    }

    /**
     * 이벤트를 처리할 때 각 핸들러에 대해 재시도 수행 후, 실패 시 DLQ 전송
     */
    fun processEvent(event: Event<out EventPayload>) {
        val eventType = event.type
        val targetHandlers = getHandlersForEventType(eventType)

        if (targetHandlers.isEmpty()) {
            log.warn("이벤트 타입 {}에 대한 핸들러가 없습니다", eventType)
            return
        }

        log.debug("이벤트 타입 {}에 대한 {}개 핸들러 실행", eventType, targetHandlers.size)

        targetHandlers.forEach { handler ->
            try {
                retryTemplate.execute<Unit, Exception> { _ ->
                    handler.invoke(event)
                    null
                }
            } catch (e: Exception) {
                log.error("핸들러 {} 실행 중 재시도 후에도 오류 발생: {}", handler.javaClass.simpleName, e.message, e)
                // DLQ로 전송 (이벤트 및 실패한 핸들러 정보를 포함)
                dlqPublisher.publishToDLQ(event, handler)
            }
        }
    }

    private fun logRegisteredHandlers() {
        log.info("총 {}개 이벤트 타입에 대한 핸들러가 등록되었습니다", handlerMap.size)
        handlerMap.forEach { (type, handlers) ->
            log.info("이벤트 타입 {}: {}개 핸들러", type, handlers.size)
            handlers.forEach { handler ->
                log.debug("- {}", handler.javaClass.simpleName)
            }
        }
    }
}
