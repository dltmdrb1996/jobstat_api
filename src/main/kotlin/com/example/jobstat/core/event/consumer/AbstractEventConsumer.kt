package com.example.jobstat.core.event.consumer

import EventHandlerRegistry
import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * 제네릭 타입 기반 이벤트 컨슈머 추상 클래스
 * 전략 패턴 구현을 위한 컨슈머
 */
abstract class AbstractEventConsumer {
    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Autowired
    protected lateinit var handlerRegistry: EventHandlerRegistry

    /**
     * 이 컨슈머가 처리하는 토픽 반환
     */
    abstract fun getTopic(): String

    /**
     * 이 컨슈머의 그룹 ID 반환
     */
    abstract fun getGroupId(): String

    /**
     * 이 컨슈머가 지원하는 이벤트 타입 반환
     */
    open fun getSupportedEventTypes(): Set<EventType> =
        handlerRegistry.getSupportedEventTypes()

    /**
     * 이벤트 수신 및 처리 기본 메서드
     */
    protected fun consumeEvent(event: Event<out EventPayload>) {
        val eventType = event.type

        log.info(
            "[{}] 이벤트 수신: type={}, topic={}",
            this::class.simpleName, eventType, getTopic()
        )

        // 이벤트 타입이 지원되는지 확인
        if (eventType !in getSupportedEventTypes()) {
            log.warn("[{}] 지원하지 않는 이벤트 타입: {}", this::class.simpleName, eventType)
            return
        }

        // 전략 패턴 - 이벤트 처리를 핸들러 레지스트리에 위임
        handlerRegistry.processEvent(event)
    }
}