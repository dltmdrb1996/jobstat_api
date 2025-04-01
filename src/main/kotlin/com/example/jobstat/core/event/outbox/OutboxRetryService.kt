//package com.example.jobstat.core.outbox
//
//import com.example.jobstat.core.event.consumer.EventProcessingStatusRepository
//import org.slf4j.LoggerFactory
//import org.springframework.scheduling.annotation.Scheduled
//import org.springframework.stereotype.Service
//import java.time.LocalDateTime
//
///**
// * Outbox 재시도 서비스
// * 처리되지 않은 아웃박스 이벤트를 주기적으로 재시도
// */
//@Service
//class OutboxRetryService(
//    private val outboxRepository: OutboxRepository,
//    private val outboxEventPublisher: OutboxEventPublisher,
//    private val eventProcessingStatusRepository: EventProcessingStatusRepository
//) {
//    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
//
//    /**
//     * 처리되지 않은 아웃박스 이벤트를 주기적으로 재시도
//     */
//    @Scheduled(fixedDelay = 60000) // 1분마다 실행
//    fun retryUnprocessedOutboxEvents() {
//        try {
//            log.info("미처리 아웃박스 이벤트 재시도 작업 시작")
//
//            // 일정 시간 이상 처리되지 않은 이벤트 조회
//            val cutoffTime = LocalDateTime.now().minusMinutes(5)
//            val unprocessedEvents = outboxRepository.findUnprocessedEventsOlderThan(cutoffTime)
//
//            if (unprocessedEvents.isEmpty()) {
//                log.info("재시도할 미처리 아웃박스 이벤트 없음")
//                return
//            }
//
//            log.info("재시도할 미처리 아웃박스 이벤트 수: {}", unprocessedEvents.size)
//
//            // 이벤트 재시도
//            unprocessedEvents.forEach { outbox ->
//                try {
//                    outboxEventPublisher.publishOutboxEvent(outbox)
//                    log.info("아웃박스 이벤트 재시도 성공: id={}, eventId={}",
//                        outbox.id, outbox.eventId)
//                } catch (e: Exception) {
//                    log.error("아웃박스 이벤트 재시도 실패: id={}, eventId={}",
//                        outbox.id, outbox.eventId, e)
//                }
//            }
//
//            log.info("미처리 아웃박스 이벤트 재시도 작업 완료")
//        } catch (e: Exception) {
//            log.error("미처리 아웃박스 이벤트 재시도 작업 중 오류 발생", e)
//        }
//    }
//
//    /**
//     * 실패한 이벤트 처리를 재시도
//     */
//    @Scheduled(fixedDelay = 300000) // 5분마다 실행
//    fun retryFailedEventProcessing() {
//        try {
//            log.info("실패한 이벤트 처리 재시도 작업 시작")
//
//            // 실패했지만 최대 재시도 횟수에 도달하지 않은 이벤트 조회
//            val failedEvents = eventProcessingStatusRepository
//                .findByProcessedFalseAndRetryCountLessThan(3)
//
//            if (failedEvents.isEmpty()) {
//                log.info("재시도할 실패한 이벤트 없음")
//                return
//            }
//
//            log.info("재시도할 실패한 이벤트 수: {}", failedEvents.size)
//
//            // 이벤트 ID로 아웃박스 이벤트 조회 및 재발행
//            failedEvents.forEach { status ->
//                val outboxOptional = outboxRepository.findByEventId(status.eventId)
//                if (outboxOptional.isPresent) {
//                    try {
//                        outboxEventPublisher.publishOutboxEvent(outboxOptional.get())
//                        log.info("이벤트 처리 재시도 성공: eventId={}", status.eventId)
//                    } catch (e: Exception) {
//                        log.error("이벤트 처리 재시도 실패: eventId={}", status.eventId, e)
//                    }
//                } else {
//                    log.warn("재시도할 아웃박스 이벤트를 찾을 수 없음: eventId={}", status.eventId)
//                }
//            }
//
//            log.info("실패한 이벤트 처리 재시도 작업 완료")
//        } catch (e: Exception) {
//            log.error("실패한 이벤트 처리 재시도 작업 중 오류 발생", e)
//        }
//    }
//}