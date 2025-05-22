package com.wildrew.app.community_read.service

import com.wildrew.app.community_read.model.BoardReadModel
import com.wildrew.app.community_read.model.CommentReadModel
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommunityReadService {
    fun getBoardByIdWithFetch(boardId: Long): BoardReadModel

    fun getBoardByIdWithComments(boardId: Long): BoardReadModel

    fun getBoardByIdsWithFetch(boardIds: List<Long>): List<BoardReadModel>

    fun getLatestBoardsByOffset(pageable: Pageable): Page<BoardReadModel>

    fun getLatestBoardsByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<BoardReadModel>

    fun getCategoryBoardsByOffset(
        categoryId: Long,
        pageable: Pageable,
    ): Page<BoardReadModel>

    fun getCategoryBoardsByCursor(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Long,
    ): List<BoardReadModel>

    fun getRankedBoardsByOffset(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        pageable: Pageable,
    ): Page<BoardReadModel>

    fun getRankedBoardsByCursor(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        lastBoardId: Long?,
        limit: Long,
    ): List<BoardReadModel>

    fun getCommentsByBoardIdByOffset(
        boardId: Long,
        pageable: Pageable,
    ): Page<CommentReadModel>

    fun getCommentsByBoardIdByCursor(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<CommentReadModel>

    fun getCommentById(commentId: Long): CommentReadModel

    fun getCommentsByIds(commentIds: List<Long>): List<CommentReadModel>
}
