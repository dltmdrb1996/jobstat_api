package com.wildrew.jobstat.community_read.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection

interface BoardIdListRepository {
    fun getAllBoardsKey(): String

    fun readAllByTimeByOffset(pageable: Pageable): Page<Long>

    fun readAllByTimeByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun readAllByCategoryByOffset(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Long>

    fun readAllByCategoryByCursor(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun readAllByLikesDayByOffset(pageable: Pageable): Page<Long>

    fun readAllByLikesDayByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun readAllByLikesWeekByOffset(pageable: Pageable): Page<Long>

    fun readAllByLikesWeekByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun readAllByLikesMonthByOffset(pageable: Pageable): Page<Long>

    fun readAllByLikesMonthByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun readAllByViewsDayByOffset(pageable: Pageable): Page<Long>

    fun readAllByViewsDayByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun readAllByViewsWeekByOffset(pageable: Pageable): Page<Long>

    fun readAllByViewsWeekByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun readAllByViewsMonthByOffset(pageable: Pageable): Page<Long>

    fun readAllByViewsMonthByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long>

    fun addBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        score: Double,
    )

    fun addBoardToCategoryInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long,
        score: Double,
    )

    fun removeBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long?,
    )

    fun replaceRankingListInPipeline(
        conn: StringRedisConnection,
        key: String,
        rankings: List<com.wildrew.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload.RankingEntry>,
    )
}
