package com.wildrew.jobstat.community.board.service

import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import com.wildrew.jobstat.core.core_web_util.CoreConstants
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 게시글 랭킹 정보를 주기적으로 갱신하는 스케줄러
 * - 매 시간마다 실행되어 최신 랭킹 데이터를 읽기 모델에 반영
 */
@Component
class RankingSyncScheduler(
    private val boardService: BoardService,
    private val eventPublisher: CommunityCommandEventPublisher,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 0 * * * *") // 매시간 정각
    @SchedulerLock(
        name = "RankingSyncScheduler_syncBoardRankings",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT59M",
    )
    fun syncBoardRankings() {
        log.debug("게시글 랭킹 주기적 동기화 시작...")

        val metrics = BoardRankingMetric.entries
        val periods = BoardRankingPeriod.entries
        val limit = CoreConstants.RANKING_LIMIT_SIZE

        metrics.forEach { metric ->
            periods.forEach { period ->
                try {
                    log.debug("랭킹 데이터 조회 중 - 지표: {}, 기간: {}, 제한: {}", metric, period, limit)
                    val results = boardService.getBoardRankingsForPeriod(metric, period, limit)

                    val rankingEntries =
                        results.map {
                            com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload.RankingEntry(
                                boardId = it.boardId,
                                score = it.score.toDouble(), // Long 카운트를 Double 점수로 변환
                            )
                        }

                    if (rankingEntries.isNotEmpty()) {
                        log.debug("랭킹 업데이트 이벤트 발행 - 지표: {}, 기간: {}, 개수: {}", metric, period, rankingEntries.size)
                        eventPublisher.publishBoardRankingUpdated(metric, period, rankingEntries)
                    } else {
                        log.debug("랭킹 데이터 없음 - 지표: {}, 기간: {}, 이벤트 발행 건너뜀", metric, period)
                    }
                } catch (e: Exception) {
                    log.error("랭킹 동기화 중 오류 발생 - 지표: {}, 기간: {}", metric, period, e)
                }
            }
        }
        log.debug("게시글 랭킹 주기적 동기화 완료")
    }
}
