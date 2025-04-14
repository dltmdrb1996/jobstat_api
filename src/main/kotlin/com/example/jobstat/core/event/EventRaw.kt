package com.example.jobstat.core.event

/**
 * 이벤트 원시 데이터 클래스
 * JSON 직렬화/역직렬화 과정에서 중간 형태로 사용됩니다.
 */
data class EventRaw(
    val eventId: Long, // 이벤트 고유 식별자
    val type: String, // 이벤트 타입 (문자열)
    val payload: Any, // 이벤트 페이로드 (역직렬화 전 원시 형태)
)
