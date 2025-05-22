package com.wildrew.app.community.board.repository

import com.wildrew.app.community.board.entity.Board
import com.wildrew.app.community.board.utils.model.BoardRankingQueryResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * 게시글 관련 데이터 액세스를 위한 리포지토리 인터페이스
 */
interface BoardRepository {
    fun save(board: Board): Board

    fun findById(id: Long): Board

    fun findAll(pageable: Pageable): Page<Board>

    fun deleteById(id: Long)

    fun existsById(id: Long): Boolean

    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board>

    fun findByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun findTopNByOrderByViewCountDesc(limit: Int): List<Board>

    fun findAllWithDetails(pageable: Pageable): Page<Board>

    fun search(
        keyword: String,
        pageable: Pageable,
    ): Page<Board>

    fun countByAuthor(author: String): Long

    fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean

    fun findViewCountById(id: Long): Int?

    fun findLikeCountById(id: Long): Int?

    fun findAllByCategoryId(categoryId: Long): List<Board>

    fun findAllByIds(ids: List<Long>): List<Board>

    fun findBoardsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun findBoardsByCategoryAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun findBoardsByAuthorAfter(
        author: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun searchBoardsAfter(
        keyword: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    fun updateViewCount(
        boardId: Long,
        count: Int,
    )

    fun updateLikeCount(
        boardId: Long,
        count: Int,
    )

    fun findBoardIdsRankedByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long>

    fun findBoardIdsRankedByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long>

    fun findBoardIdsRankedByViewsAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastScore: Int?,
        lastId: Long?,
        limit: Int,
    ): List<Long>

    fun findBoardIdsRankedByLikesAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastScore: Int?,
        lastId: Long?,
        limit: Int,
    ): List<Long>

    fun findBoardRankingsByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult>

    fun findBoardRankingsByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult>
}
