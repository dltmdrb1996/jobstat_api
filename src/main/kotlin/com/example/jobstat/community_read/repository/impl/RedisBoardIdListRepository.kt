package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.repository.BoardIdListRepository
import com.example.jobstat.community_read.repository.BoardIdListRepository.Companion.ALL_BOARD_LIMIT_SIZE
import com.example.jobstat.community_read.repository.BoardIdListRepository.Companion.CATEGORY_LIMIT_SIZE
import com.example.jobstat.core.constants.CoreConstants
import com.example.jobstat.core.event.payload.board.BoardRankingUpdatedEventPayload
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.DefaultStringTuple
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.connection.StringRedisConnection.StringTuple
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository

@Repository
class RedisBoardIdListRepository(
    private val redisTemplate: StringRedisTemplate,
    private val cursorPaginationScript: RedisScript<List<*>>
) : BoardIdListRepository {
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

        val likeRankingKeys = mapOf(
            BoardRankingPeriod.DAY to BOARDS_BY_LIKES_DAY_KEY,
            BoardRankingPeriod.WEEK to BOARDS_BY_LIKES_WEEK_KEY,
            BoardRankingPeriod.MONTH to BOARDS_BY_LIKES_MONTH_KEY
        )
        val viewRankingKeys = mapOf(
            BoardRankingPeriod.DAY to BOARDS_BY_VIEWS_DAY_KEY,
            BoardRankingPeriod.WEEK to BOARDS_BY_VIEWS_WEEK_KEY,
            BoardRankingPeriod.MONTH to BOARDS_BY_VIEWS_MONTH_KEY
        )

        fun getRankingKey(metric: BoardRankingMetric, period: BoardRankingPeriod): String? {
            return when (metric) {
                BoardRankingMetric.LIKES -> likeRankingKeys[period]
                BoardRankingMetric.VIEWS -> viewRankingKeys[period]
            }
        }

        // 인터페이스(또는 공유되는 경우 CoreConstants)의 제한 상수
        const val RANKING_LIMIT_SIZE = CoreConstants.RANKING_LIMIT_SIZE
    }

    override fun getAllBoardsKey() = ALL_BOARDS_KEY
    override fun getCategoryKey(categoryId: Long) = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)

    // ----------------------------
    // A) 읽기 (Query) 로직
    // ----------------------------
    override fun readAllByTimeByOffset(pageable: Pageable): Page<Long> {
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(ALL_BOARDS_KEY) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(ALL_BOARDS_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByTimeByCursor(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(ALL_BOARDS_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(ALL_BOARDS_KEY),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun readAllByCategoryByOffset(categoryId: Long, pageable: Pageable): Page<Long> {
        val key = getCategoryKey(categoryId)
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(key) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByCategoryByCursor(categoryId: Long, lastBoardId: Long?, limit: Long): List<Long> {
        val key = getCategoryKey(categoryId)
        
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(key),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun readAllByLikesDayByOffset(pageable: Pageable): Page<Long> {
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(BOARDS_BY_LIKES_DAY_KEY) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_LIKES_DAY_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByLikesDayByCursor(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_DAY_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(BOARDS_BY_LIKES_DAY_KEY),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun readAllByLikesWeekByOffset(pageable: Pageable): Page<Long> {
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(BOARDS_BY_LIKES_WEEK_KEY) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_LIKES_WEEK_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByLikesWeekByCursor(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_WEEK_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(BOARDS_BY_LIKES_WEEK_KEY),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun readAllByLikesMonthByOffset(pageable: Pageable): Page<Long> {
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(BOARDS_BY_LIKES_MONTH_KEY) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_LIKES_MONTH_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByLikesMonthByCursor(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_MONTH_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(BOARDS_BY_LIKES_MONTH_KEY),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun readAllByViewsDayByOffset(pageable: Pageable): Page<Long> {
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(BOARDS_BY_VIEWS_DAY_KEY) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_VIEWS_DAY_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByViewsDayByCursor(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_DAY_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(BOARDS_BY_VIEWS_DAY_KEY),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun readAllByViewsWeekByOffset(pageable: Pageable): Page<Long> {
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(BOARDS_BY_VIEWS_WEEK_KEY) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_VIEWS_WEEK_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByViewsWeekByCursor(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(BOARDS_BY_VIEWS_WEEK_KEY),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    override fun readAllByViewsMonthByOffset(pageable: Pageable): Page<Long> {
        val offset = pageable.offset
        val pageSize = pageable.pageSize.toLong()
        
        val totalSize = redisTemplate.opsForZSet().size(BOARDS_BY_VIEWS_MONTH_KEY) ?: 0
        
        val content = redisTemplate.opsForZSet()
            .reverseRangeByScore(BOARDS_BY_VIEWS_MONTH_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize)
            ?.map { it.toLong() } ?: emptyList()
            
        return PageImpl(content, pageable, totalSize)
    }

    override fun readAllByViewsMonthByCursor(lastBoardId: Long?, limit: Long): List<Long> {
        if (lastBoardId == null) {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        }

        val result = redisTemplate.execute(
            cursorPaginationScript,
            listOf(BOARDS_BY_VIEWS_MONTH_KEY),
            lastBoardId.toString(),
            limit.toString()
        )

        return result.mapNotNull { (it as? String)?.toLongOrNull() }
    }

    // -----------------------------
    // B) 쓰기 (파이프라인, eventTs 기반)
    // -----------------------------
    override fun addBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        score: Double,
    ) {
        conn.zAdd(ALL_BOARDS_KEY, score, boardId.toString())
        conn.zRemRange(ALL_BOARDS_KEY, 0, -(ALL_BOARD_LIMIT_SIZE + 1))
    }

    override fun addBoardToCategoryInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long,
        score: Double,
    ) {
        val catKey = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)
        conn.zAdd(catKey, score, boardId.toString())
        conn.zRemRange(catKey, 0, -(CATEGORY_LIMIT_SIZE + 1))
    }

    override fun removeBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long?,
    ) {
        conn.zRem(ALL_BOARDS_KEY, boardId.toString())
        if (categoryId != null) {
            val catKey = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)
            conn.zRem(catKey, boardId.toString())
        }
    }

    override fun replaceRankingListInPipeline(
        conn: StringRedisConnection,
        key: String,
        rankings: List<BoardRankingUpdatedEventPayload.RankingEntry>
    ) {
        // 1. 기존 랭킹 리스트 삭제
        conn.del(key)

        // 2. 새로운 랭킹 항목이 있으면 모두 추가
        if (rankings.isNotEmpty()) {
            val stringTuples: Set<StringTuple> = rankings.map { entry ->
                DefaultStringTuple(
                    entry.boardId.toString(), // 멤버(값)를 String으로
                    entry.score             // 점수를 Double로
                )
            }.toSet() // Set으로 수집

            // Set<StringTuple>을 받는 zAdd 오버로드 사용
            conn.zAdd(key, stringTuples) // <-- 올바른 사용법

            // 3. 최대 크기로 리스트 트림
            conn.zRemRange(key, 0, -(RANKING_LIMIT_SIZE + 1))
        }
        // rankings가 비어있으면 DEL 작업이 리스트를 효과적으로 지움
    }

//    override fun addLikesRankingInPipeline(
//        conn: StringRedisConnection,
//        boardId: Long,
//        likeScore: Double
//    ) {
//        conn.zAdd(BOARDS_BY_LIKES_DAY_KEY, likeScore, boardId.toString())
//        conn.zRemRange(BOARDS_BY_LIKES_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1)) // <-- 올바른 메서드
//
//        conn.zAdd(BOARDS_BY_LIKES_WEEK_KEY, likeScore, boardId.toString())
//        conn.zRemRange(BOARDS_BY_LIKES_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1)) // <-- 올바른 메서드
//
//        conn.zAdd(BOARDS_BY_LIKES_MONTH_KEY, likeScore, boardId.toString())
//        conn.zRemRange(BOARDS_BY_LIKES_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1)) // <-- 올바른 메서드
//    }
//
//    override fun incrLikesRankingInPipeline(
//        conn: StringRedisConnection,
//        boardId: Long,
//        likeDelta: Double
//    ) {
//        conn.zIncrBy(BOARDS_BY_LIKES_DAY_KEY, likeDelta, boardId.toString())
//        conn.zRemRange(BOARDS_BY_LIKES_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//
//        conn.zIncrBy(BOARDS_BY_LIKES_WEEK_KEY, likeDelta, boardId.toString())
//        conn.zRemRange(BOARDS_BY_LIKES_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//
//        conn.zIncrBy(BOARDS_BY_LIKES_MONTH_KEY, likeDelta, boardId.toString())
//        conn.zRemRange(BOARDS_BY_LIKES_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//    }
//
//    override fun addViewsRankingInPipeline(
//        conn: StringRedisConnection,
//        boardId: Long,
//        viewScore: Double
//    ) {
//        conn.zAdd(BOARDS_BY_VIEWS_DAY_KEY, viewScore, boardId.toString())
//        conn.zRemRange(BOARDS_BY_VIEWS_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//
//        conn.zAdd(BOARDS_BY_VIEWS_WEEK_KEY, viewScore, boardId.toString())
//        conn.zRemRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//
//        conn.zAdd(BOARDS_BY_VIEWS_MONTH_KEY, viewScore, boardId.toString())
//        conn.zRemRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//    }
//
//    override fun incrViewsRankingInPipeline(
//        conn: StringRedisConnection,
//        boardId: Long,
//        viewDelta: Double
//    ) {
//        conn.zIncrBy(BOARDS_BY_VIEWS_DAY_KEY, viewDelta, boardId.toString())
//        conn.zRemRange(BOARDS_BY_VIEWS_DAY_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//
//        conn.zIncrBy(BOARDS_BY_VIEWS_WEEK_KEY, viewDelta, boardId.toString())
//        conn.zRemRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//
//        conn.zIncrBy(BOARDS_BY_VIEWS_MONTH_KEY, viewDelta, boardId.toString())
//        conn.zRemRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, -(RANKING_LIMIT_SIZE + 1))
//    }

}