package com.example.jobstat.core.event

import com.example.jobstat.core.global.utils.serializer.DataSerializer
import com.example.jobstat.core.global.utils.serializer.ObjectMapperDataSerializer
import com.example.jobstat.core.global.utils.serializer.deserializeAs
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class EventHelper(
    private val objectMapper: ObjectMapper,
    private val serializer: DataSerializer
) {
    // 이벤트 타입 변환 캐시
    private val eventTypeCache = ConcurrentHashMap<String, EventType?>()

    // 자주 사용되는 EventRaw 클래스에 대한 타입 정보 캐싱 (Jackson 내부 사용)
    private val rawTypeRef by lazy {
        if (serializer is ObjectMapperDataSerializer) {
            objectMapper.typeFactory.constructType(EventRaw::class.java)
        } else null
    }

    /**
     * JSON 문자열에서 이벤트 객체 생성
     * @param json 역직렬화할 JSON 문자열
     * @return 역직렬화된 이벤트 객체 또는 실패 시 null
     */
    fun fromJson(json: String): Event<out EventPayload>? {
        try {
            // 1. EventRaw 객체로 역직렬화 (캐싱된 타입 정보 활용)
            val eventRaw = if (serializer is ObjectMapperDataSerializer && rawTypeRef != null) {
                try {
                    objectMapper.readValue<EventRaw>(json, rawTypeRef)
                } catch (e: Exception) {
                    throw DeserializationException("이벤트 원시 데이터 역직렬화 실패", e)
                }
            } else {
                json.deserializeAs<EventRaw>(serializer)
                    ?: throw DeserializationException("이벤트 원시 데이터 역직렬화 실패")
            }

            // 2. 이벤트 타입 확인 (캐싱된 결과 활용)
            val eventType = getEventType(eventRaw.type)
                ?: throw DeserializationException("알 수 없는 이벤트 타입: ${eventRaw.type}")

            // 3. 페이로드 역직렬화
            val payload = serializer.deserialize(eventRaw.payload, eventType.payloadClass)
                ?: throw DeserializationException("이벤트 페이로드 역직렬화 실패")

            return Event(eventRaw.eventId, eventType, payload)
        } catch (e: DeserializationException) {
            // 로깅 등 추가 처리 후 null 반환
            return null
        }
    }

    /**
     * 캐싱된 이벤트 타입 조회
     */
    private fun getEventType(typeString: String): EventType? {
        return eventTypeCache.computeIfAbsent(typeString) { type ->
            EventType.from(type)
        }
    }

    /**
     * 이벤트 역직렬화 과정에서 발생하는 예외
     */
    class DeserializationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}