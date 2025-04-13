package com.example.jobstat.community.board.repository

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

internal interface BoardRepository{
    fun save(board: Board): Board

    fun findById(id: Long): Board

    fun findByAuthor(author: String, pageable: Pageable): Page<Board>

    fun findByCategory(categoryId: Long, pageable: Pageable): Page<Board>

    fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable
    ): Page<Board>

    fun findTopNByOrderByViewCountDesc(limit: Int): List<Board>

    fun findAllWithDetails(pageable: Pageable): Page<Board>

    fun search(keyword: String, pageable: Pageable): Page<Board>

    fun countByAuthor(author: String): Long

    fun existsByAuthorAndTitle(author: String, title: String): Boolean

    fun findViewCountById(id: Long): Int?

    fun findLikeCountById(id: Long): Int?

    fun updateViewCount(boardId: Long, count: Int)

    fun updateLikeCount(boardId: Long, count: Int)

    fun findAllByCategoryId(categoryId: Long): List<Board>

    fun findAll(pageable: Pageable): Page<Board>

    fun deleteById(id: Long)

    fun findAllByIds(ids: List<Long>): List<Board>

    fun findBoardsAfter(lastBoardId: Long?, limit: Int): List<Board>
    fun findBoardsByCategoryAfter(categoryId: Long, lastBoardId: Long?, limit: Int): List<Board>
    fun findBoardsByAuthorAfter(author: String, lastBoardId: Long?, limit: Int): List<Board>
    fun searchBoardsAfter(keyword: String, lastBoardId: Long?, limit: Int): List<Board>

    fun findBoardIdsRankedByLikes(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<Long>
    fun findBoardIdsRankedByViews(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<Long>
    fun findBoardIdsRankedByLikesAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastBoardId: Long?,
        limit: Int
    ): List<Long>
    fun findBoardIdsRankedByViewsAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastBoardId: Long?,
        limit: Int
    ): List<Long>

    fun findBoardRankingsByViews(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<BoardRankingQueryResult>
    fun findBoardRankingsByLikes(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<BoardRankingQueryResult>
}

interface BoardRankingQueryResult {
    fun getBoardId(): Long
    fun getScore(): Long
}
