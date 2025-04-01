package com.example.jobstat.core.usecase.impl

import com.example.jobstat.core.event.Event
import com.example.jobstat.core.event.EventPayload
import com.example.jobstat.core.event.EventType
import org.slf4j.LoggerFactory

/**
 * 제네릭 타입 기반 이벤트 핸들링 유스케이스 추상 클래스
 * EventType을 제네릭 파라미터로 받아 전략 패턴 구현을 간소화
 */
abstract class EventHandlingUseCase<T : EventType, P : EventPayload, R> {

    protected val log by lazy { LoggerFactory.getLogger(this::class.java) }

    // 이 핸들러가 처리하는 이벤트 타입 (제네릭 타입)
    abstract val eventType: T

    /**
     * 이벤트를 처리하는 메서드
     * AbstractEventConsumer에서 호출됨
     */
    operator fun invoke(event: Event<out EventPayload>): R {
        if (event.type != eventType) {
            throw IllegalArgumentException("이벤트 타입이 일치하지 않습니다: expected=$eventType, actual=${event.type}")
        }
        @Suppress("UNCHECKED_CAST")
        val typedPayload = event.payload as P
        validatePayload(typedPayload)
        return execute(typedPayload)
    }


    /**
     * 페이로드 유효성 검사
     * 필요 시 하위 클래스에서 오버라이드
     */
    protected open fun validatePayload(payload: P) {}

    /**
     * 실제 비즈니스 로직을 실행하는 추상 메서드
     * 각 핸들러에서 구현해야 함
     */
    protected abstract fun execute(payload: P): R
}