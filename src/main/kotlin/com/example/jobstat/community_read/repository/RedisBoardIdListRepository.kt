package com.example.jobstat.community_read.repository

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisBoardIdListRepository(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val ALL_BOARDS_KEY = "community-read::board-list"
        const val CATEGORY_BOARDS_KEY_FORMAT = "community-read::category::%s::board-list"

        const val BOARDS_BY_LIKES_DAY_KEY = "community-read::likes::day::board-list"
        const val BOARDS_BY_LIKES_WEEK_KEY = "community-read::likes::week::board-list"
        const val BOARDS_BY_LIKES_MONTH_KEY = "community-read::likes::month::board-list"

        const val BOARDS_BY_VIEWS_DAY_KEY = "community-read::views::day::board-list"
        const val BOARDS_BY_VIEWS_WEEK_KEY = "community-read::views::week::board-list"
        const val BOARDS_BY_VIEWS_MONTH_KEY = "community-read::views::month::board-list"

        // 내부 상태 키 (마지막 업데이트 시각 등을 저장)
        private fun listStateKey(boardId: Long) = "board:list::$boardId"

        // AllBoard Limit Size
        const val ALL_BOARD_LIMIT_SIZE = 1000L
        const val CATEGORY_LIMIT_SIZE = 1000L

        // Ranking Limit Size
        const val RANKING_LIMIT_SIZE = 100L
    }

    // 공개 getter들 (Service에서 직접 키를 쓰지 않도록)
    fun getAllBoardsKey() = ALL_BOARDS_KEY
    fun getCategoryKey(categoryId: Long) = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)

    // ----------------------------
    // A) 읽기 (Query) 로직
    // ----------------------------
    fun readAllByTime(offset: Long, limit: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .reverseRange(ALL_BOARDS_KEY, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByTimeInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(ALL_BOARDS_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(ALL_BOARDS_KEY, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(ALL_BOARDS_KEY, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByCategory(categoryId: Long, offset: Long, limit: Long): List<Long> {
        val key = getCategoryKey(categoryId)
        return redisTemplate.opsForZSet()
            .reverseRange(key, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByCategoryInfiniteScroll(categoryId: Long, lastBoardId: Long?, limit: Long): List<Long> {
        val key = getCategoryKey(categoryId)
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(key, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(key, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByLikesDay(offset: Long, limit: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .reverseRange(BOARDS_BY_LIKES_DAY_KEY, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByLikesDayInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_DAY_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(BOARDS_BY_LIKES_DAY_KEY, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_LIKES_DAY_KEY, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByLikesWeek(offset: Long, limit: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .reverseRange(BOARDS_BY_LIKES_WEEK_KEY, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByLikesWeekInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_WEEK_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(BOARDS_BY_LIKES_WEEK_KEY, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_LIKES_WEEK_KEY, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByLikesMonth(offset: Long, limit: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .reverseRange(BOARDS_BY_LIKES_MONTH_KEY, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByLikesMonthInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_MONTH_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(BOARDS_BY_LIKES_MONTH_KEY, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_LIKES_MONTH_KEY, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByViewsDay(offset: Long, limit: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .reverseRange(BOARDS_BY_VIEWS_DAY_KEY, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByViewsDayInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_DAY_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(BOARDS_BY_VIEWS_DAY_KEY, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_VIEWS_DAY_KEY, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByViewsWeek(offset: Long, limit: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .reverseRange(BOARDS_BY_VIEWS_WEEK_KEY, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByViewsWeekInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(BOARDS_BY_VIEWS_WEEK_KEY, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_VIEWS_WEEK_KEY, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByViewsMonth(offset: Long, limit: Long): List<Long> {
        return redisTemplate.opsForZSet()
            .reverseRange(BOARDS_BY_VIEWS_MONTH_KEY, offset, offset + limit - 1)
            ?.map { it.toLong() } ?: emptyList()
    }

    fun readAllByViewsMonthInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }
        val score = redisTemplate.opsForZSet().score(BOARDS_BY_VIEWS_MONTH_KEY, lastBoardId.toString()) ?: 0.0
        return redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_VIEWS_MONTH_KEY, 0.0, score - 0.000001, 0, limit)
            ?.map { it.toLong() } ?: emptyList()
    }

    // -----------------------------
    // B) 쓰기 (파이프라인, eventTs 기반)
    // -----------------------------
    fun addBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        eventTs: Long,
        score: Double,
    ) {
        val stateKey = listStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "eventTs").toLongOrNull() ?: 0
        if (eventTs <= currentTs) return

        conn.zAdd(ALL_BOARDS_KEY, score, boardId.toString())
        conn.zRemRange(ALL_BOARDS_KEY, 0, -(ALL_BOARD_LIMIT_SIZE + 1))
        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }

    fun addBoardToCategoryInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long,
        eventTs: Long,
        score: Double,
    ) {
        val stateKey = listStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "eventTs")?.toLongOrNull() ?: 0
        if (eventTs <= currentTs) return

        val catKey = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)
        conn.zAdd(catKey, score, boardId.toString())
        conn.zRemRange(catKey, 0, -(CATEGORY_LIMIT_SIZE + 1))
        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }

    fun removeBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long?,
        eventTs: Long
    ) {
        val stateKey = listStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "eventTs")?.toLongOrNull() ?: 0
        if (eventTs <= currentTs) return

        conn.zRem(ALL_BOARDS_KEY, boardId.toString())
        if (categoryId != null) {
            val catKey = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)
            conn.zRem(catKey, boardId.toString())
        }
        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }

    fun addLikesRankingInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        eventTs: Long,
        likeScore: Double
    ) {
        val stateKey = listStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "eventTs")?.toLongOrNull() ?: 0
        if (eventTs <= currentTs) return

        conn.zAdd(BOARDS_BY_LIKES_DAY_KEY, likeScore, boardId.toString())
        conn.zRemRange(BOARDS_BY_LIKES_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zAdd(BOARDS_BY_LIKES_WEEK_KEY, likeScore, boardId.toString())
        conn.zRemRange(BOARDS_BY_LIKES_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zAdd(BOARDS_BY_LIKES_MONTH_KEY, likeScore, boardId.toString())
        conn.zRemRange(BOARDS_BY_LIKES_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }

    fun incrLikesRankingInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        eventTs: Long,
        likeDelta: Double
    ) {
        val stateKey = listStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "eventTs")?.toLongOrNull() ?: 0
        if (eventTs <= currentTs) return

        conn.zIncrBy(BOARDS_BY_LIKES_DAY_KEY, likeDelta, boardId.toString())
        conn.zRemRange(BOARDS_BY_LIKES_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zIncrBy(BOARDS_BY_LIKES_WEEK_KEY, likeDelta, boardId.toString())
        conn.zRemRange(BOARDS_BY_LIKES_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zIncrBy(BOARDS_BY_LIKES_MONTH_KEY, likeDelta, boardId.toString())
        conn.zRemRange(BOARDS_BY_LIKES_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }

    fun addViewsRankingInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        eventTs: Long,
        viewScore: Double
    ) {
        val stateKey = listStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "eventTs")?.toLongOrNull() ?: 0
        if (eventTs <= currentTs) return

        conn.zAdd(BOARDS_BY_VIEWS_DAY_KEY, viewScore, boardId.toString())
        conn.zRemRange(BOARDS_BY_VIEWS_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zAdd(BOARDS_BY_VIEWS_WEEK_KEY, viewScore, boardId.toString())
        conn.zRemRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zAdd(BOARDS_BY_VIEWS_MONTH_KEY, viewScore, boardId.toString())
        conn.zRemRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }

    fun incrViewsRankingInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        eventTs: Long,
        viewDelta: Double
    ) {
        val stateKey = listStateKey(boardId)
        val currentTs = conn.hGet(stateKey, "eventTs")?.toLongOrNull() ?: 0
        if (eventTs <= currentTs) return

        conn.zIncrBy(BOARDS_BY_VIEWS_DAY_KEY, viewDelta, boardId.toString())
        conn.zRemRange(BOARDS_BY_VIEWS_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zIncrBy(BOARDS_BY_VIEWS_WEEK_KEY, viewDelta, boardId.toString())
        conn.zRemRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.zIncrBy(BOARDS_BY_VIEWS_MONTH_KEY, viewDelta, boardId.toString())
        conn.zRemRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1))

        conn.hSet(stateKey, "eventTs", eventTs.toString())
    }
}