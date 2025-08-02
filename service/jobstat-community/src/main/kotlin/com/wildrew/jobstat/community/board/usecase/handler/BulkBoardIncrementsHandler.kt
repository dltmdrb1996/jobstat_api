package com.wildrew.jobstat.community.board.usecase.handler

import com.wildrew.jobstat.community.board.repository.batch.BoardBatchRepositoryImpl
import com.wildrew.jobstat.community.event.CommunityCommandEventPublisher
import com.wildrew.jobstat.core.core_event.consumer.EventHandlingUseCase
import com.wildrew.jobstat.core.core_event.model.Event
import com.wildrew.jobstat.core.core_event.model.EventPayload
import com.wildrew.jobstat.core.core_event.model.EventType
import com.wildrew.jobstat.core.core_event.model.payload.board.BulkBoardIncrementsForCommandPayload
import com.wildrew.jobstat.core.core_jdbc_batch.core.model.BatchOptions
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnType
import com.wildrew.jobstat.core.core_jdbc_batch.infrastructure.update.ColumnUpdate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BulkBoardIncrementsHandler(
    private val boardBatchRepositoryImpl: BoardBatchRepositoryImpl,
    private val publisher: CommunityCommandEventPublisher,
) : EventHandlingUseCase<EventType, BulkBoardIncrementsForCommandPayload, Unit>() {
    override val eventType: EventType = EventType.BULK_BOARD_INCREMENTS_COMMAND

    @Transactional
    override fun invoke(event: Event<out EventPayload>) {
        super.invoke(event)
    }

    override fun execute(payload: BulkBoardIncrementsForCommandPayload) {
        if (payload.items.isEmpty()) {
            log.info("배치 ID: ${payload.batchId}에 대한 처리할 항목이 없습니다")
            return
        }

        val likeColumnName = "like_count"
        val viewColumnName = "view_count"

        val likeUpdates =
            payload.items
                .filter { it.likeIncrement != 0 }
                .map { item ->
                    item.boardId to
                        ColumnUpdate(
                            columnName = likeColumnName,
                            value = item.likeIncrement,
                            type = ColumnType.INT,
                        )
                }

        val viewUpdates =
            payload.items
                .filter { it.viewIncrement != 0 }
                .map { item ->
                    item.boardId to
                        ColumnUpdate(
                            columnName = viewColumnName,
                            value = item.viewIncrement,
                            type = ColumnType.INT,
                        )
                }

        val batchOptions = BatchOptions()

        var totalLikesUpdated = 0
        var totalViewsUpdated = 0

        if (likeUpdates.isNotEmpty()) {
            log.info("배치 ID: ${payload.batchId}의 ${likeUpdates.size}개 항목에 대해 '$likeColumnName' 일괄 업데이트 중")
            totalLikesUpdated = boardBatchRepositoryImpl.batchColumnUpdate(likeUpdates, batchOptions)
            log.info("'$likeColumnName' 업데이트 완료. ${totalLikesUpdated}개 레코드 영향받음. 배치 ID: ${payload.batchId}")
        } else {
            log.info("배치 ID: ${payload.batchId}에 대한 '$likeColumnName' 업데이트 필요 없음")
        }

        if (viewUpdates.isNotEmpty()) {
            log.info("배치 ID: ${payload.batchId}의 ${viewUpdates.size}개 항목에 대해 '$viewColumnName' 일괄 업데이트 중")
            totalViewsUpdated = boardBatchRepositoryImpl.batchColumnUpdate(viewUpdates, batchOptions)
            log.info("'$viewColumnName' 업데이트 완료. ${totalViewsUpdated}개 레코드 영향받음. 배치 ID: ${payload.batchId}")
        } else {
            log.info("배치 ID: ${payload.batchId}에 대한 '$viewColumnName' 업데이트 필요 없음")
        }

        log.info(
            "배치 ID: ${payload.batchId}에 대한 게시판 증가 일괄 처리 완료. " +
                "좋아요 업데이트: ${totalLikesUpdated}개, 조회수 업데이트: ${totalViewsUpdated}개",
        )

        publisher.publishBulkBoardIncrementsRead(
            batchId = payload.batchId,
            items = payload.items,
            eventTs = payload.eventTs,
        )
    }
}
