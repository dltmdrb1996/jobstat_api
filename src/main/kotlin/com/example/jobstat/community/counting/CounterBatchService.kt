package com.example.jobstat.community.counting

import com.example.jobstat.community.board.repository.BoardRepository // BoardRepository 의존성 유지
import com.example.jobstat.community.event.CommunityCommandEventPublisher // EventPublisher 의존성 유지
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime // 예시 EventPublisher 사용 위해 추가

/**
 * REQUIRES_NEW 트랜잭션을 사용하여 개별 게시글 카운터의 DB 반영을 처리.
 * - CounterService에서 호출되며, 각 게시글 업데이트는 격리된 트랜잭션에서 실행됨.
 * - Redis 접근 로직은 제거되고, 카운트 값은 파라미터로 전달받음.
 */
@Service
internal class CounterBatchService(
    private val boardRepository: BoardRepository,
    private val communityCommandEventPublisher: CommunityCommandEventPublisher,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 하나의 게시글에 대한 조회수/좋아요 카운터를 DB에 반영 (값을 파라미터로 받음).
     * Redis 키는 이 메소드 호출 전에 이미 삭제됨 (CounterService가 CounterRepository를 통해 삭제).
     * - REQUIRES_NEW 트랜잭션 사용.
     *
     * @param boardId 처리할 게시글 ID
     * @param viewCount Redis에서 가져온 조회수 증가분 (getAndDelete 결과)
     * @param likeCount Redis에서 가져온 좋아요 증가분 (getAndDelete 결과)
     * @return DB 업데이트 성공 여부 (true: 성공 또는 변경 없음, false: 실패)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processSingleBoardCounter(
        boardId: Long,
        viewCount: Int,
        likeCount: Int,
    ): Boolean =
        try {
            // 변경사항 없으면 DB 접근 없이 성공 처리 (Redis 키는 이미 삭제됨)
            if (viewCount == 0 && likeCount == 0) {
                log.debug("게시글 ID {} 카운터 변경 없음 (DB 업데이트 불필요)", boardId)
                true
            } else {
                val board = boardRepository.findById(boardId) // 예시: JPA Repository 사용
                board.incrementViewCount(viewCount)
                board.incrementLikeCount(likeCount)

                // 조회수 변경 시 이벤트 발행
                if (viewCount > 0) {
                    communityCommandEventPublisher.publishBoardViewed(
                        boardId = boardId,
                        createdAt = board.createdAt ?: LocalDateTime.now(),
                        viewCount = board.viewCount,
                    )
                }

                log.info(
                    "게시글 ID {} DB 카운터 반영: 조회수 {}{}, 좋아요 {}{}",
                    boardId,
                    if (viewCount >= 0) "+" else "",
                    viewCount,
                    if (likeCount >= 0) "+" else "",
                    likeCount,
                )
                true // DB 업데이트 성공
            }
        } catch (e: Exception) {
            log.error("게시글 ID {} DB 카운터 처리 중 예외 발생", boardId, e)
            false // 실패 반환 -> CounterService에서 재시도 관리
        }
}
