package com.example.jobstat.core.event

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.serializer.DataSerializer

/**
 * 이벤트 클래스
 * DataSerializer 확장 함수 활용 및 Kotlin 스타일 적용
 */
data class Event<T : EventPayload>(
    val eventId: Long,
    val type: EventType,
    val payload: T,
) {
    // ===================================================
    // 직렬화 관련 메소드
    // ===================================================

    /**
     * 이벤트를 JSON 문자열로 직렬화합니다.
     * @param serializer 직렬화기
     * @return 직렬화된 JSON 문자열
     * @throws AppException 직렬화 실패 시 발생
     */
    fun toJson(serializer: DataSerializer): String = serializer.serialize(this) ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

    // ===================================================
    // 정적 팩토리 및 역직렬화 메소드
    // ===================================================

    companion object {
        /**
         * 이벤트 생성 팩토리 함수 (타입 안전성 강화)
         *
         * @param eventId 이벤트 ID
         * @param type 이벤트 타입
         * @param payload 이벤트 페이로드
         * @return 생성된 이벤트 객체
         */
        inline fun <reified P : EventPayload> of(
            eventId: Long,
            type: EventType,
            payload: P,
        ): Event<P> = Event(eventId, type, payload)

        /**
         * JSON 문자열을 이벤트 객체로 역직렬화합니다.
         *
         * @param json JSON 문자열
         * @param serializer 직렬화기
         * @return 역직렬화된 이벤트 객체
         * @throws AppException 역직렬화 실패 시 발생
         */
        fun fromJson(
            json: String,
            serializer: DataSerializer,
        ): Event<EventPayload> {
            // 1. JSON을 EventRaw로 역직렬화
            val eventRaw =
                serializer.deserialize(json, EventRaw::class.java)
                    ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

            // 2. 이벤트 타입 변환
            val eventType =
                EventType.from(eventRaw.type)
                    ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

            // 3. 페이로드 역직렬화
            val payload =
                serializer.deserialize(eventRaw.payload, eventType.payloadClass)
                    ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

            // 4. 최종 이벤트 객체 생성
            return Event(eventRaw.eventId, eventType, payload)
        }
    }
}
