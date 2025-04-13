package com.example.jobstat.community.board.service

import com.example.jobstat.community.event.CommunityCommandEventPublisher
import com.example.jobstat.core.constants.CoreConstants
import com.example.jobstat.core.event.payload.board.BoardRankingUpdatedEventPayload
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class RankingSyncScheduler(
    private val boardService: BoardService,
    private val eventPublisher: CommunityCommandEventPublisher
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true) // 데이터 조회를 위한 읽기 전용 트랜잭션
    fun syncBoardRankings() {
        log.info("게시글 랭킹 주기적 동기화 시작...")
        // .entries 로 변경 (더 Kotlin 스타일)
        val metrics = BoardRankingMetric.entries
        val periods = BoardRankingPeriod.entries
        val limit = CoreConstants.RANKING_LIMIT_SIZE

        metrics.forEach { metric ->
            periods.forEach { period ->
                try {
                    log.debug("랭킹 데이터 조회 중 - 지표: {}, 기간: {}, 제한: {}", metric, period, limit)
                    val results = boardService.getBoardRankingsForPeriod(metric, period, limit)

                    val rankingEntries = results.map {
                        BoardRankingUpdatedEventPayload.RankingEntry(
                            boardId = it.getBoardId(),
                            score = it.getScore().toDouble() // Long 카운트를 Double 점수로 변환
                        )
                    }

                    if (rankingEntries.isNotEmpty()) {
                        log.info("랭킹 업데이트 이벤트 발행 - 지표: {}, 기간: {}, 개수: {}", metric, period, rankingEntries.size)
                        eventPublisher.publishBoardRankingUpdated(metric, period, rankingEntries)
                    } else {
                        log.debug("랭킹 데이터 없음 - 지표: {}, 기간: {}, 이벤트 발행 건너뜀", metric, period)
                        // 선택사항: 읽기 모델에서 랭킹을 명시적으로 지워야 하는 경우 빈 리스트 이벤트 발행
                        // eventPublisher.publishBoardRankingUpdated(metric, period, emptyList())
                    }

                } catch (e: Exception) {
                    log.error("랭킹 동기화 중 오류 발생 - 지표: {}, 기간: {}", metric, period, e)
                    // 오류 처리 결정: 다음으로 계속, 재시도, 알림 등
                }
            }
        }
        log.info("게시글 랭킹 주기적 동기화 완료")
    }
}