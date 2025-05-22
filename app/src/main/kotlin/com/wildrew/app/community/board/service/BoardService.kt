package com.wildrew.app.community.board.service

import com.wildrew.app.community.board.entity.Board
import com.wildrew.app.community.board.utils.model.BoardRankingQueryResult
import com.wildrew.jobstat.core.core_global.model.BoardRankingMetric
import com.wildrew.jobstat.core.core_global.model.BoardRankingPeriod
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BoardService {
    fun createBoard(
        title: String,
        content: String,
        author: String,
        categoryId: Long,
        password: String? = null,
        userId: Long? = null,
    ): Board

    fun updateBoard(
        id: Long,
        title: String,
        content: String,
    ): Board

    fun deleteBoard(id: Long)

    fun getBoard(id: Long): Board

    fun getBoardsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board>

    fun getBoardsByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun getBoardsByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun getAllBoards(pageable: Pageable): Page<Board>

    fun getAllBoardsWithComments(pageable: Pageable): Page<Board>

    fun getTopNBoardsByViews(limit: Int): List<Board>

    fun searchBoards(
        keyword: String,
        pageable: Pageable,
    ): Page<Board>

    fun countBoardsByAuthor(author: String): Long

    fun getBoardsByIds(ids: List<Long>): List<Board>

    fun getBoardsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun getBoardsByCategoryAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun getBoardsByAuthorAfter(
        author: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun searchBoardsAfter(
        keyword: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun getBoardIdsRankedByMetric(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        pageable: Pageable,
    ): Page<Long>

    fun getBoardIdsRankedByMetricAfter(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>

    fun getBoardRankingsForPeriod(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        limit: Long,
    ): List<BoardRankingQueryResult>
}
