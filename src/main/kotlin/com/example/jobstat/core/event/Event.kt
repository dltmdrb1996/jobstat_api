package com.example.jobstat.core.event

import com.example.jobstat.core.base.BaseIdEntity
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Id

/**
 * 이벤트 클래스
 * DataSerializer 확장 함수 활용 및 Kotlin 스타일 적용
 */
@Entity
data class Event<T : EventPayload>(
    override val id: Long,
    @Enumerated
    val type: EventType,
    val payload: T
) : BaseIdEntity() {
    /**
     * 이벤트를 JSON 문자열로 직렬화 (직렬화기를 받아서 사용)
     * @return 직렬화된 JSON 문자열
     * @throws SerializationException 직렬화 실패 시 발생
     */
    fun toJson(serializer: DataSerializer): String =
        serializer.serialize(this) ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

    companion object {
        /**
         * 이벤트 생성 팩토리 함수 (타입 안전성 강화)
         */
        inline fun <reified P : EventPayload> of(eventId: Long, type: EventType, payload: P): Event<P> {
            return Event(eventId, type, payload)
        }
    }
}
