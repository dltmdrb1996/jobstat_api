package com.example.jobstat.community_read.scheduler

import com.example.jobstat.community_read.repository.RedisBoardDetailRepository
import com.example.jobstat.community_read.repository.RedisBoardIdListRepository
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.global.extension.toEpochMilli
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
    private val redisTemplate: StringRedisTemplate,
    private val communityReadService: CommunityReadService
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
        val cursor = redisTemplate.opsForZSet().scan(
            key,
            org.springframework.data.redis.core.ScanOptions.scanOptions().count(100).build()
        )

        val expiredElements = mutableListOf<String>()
        val boardIdsToCheck = mutableListOf<Long>()

        try {
            while (cursor.hasNext()) {
                val tuple = cursor.next()
                val boardIdStr = tuple.value

                if (boardIdStr != null) {
                    try {
                        // 게시글 ID 추출
                        val boardId = boardIdStr.toLong()
                        // 검증 대상 목록에 추가
                        boardIdsToCheck.add(boardId)
                    } catch (e: Exception) {
                        log.warn("[CacheCleanupScheduler] 게시글 ID 변환 중 오류: boardId={}, error={}", boardIdStr, e.message)
                    }
                }
            }
        } finally {
            cursor.close()
        }

        if (boardIdsToCheck.isEmpty()) {
            log.info("[CacheCleanupScheduler] {}에서 검증할 게시글 없음", label)
            return
        }

        log.info("[CacheCleanupScheduler] {}에서 {} 개 게시글 검증 시작", label, boardIdsToCheck.size)

        // 서비스를 통해 게시글 세부 정보 조회 (DB 조회 포함)
        try {
            val boardDetails = communityReadService.getBoardByIdsWithFetch(boardIdsToCheck)

            // 검증: 기간이 지난 게시글 찾기
            for (boardId in boardIdsToCheck) {
                val board = boardDetails.find { it.id == boardId }

                if (board == null) {
                    // DB에도 없는 게시글은 제거 대상
                    expiredElements.add(boardId.toString())
                    log.debug("[CacheCleanupScheduler] DB에 존재하지 않는 게시글 제거 대상으로 설정: boardId={}", boardId)
                } else {
                    // 날짜 확인
                    val createdTimestamp = board.createdAt.toEpochMilli()

                    if (createdTimestamp < timestampThreshold) {
                        // 기간이 지난 게시글은 제거 대상
                        expiredElements.add(boardId.toString())
                        log.debug("[CacheCleanupScheduler] 기간 경과로 제거 대상으로 설정: boardId={}, createdAt={}",
                            boardId, board.createdAt)
                    }
                }
            }

            // 한 번에 일괄 삭제
            if (expiredElements.isNotEmpty()) {
                redisTemplate.opsForZSet().remove(key, *expiredElements.toTypedArray())
                log.info("[CacheCleanupScheduler] {}에서 만료된 게시글 {} 개 제거", label, expiredElements.size)
            } else {
                log.info("[CacheCleanupScheduler] {}에서 만료된 게시글 없음", label)
            }

        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] 게시글 검증 중 오류 발생", e)
        }
    }

    /**
     * 미아 데이터 검증 및 정리 (매일 새벽 5시에 실행)
     * 랭킹은 있지만 상세 정보가 없는 데이터를 검증하고 필요한 경우 DB에서 가져온 후 정리
     */
    @Scheduled(cron = "0 0 5 * * *")
    fun verifyAndReconcileData() {
        try {
            log.info("[CacheCleanupScheduler] 데이터 일관성 검증 및 복구 시작")
            val startTime = System.currentTimeMillis()

            // 모든 랭킹 키 목록
            val rankingKeys = listOf(
                RedisBoardIdListRepository.BOARDS_BY_LIKES_DAY_KEY,
                RedisBoardIdListRepository.BOARDS_BY_LIKES_WEEK_KEY,
                RedisBoardIdListRepository.BOARDS_BY_LIKES_MONTH_KEY,
                RedisBoardIdListRepository.BOARDS_BY_VIEWS_DAY_KEY,
                RedisBoardIdListRepository.BOARDS_BY_VIEWS_WEEK_KEY,
                RedisBoardIdListRepository.BOARDS_BY_VIEWS_MONTH_KEY
            )

            // 각 랭킹 키에 대해 재검증 및 복구 수행
            for (key in rankingKeys) {
                verifyAndReconcileRankingData(key)
            }

            val duration = System.currentTimeMillis() - startTime
            log.info("[CacheCleanupScheduler] 데이터 일관성 검증 및 복구 완료: 소요 시간 {}ms", duration)
        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] 데이터 일관성 검증 및 복구 중 오류 발생", e)
        }
    }

    /**
     * 랭킹 데이터 검증 및 복구
     * - Redis에 있지만 상세 정보가 없는 게시글: DB에서 조회하여 복구 시도
     * - DB에도 없는 게시글: 랭킹에서 제거
     */
    private fun verifyAndReconcileRankingData(key: String) {
        try {
            log.info("[CacheCleanupScheduler] {} 랭킹 데이터 검증 및 복구 시작", key)

            val cursor = redisTemplate.opsForZSet().scan(
                key,
                org.springframework.data.redis.core.ScanOptions.scanOptions().count(100).build()
            )

            val boardIds = mutableListOf<Long>()

            try {
                while (cursor.hasNext()) {
                    val tuple = cursor.next()
                    val boardIdStr = tuple.value

                    if (boardIdStr != null) {
                        try {
                            boardIds.add(boardIdStr.toLong())
                        } catch (e: Exception) {
                            log.warn("[CacheCleanupScheduler] ID 변환 오류: {}", boardIdStr)
                        }
                    }
                }
            } finally {
                cursor.close()
            }

            if (boardIds.isEmpty()) {
                log.info("[CacheCleanupScheduler] {}에 검증할 데이터 없음", key)
                return
            }

            // 서비스를 통해 게시글 정보 조회 (캐시 미스 시 DB에서 자동 조회)
            log.info("[CacheCleanupScheduler] {}의 {} 개 게시글 검증", key, boardIds.size)
            val boardDetails = communityReadService.getBoardByIdsWithFetch(boardIds)

            // DB에도 없는 게시글 식별
            val validIds = boardDetails.map { it.id }.toSet()
            val invalidIds = boardIds.filter { !validIds.contains(it) }

            if (invalidIds.isNotEmpty()) {
                // DB에 없는 게시글은 랭킹에서 제거
                redisTemplate.opsForZSet().remove(key, *invalidIds.map { it.toString() }.toTypedArray())
                log.info("[CacheCleanupScheduler] {}에서 DB에 존재하지 않는 게시글 {} 개 제거", key, invalidIds.size)
            } else {
                log.info("[CacheCleanupScheduler] {}의 모든 게시글이 유효함", key)
            }

        } catch (e: Exception) {
            log.error("[CacheCleanupScheduler] {} 랭킹 데이터 검증 및 복구 중 오류 발생", key, e)
        }
    }
}