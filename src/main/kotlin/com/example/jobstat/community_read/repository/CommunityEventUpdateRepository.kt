package com.example.jobstat.community_read.repository

import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.core.event.payload.comment.*

/**
 * 이벤트 처리에 따른 Redis Read Model 업데이트를 원자적으로 수행하는 Repository
 */
interface CommunityEventUpdateRepository {

    /** 게시글 생성 이벤트 처리 */
    fun applyBoardCreation(payload: BoardCreatedEventPayload): Boolean

    /** 게시글 수정 이벤트 처리 (수정된 JSON 필요) */
    fun applyBoardUpdate(payload: BoardUpdatedEventPayload, updatedBoardJson: String): Boolean

    /** 게시글 삭제 이벤트 처리 */
    fun applyBoardDeletion(payload: BoardDeletedEventPayload): Boolean

    /** 게시글 좋아요 업데이트 처리 (수정된 JSON 필요) */
    fun applyBoardLikeUpdate(payload: BoardLikedEventPayload, updatedBoardJson: String): Boolean

    /** 게시글 조회수 업데이트 처리 (수정된 JSON 필요) */
    fun applyBoardViewUpdate(payload: BoardViewedEventPayload, updatedBoardJson: String): Boolean

    /** 게시글 랭킹 업데이트 처리 */
    fun applyBoardRankingUpdate(payload: BoardRankingUpdatedEventPayload): Boolean

    /** 댓글 생성 이벤트 처리 (생성된 댓글 JSON, 수정될 게시글 JSON 필요) */
    fun applyCommentCreation(payload: CommentCreatedEventPayload, commentJson: String, updatedBoardJson: String?): Boolean

    /** 댓글 수정 이벤트 처리 (수정된 JSON 필요) */
    fun applyCommentUpdate(payload: CommentUpdatedEventPayload, updatedCommentJson: String): Boolean

    /** 댓글 삭제 이벤트 처리 (수정될 게시글 JSON 필요) */
    fun applyCommentDeletion(payload: CommentDeletedEventPayload, updatedBoardJson: String?): Boolean
}