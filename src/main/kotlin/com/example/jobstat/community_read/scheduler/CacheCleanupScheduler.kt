package com.example.jobstat.community_read.scheduler

import com.example.jobstat.community_read.repository.RedisBoardDetailRepository
import com.example.jobstat.community_read.repository.RedisBoardIdListRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * 기간별 캐시 데이터를 정리하는 스케줄러
 * - 일간(1일) 캐시: 하루가 지난 게시글 제거
 * - 주간(7일) 캐시: 7일이 지난 게시글 제거
 * - 월간(30일) 캐시: 30일이 지난 게시글 제거
 */
@Component
@EnableScheduling
class CacheCleanupScheduler(
    private val redisTemplate: StringRedisTemplate
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
    
    /**
     * 일간 캐시 정리 (매일 새벽 1시에 실행)
     * 하루가 지난 게시글을 일간 좋아요/조회수 순위 목록에서 제거
     */
    @Scheduled(cron = "0 0 1 * * *")
    fun cleanupDailyCache() {
        try {
            log.info("[CacheCleanupScheduler] 일간 캐시 정리 시작")
            val startTime = System.currentTimeMillis()
            
            // 게시글 스캔 및 필터링
            val dayAgo = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()
            
            // 일간 좋아요 순위 정리
            cleanupExpiredElements(
                RedisBoardIdListRepository.BOARDS_BY_LIKES_DAY_KEY, 
                dayAgo,
                "일간 좋아요 순위"
            )
            
            // 일간 조회수 순위 정리
            cleanupExpiredElements(
                RedisBoardIdListRepository.BOARDS_BY_VIEWS_DAY_KEY, 
                dayAgo,
                "일간 조회수 순위"
            )
            
            val duration = System.currentTimeMillis() - startTime
            log.info("[CacheCleanupScheduler] 일간 캐시 정리 완료: 소요 시간 {}ms", duration)
        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] 일간 캐시 정리 중 오류 발생", e)
        }
    }
    
    /**
     * 주간 캐시 정리 (매주 월요일 새벽 2시에 실행)
     * 7일이 지난 게시글을 주간 좋아요/조회수 순위 목록에서 제거
     */
    @Scheduled(cron = "0 0 2 * * MON")
    fun cleanupWeeklyCache() {
        try {
            log.info("[CacheCleanupScheduler] 주간 캐시 정리 시작")
            val startTime = System.currentTimeMillis()
            
            // 게시글 스캔 및 필터링
            val weekAgo = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()
            
            // 주간 좋아요 순위 정리
            cleanupExpiredElements(
                RedisBoardIdListRepository.BOARDS_BY_LIKES_WEEK_KEY, 
                weekAgo,
                "주간 좋아요 순위"
            )
            
            // 주간 조회수 순위 정리
            cleanupExpiredElements(
                RedisBoardIdListRepository.BOARDS_BY_VIEWS_WEEK_KEY, 
                weekAgo,
                "주간 조회수 순위"
            )
            
            val duration = System.currentTimeMillis() - startTime
            log.info("[CacheCleanupScheduler] 주간 캐시 정리 완료: 소요 시간 {}ms", duration)
        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] 주간 캐시 정리 중 오류 발생", e)
        }
    }
    
    /**
     * 월간 캐시 정리 (매월 1일 새벽 3시에 실행)
     * 30일이 지난 게시글을 월간 좋아요/조회수 순위 목록에서 제거
     */
    @Scheduled(cron = "0 0 3 1 * *")
    fun cleanupMonthlyCache() {
        try {
            log.info("[CacheCleanupScheduler] 월간 캐시 정리 시작")
            val startTime = System.currentTimeMillis()
            
            // 게시글 스캔 및 필터링
            val monthAgo = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
            
            // 월간 좋아요 순위 정리
            cleanupExpiredElements(
                RedisBoardIdListRepository.BOARDS_BY_LIKES_MONTH_KEY, 
                monthAgo,
                "월간 좋아요 순위"
            )
            
            // 월간 조회수 순위 정리
            cleanupExpiredElements(
                RedisBoardIdListRepository.BOARDS_BY_VIEWS_MONTH_KEY, 
                monthAgo,
                "월간 조회수 순위"
            )
            
            val duration = System.currentTimeMillis() - startTime
            log.info("[CacheCleanupScheduler] 월간 캐시 정리 완료: 소요 시간 {}ms", duration)
        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] 월간 캐시 정리 중 오류 발생", e)
        }
    }
    
    /**
     * 모든 캐시 정리 (매일 새벽 4시에 실행)
     * 모든 기간의 캐시를 한 번에 정리
     */
    @Scheduled(cron = "0 0 4 * * *")
    fun cleanupAllCaches() {
        try {
            log.info("[CacheCleanupScheduler] 전체 캐시 정리 시작")
            val startTime = System.currentTimeMillis()
            
            // 각 기간별 기준 시간 계산
            val dayAgo = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()
            val weekAgo = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli()
            val monthAgo = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()
            
            // 일간 캐시 정리
            cleanupExpiredElements(RedisBoardIdListRepository.BOARDS_BY_LIKES_DAY_KEY, dayAgo, "일간 좋아요 순위")
            cleanupExpiredElements(RedisBoardIdListRepository.BOARDS_BY_VIEWS_DAY_KEY, dayAgo, "일간 조회수 순위")
            
            // 주간 캐시 정리
            cleanupExpiredElements(RedisBoardIdListRepository.BOARDS_BY_LIKES_WEEK_KEY, weekAgo, "주간 좋아요 순위")
            cleanupExpiredElements(RedisBoardIdListRepository.BOARDS_BY_VIEWS_WEEK_KEY, weekAgo, "주간 조회수 순위")
            
            // 월간 캐시 정리
            cleanupExpiredElements(RedisBoardIdListRepository.BOARDS_BY_LIKES_MONTH_KEY, monthAgo, "월간 좋아요 순위")
            cleanupExpiredElements(RedisBoardIdListRepository.BOARDS_BY_VIEWS_MONTH_KEY, monthAgo, "월간 조회수 순위")
            
            val duration = System.currentTimeMillis() - startTime
            log.info("[CacheCleanupScheduler] 전체 캐시 정리 완료: 소요 시간 {}ms", duration)
        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] 전체 캐시 정리 중 오류 발생", e)
        }
    }
    
    /**
     * 수동 캐시 정리 (API 또는 관리자 도구에서 호출 가능)
     * 모든 기간의 캐시를 강제로 정리
     */
    fun forceCleanupAllCaches() {
        try {
            log.info("[CacheCleanupScheduler] 강제 캐시 정리 시작")
            cleanupAllCaches()
            log.info("[CacheCleanupScheduler] 강제 캐시 정리 완료")
        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] 강제 캐시 정리 중 오류 발생", e)
            throw e
        }
    }
    
    /**
     * 기간이 지난 요소를 정리
     * @param key Redis 키
     * @param timestampThreshold 기준 시간(ms) - 이 시간 이전의 게시글들은 제거
     * @param label 로깅용 레이블
     */
    private fun cleanupExpiredElements(key: String, timestampThreshold: Long, label: String) {
        // 게시글 ID 형식: %019d (19자리 숫자, 0으로 패딩)
        val cursor = redisTemplate.opsForZSet().scan(
            key,
            org.springframework.data.redis.core.ScanOptions.scanOptions().count(100).build()
        )

        val expiredElements = mutableListOf<String>()

        try {
            while (cursor.hasNext()) {
                val tuple = cursor.next()
                val boardIdPadded = tuple.value // TypedTuple에서 값을 추출

                if (boardIdPadded != null) {
                    try {
                        // 게시글 ID 추출
                        val boardId = boardIdPadded.toLong()

                        // 게시글 상세 정보 키 형식 사용 (RedisBoardDetailRepository와 일치)
                        val boardKey = RedisBoardDetailRepository.BOARD_KEY_FORMAT.format(boardId)

                        // 게시글 JSON 데이터 조회
                        val boardJson = redisTemplate.opsForValue().get(boardKey)

                        if (boardJson == null) {
                            // 게시글 정보가 없으면 제거 대상으로 간주
                            expiredElements.add(boardIdPadded)
                        } else {
                            // JSON에서 createdAt 정보 추출 (ObjectMapper 없이 간단히 처리)
                            val createdAtPattern = "\"createdAt\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                            val matchResult = createdAtPattern.find(boardJson)

                            if (matchResult != null) {
                                val createdAt = matchResult.groupValues[1]
                                val createdTimestamp = Instant.parse(createdAt).toEpochMilli()

                                if (createdTimestamp < timestampThreshold) {
                                    expiredElements.add(boardIdPadded)
                                }
                            } else {
                                // createdAt을 찾을 수 없으면 보수적으로 유지
                                log.warn("[CacheCleanupScheduler] 게시글 JSON에서 createdAt을 찾을 수 없음: boardId={}", boardId)
                            }
                        }
                    } catch (e: Exception) {
                        log.warn("[CacheCleanupScheduler] 게시글 확인 중 오류: boardId={}, error={}", boardIdPadded, e.message)
                        // 오류 발생해도 계속 진행
                    }
                }
            }
        } finally {
            TimeUnit.MILLISECONDS.sleep(100) // 약간의 지연으로 부하 감소
            cursor.close()
        }

        // 한 번에 일괄 삭제
        if (expiredElements.isNotEmpty()) {
            redisTemplate.opsForZSet().remove(key, *expiredElements.toTypedArray())
            log.info("[CacheCleanupScheduler] {}에서 만료된 게시글 {} 개 제거", label, expiredElements.size)
        } else {
            log.info("[CacheCleanupScheduler] {}에서 만료된 게시글 없음", label)
        }
    }
} 