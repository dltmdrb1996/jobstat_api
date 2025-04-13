package com.example.jobstat.community_read.repository

import com.example.jobstat.core.event.payload.board.BoardRankingUpdatedEventPayload
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection

interface BoardIdListRepository {
    // 공개 상수
    companion object {
        const val ALL_BOARD_LIMIT_SIZE = 10000L
        const val CATEGORY_LIMIT_SIZE = 1000L
        const val RANKING_LIMIT_SIZE = 100L
    }

    // 키 반환 메서드
    fun getAllBoardsKey(): String
    fun getCategoryKey(categoryId: Long): String

    // 타임라인 조회
    fun readAllByTimeByOffset(pageable: Pageable): Page<Long>
    fun readAllByTimeByCursor(lastBoardId: Long?, limit: Long): List<Long>

    // 카테고리 조회
    fun readAllByCategoryByOffset(categoryId: Long, pageable: Pageable): Page<Long>
    fun readAllByCategoryByCursor(categoryId: Long, lastBoardId: Long?, limit: Long): List<Long>

    // 좋아요 랭킹 조회 (일/주/월)
    fun readAllByLikesDayByOffset(pageable: Pageable): Page<Long>
    fun readAllByLikesDayByCursor(lastBoardId: Long?, limit: Long): List<Long>
    fun readAllByLikesWeekByOffset(pageable: Pageable): Page<Long>
    fun readAllByLikesWeekByCursor(lastBoardId: Long?, limit: Long): List<Long>
    fun readAllByLikesMonthByOffset(pageable: Pageable): Page<Long>
    fun readAllByLikesMonthByCursor(lastBoardId: Long?, limit: Long): List<Long>

    // 조회수 랭킹 조회 (일/주/월)
    fun readAllByViewsDayByOffset(pageable: Pageable): Page<Long>
    fun readAllByViewsDayByCursor(lastBoardId: Long?, limit: Long): List<Long>
    fun readAllByViewsWeekByOffset(pageable: Pageable): Page<Long>
    fun readAllByViewsWeekByCursor(lastBoardId: Long?, limit: Long): List<Long>
    fun readAllByViewsMonthByOffset(pageable: Pageable): Page<Long>
    fun readAllByViewsMonthByCursor(lastBoardId: Long?, limit: Long): List<Long>

    // 파이프라인 작업
    fun addBoardInPipeline(conn: StringRedisConnection, boardId: Long, score: Double)
    fun addBoardToCategoryInPipeline(conn: StringRedisConnection, boardId: Long, categoryId: Long, score: Double)
    fun removeBoardInPipeline(conn: StringRedisConnection, boardId: Long, categoryId: Long?)
    fun replaceRankingListInPipeline(conn: StringRedisConnection, key: String, rankings: List<BoardRankingUpdatedEventPayload.RankingEntry>)
//    fun addLikesRankingInPipeline(conn: StringRedisConnection, boardId: Long, likeScore: Double)
//    fun incrLikesRankingInPipeline(conn: StringRedisConnection, boardId: Long, likeDelta: Double)
//    fun addViewsRankingInPipeline(conn: StringRedisConnection, boardId: Long, viewScore: Double)
//    fun incrViewsRankingInPipeline(conn: StringRedisConnection, boardId: Long, viewDelta: Double)
}


