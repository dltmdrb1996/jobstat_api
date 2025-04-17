package com.example.jobstat.core.event

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.serializer.DataSerializer

data class Event<T : EventPayload>(
    val eventId: Long,
    val type: EventType,
    val payload: T,
) {
    fun toJson(serializer: DataSerializer): String = serializer.serialize(this) ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

    companion object {
        inline fun <reified P : EventPayload> of(
            eventId: Long,
            type: EventType,
            payload: P,
        ): Event<P> = Event(eventId, type, payload)

        fun fromJson(
            json: String,
            serializer: DataSerializer,
        ): Event<EventPayload> {
            val eventRaw =
                serializer.deserialize(json, EventRaw::class.java)
                    ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

            val eventType =
                EventType.from(eventRaw.type)
                    ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

            val payload =
                serializer.deserialize(eventRaw.payload, eventType.payloadClass)
                    ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)

            return Event(eventRaw.eventId, eventType, payload)
        }
    }
}
