package com.example.jobstat.core.global.utils

import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.slf4j.Logger

/**
 * Redis 작업의 예외 처리 및 재시도 로직을 캡슐화한 유틸리티 클래스
 */
object RedisOperationUtils {
    const val MAX_RETRY_COUNT = 3
    const val RETRY_DELAY_MS = 100L

    /**
     * Redis 작업을 실행하고 예외 발생 시 재시도하는 함수
     *
     * @param logger 로깅을 위한 Logger
     * @param operationName 작업 이름 (로그 출력용)
     * @param detailInfo 상세 정보 (로그 및 예외 메시지용)
     * @param errorCode 예외 발생 시 사용할 에러 코드
     * @param operation 실행할 Redis 작업
     * @return T 작업 결과
     * @throws AppException 최대 재시도 횟수 초과 시 발생
     */
    inline fun <T> executeWithRetry(
        logger: Logger,
        operationName: String,
        detailInfo: String,
        errorCode: ErrorCode,
        operation: () -> T,
    ): T {
        var lastException: Exception? = null
        var retryCount = 0

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                retryCount++

                logger.warn(
                    "Redis 작업 실패 (재시도 {}/{}): {} - {}. 원인: {}",
                    retryCount,
                    MAX_RETRY_COUNT,
                    operationName,
                    detailInfo,
                    e.message,
                )

                if (retryCount < MAX_RETRY_COUNT) {
                    Thread.sleep(RETRY_DELAY_MS * retryCount)
                }
            }
        }

        // 최대 재시도 횟수 초과 시 예외 발생
        logger.error(
            "Redis 작업 최종 실패 (최대 재시도 횟수 초과): {} - {}",
            operationName,
            detailInfo,
            lastException,
        )

        throw AppException.fromErrorCode(
            errorCode,
            message = "$operationName 실패",
            detailInfo = detailInfo,
        )
    }

    /**
     * 현재 시각을 공통 포맷으로 가져오는 유틸리티 메서드
     * 모든 타임스탬프는 이 메서드를 통해 생성하여 일관성 확보
     *
     * @return Long 밀리초 단위의 타임스탬프
     */
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    /**
     * 자정까지 남은 시간(초)을 계산
     *
     * @return Long 자정까지 남은 시간(초)
     */
    fun calculateSecondsUntilMidnight(): Long {
        val now = java.time.LocalDateTime.now()
        val tomorrow =
            java.time.LocalDate
                .now()
                .plusDays(1)
                .atStartOfDay()
        return java.time.Duration
            .between(now, tomorrow)
            .seconds
    }
}
