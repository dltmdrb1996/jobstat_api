package com.example.jobstat.core.event.dlt

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * 데드 레터 토픽 저장소
 * 실패한
 * 이벤트를 저장하고 조회하는 기능을 제공합니다.
 */
@Repository
interface DeadLetterTopicRepository : JpaRepository<DeadLetterTopicEvent, Long> {
    // ===================================================
    // 조회 관련 메소드
    // ===================================================

    /**
     * 이벤트 ID로 데드 레터 토픽 이벤트를 조회합니다.
     * @param eventId 이벤트 ID
     * @return 데드 레터 토픽 이벤트 또는 null
     */
    fun findByEventId(eventId: String): DeadLetterTopicEvent?

    /**
     * 이벤트 타입으로 데드 레터 토픽 이벤트 목록을 조회합니다.
     * @param eventType 이벤트 타입
     * @return 데드 레터 토픽 이벤트 목록
     */
    fun findByEventType(eventType: String): List<DeadLetterTopicEvent>

    /**
     * 고유한 이벤트 타입 목록을 조회합니다.
     * @return 고유한 이벤트 타입 목록
     */
    @Query("SELECT DISTINCT e.eventType FROM DeadLetterTopicEvent e")
    fun findDistinctEventTypes(): List<String>
}
