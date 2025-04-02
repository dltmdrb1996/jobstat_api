package com.example.jobstat.community.counting

import com.example.jobstat.community.CommunityEventPublisher
import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.core.global.extension.toEpochMilli
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * REQUIRES_NEW 트랜잭션을 사용하기 위해 별도 Bean으로 분리
 * - CounterService -> CounterBatchService 호출 시, 프록시를 통해 새 트랜잭션이 생성됨
 */
@Service
internal class CounterBatchService(
    private val counterRepository: CounterRepository,
    private val boardRepository: BoardRepository,
    private val communityEventPublisher: CommunityEventPublisher,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 하나의 게시글에 대한 조회수/좋아요 카운터를 DB에 반영
     * - REQUIRES_NEW
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processSingleBoardCounter(boardId: Long): Boolean {
        return try {
            val viewCount = counterRepository.getAndResetViewCount(boardId)
            val likeCount = counterRepository.getAndResetLikeCount(boardId)

            // 변경사항 없으면 skip
            if (viewCount == 0 && likeCount == 0) {
                log.debug("게시글 ID {} 카운터 변경 없음", boardId)
                true
            } else {
                val board = boardRepository.findById(boardId)

                board.incrementViewCount(viewCount)
                board.incrementLikeCount(likeCount)
                communityEventPublisher.publishBoardViewed(
                    boardId = boardId,
                    createdAt = board.createdAt,
                    eventTs = board.updatedAt.toEpochMilli(),
                    viewCount = board.viewCount
                )
                log.info(
                    "게시글 ID {} 카운터 반영: 조회수 {}{}, 좋아요 {}{}",
                    boardId,
                    if (viewCount >= 0) "+" else "", viewCount,
                    if (likeCount >= 0) "+" else "", likeCount
                )
                true
            }
        } catch (e: Exception) {
            log.error("게시글 ID {} 카운터 처리 중 예외 발생", boardId, e)
            false
        }
    }
}