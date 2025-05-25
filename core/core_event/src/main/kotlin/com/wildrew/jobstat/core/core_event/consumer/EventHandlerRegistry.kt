package com.wildrew.jobstat.core.core_event.consumer

import com.wildrew.jobstat.core.core_event.model.Event
import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_event.model.EventType
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory

interface EventHandlerRegistryService {
    fun getHandlersForEventType(eventType: EventType): List<EventHandlingUseCase<*, *, *>>

    fun getSupportedEventTypes(): Set<EventType>
}

class EventHandlerRegistry(
    private val handlers: List<EventHandlingUseCase<*, *, *>>,
) : EventHandlerRegistryService {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val handlerMap: MutableMap<EventType, MutableList<EventHandlingUseCase<*, *, *>>> = mutableMapOf()

    @PostConstruct
    fun init() {
        log.debug("이벤트 핸들러 레지스트리 초기화 시작: 총 ${handlers.size}개 핸들러 검색됨")
        handlers.forEach { handler ->
            registerHandler(handler)
        }
        logRegisteredHandlers()
    }

    private fun registerHandler(handler: EventHandlingUseCase<*, *, *>) {
        try {
            val eventType = handler.eventType
            val handlerList = handlerMap.getOrPut(eventType) { mutableListOf() }
            if (!handlerList.contains(handler)) {
                handlerList.add(handler)
                log.debug("이벤트 핸들러 등록: type=$eventType, handler=${handler.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            log.error("핸들러 등록 중 오류 발생: handler=${handler.javaClass.simpleName}, error=${e.message}", e)
        }
    }

    override fun getHandlersForEventType(eventType: EventType): List<EventHandlingUseCase<*, *, *>> {
        val foundHandlers = handlerMap[eventType] ?: emptyList()
        log.debug("이벤트 타입 ${eventType}에 대한 핸들러 조회: ${foundHandlers.size}개 발견")
        return foundHandlers
    }

    override fun getSupportedEventTypes(): Set<EventType> = handlerMap.keys

    internal fun processEvent(event: Event<out EventPayload>) { // internal로 변경
        val eventType = event.type
        val eventId = event.eventId
        val targetHandlers = getHandlersForEventType(eventType)

        log.debug(
            "이벤트 처리 위임 시작: eventId=$eventId, type=$eventType, 등록된 핸들러=${targetHandlers.size}개",
        )

        if (targetHandlers.isEmpty()) {
            log.warn("처리 위임 단계에서 이벤트 타입 ${eventType}에 대한 핸들러를 찾을 수 없음: eventId=$eventId")
            return
        }

        log.debug("이벤트 타입 ${eventType}에 대한 ${targetHandlers.size}개 핸들러 순차 실행 시작")

        var success = true
        targetHandlers.forEach { handler ->
            val handlerName = handler.javaClass.simpleName
            log.debug("핸들러 실행 시도: eventId=$eventId, type=$eventType, handler=$handlerName")
            val startTime = System.currentTimeMillis()

            try {
                @Suppress("UNCHECKED_CAST")
                (handler as EventHandlingUseCase<EventType, EventPayload, Any>).invoke(event as Event<EventPayload>)

                val elapsedTime = System.currentTimeMillis() - startTime
                log.debug(
                    "핸들러 실행 성공: eventId=$eventId, type=$eventType, handler=$handlerName, 소요시간=${elapsedTime}ms",
                )
            } catch (e: Exception) {
                val elapsedTime = System.currentTimeMillis() - startTime
                log.error(
                    "핸들러 실행 실패: eventId=$eventId, type=$eventType, handler=$handlerName, 소요시간=${elapsedTime}ms. 예외 전파됨.",
                    e,
                )
                success = false
                throw e
            }
        }
        if (success) {
            log.debug("이벤트에 대한 모든 핸들러 성공적으로 실행 완료: eventId=$eventId, type=$eventType")
        }
    }

    private fun logRegisteredHandlers() {
        log.debug("--- 이벤트 핸들러 등록 완료 ---")
        if (handlerMap.isEmpty()) {
            log.warn("등록된 이벤트 핸들러가 없습니다.")
            return
        }
        log.debug("총 ${handlerMap.size}개 이벤트 타입에 대한 핸들러 등록됨:")
        handlerMap.entries.sortedBy { it.key.name }.forEach { (type, handlers) ->
            log.debug("  이벤트 타입 [$type]: ${handlers.size}개 핸들러")
            handlers.forEach { handler ->
                log.debug("    - ${handler.javaClass.name}")
            }
        }
        log.debug("--- 핸들러 레지스트리 초기화 완료 ---")
    }
}
