package com.example.jobstat.community_read.repository

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.temporal.ChronoUnit

@Repository
class RedisBoardIdListRepository(
    private val redisTemplate: StringRedisTemplate
) : BoardIdListRepository {

    companion object {
        const val ALL_BOARDS_KEY = "community-read::board-list"
        const val CATEGORY_BOARDS_KEY_FORMAT = "community-read::category::%s::board-list"
        const val BOARDS_BY_LIKES_DAY_KEY = "community-read::likes::day::board-list"
        const val BOARDS_BY_LIKES_WEEK_KEY = "community-read::likes::week::board-list"
        const val BOARDS_BY_LIKES_MONTH_KEY = "community-read::likes::month::board-list"
        const val BOARDS_BY_VIEWS_DAY_KEY = "community-read::views::day::board-list"
        const val BOARDS_BY_VIEWS_WEEK_KEY = "community-read::views::week::board-list"
        const val BOARDS_BY_VIEWS_MONTH_KEY = "community-read::views::month::board-list"
    }

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun add(boardId: Long, sortValue: Double, limit: Long) {
        try {
            redisTemplate.executePipelined { connection ->
                val stringConn = connection as StringRedisConnection
                val existingScore = stringConn.zScore(ALL_BOARDS_KEY, toPaddedString(boardId))
                if (existingScore != null && existingScore == sortValue) {
                    log.info("Board id {} already exists with same sortValue.", boardId)
                } else {
                    stringConn.zAdd(ALL_BOARDS_KEY, sortValue, toPaddedString(boardId))
                }
                stringConn.zRemRange(ALL_BOARDS_KEY, 0, -(limit + 1))
                null
            }
        } catch (e: Exception) {
            log.error("게시글 ID 리스트 추가 실패: boardId={}, sortValue={}", boardId, sortValue, e)
            throw e
        }
    }

    override fun delete(boardId: Long) {
        try {
            redisTemplate.opsForZSet().remove(ALL_BOARDS_KEY, toPaddedString(boardId))
        } catch (e: Exception) {
            log.error("게시글 ID 리스트 삭제 실패: boardId={}", boardId, e)
            throw e
        }
    }

    override fun readAllByTime(offset: Long, limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(ALL_BOARDS_KEY, offset, offset + limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("시간순 게시글 ID 리스트 조회 실패: offset={}, limit={}", offset, limit, e)
            throw e
        }
    }

    override fun readAllByTimeInfiniteScroll(lastBoardId: Long?, limit: Long): List<Long> {
        try {
            return if (lastBoardId == null) {
                redisTemplate.opsForZSet()
                    .reverseRange(ALL_BOARDS_KEY, 0, limit - 1)
                    ?.map { it.toLong() } ?: emptyList()
            } else {
                val lastBoardScore = redisTemplate.opsForZSet().score(ALL_BOARDS_KEY, toPaddedString(lastBoardId))
                if (lastBoardScore == null) {
                    emptyList()
                } else {
                    redisTemplate.opsForZSet()
                        .reverseRangeByScore(ALL_BOARDS_KEY, 0.0, lastBoardScore - 0.000001, 0, limit)
                        ?.map { it.toLong() } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            log.error("무한 스크롤 시간순 게시글 ID 리스트 조회 실패: lastBoardId={}, limit={}", lastBoardId, limit, e)
            throw e
        }
    }

    override fun addToCategoryList(categoryId: Long, boardId: Long, sortValue: Double) {
        try {
            val key = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)
            val existingScore = redisTemplate.opsForZSet().score(key, toPaddedString(boardId))
            if (existingScore != null && existingScore == sortValue) {
                log.info("Board id {} already exists in category {} with same sortValue.", boardId, categoryId)
            } else {
                redisTemplate.opsForZSet().add(key, toPaddedString(boardId), sortValue)
            }
        } catch (e: Exception) {
            log.error("카테고리별 게시글 ID 리스트 추가 실패: categoryId={}, boardId={}", categoryId, boardId, e)
            throw e
        }
    }

    override fun deleteFromCategoryList(categoryId: Long, boardId: Long) {
        try {
            redisTemplate.opsForZSet().remove(CATEGORY_BOARDS_KEY_FORMAT.format(categoryId), toPaddedString(boardId))
        } catch (e: Exception) {
            log.error("카테고리별 게시글 ID 리스트 삭제 실패: categoryId={}, boardId={}", categoryId, boardId, e)
            throw e
        }
    }

    override fun readAllByCategory(categoryId: Long, offset: Long, limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(CATEGORY_BOARDS_KEY_FORMAT.format(categoryId), offset, offset + limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("카테고리별 게시글 ID 리스트 조회 실패: categoryId={}, offset={}, limit={}", categoryId, offset, limit, e)
            throw e
        }
    }

    override fun readAllByCategoryInfiniteScroll(categoryId: Long, lastBoardId: Long?, limit: Long): List<Long> {
        try {
            val key = CATEGORY_BOARDS_KEY_FORMAT.format(categoryId)
            return if (lastBoardId == null) {
                redisTemplate.opsForZSet()
                    .reverseRange(key, 0, limit - 1)
                    ?.map { it.toLong() } ?: emptyList()
            } else {
                val lastBoardScore = redisTemplate.opsForZSet().score(key, toPaddedString(lastBoardId))
                if (lastBoardScore == null) {
                    emptyList()
                } else {
                    redisTemplate.opsForZSet()
                        .reverseRangeByScore(key, 0.0, lastBoardScore - 0.000001, 0, limit)
                        ?.map { it.toLong() } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            log.error("무한 스크롤 카테고리별 게시글 ID 리스트 조회 실패: categoryId={}, lastBoardId={}, limit={}", categoryId, lastBoardId, limit, e)
            throw e
        }
    }

    override fun addToLikesByDayList(boardId: Long, createdAt: Long, likeCount: Int) {
        try {
            val now = Instant.now()
            val boardCreatedAt = Instant.ofEpochMilli(createdAt)
            if (ChronoUnit.DAYS.between(boardCreatedAt, now) <= 1) {
                redisTemplate.executePipelined { connection ->
                    val stringConn = connection as StringRedisConnection
                    stringConn.zAdd(BOARDS_BY_LIKES_DAY_KEY, likeCount.toDouble(), toPaddedString(boardId))
                    stringConn.zRemRange(BOARDS_BY_LIKES_DAY_KEY, 0, -101)
                    null
                }
            }
        } catch (e: Exception) {
            log.error("일별 좋아요순 게시글 ID 리스트 추가 실패: boardId={}, likeCount={}", boardId, likeCount, e)
            throw e
        }
    }

    override fun addToLikesByWeekList(boardId: Long, createdAt: Long, likeCount: Int) {
        try {
            val now = Instant.now()
            val boardCreatedAt = Instant.ofEpochMilli(createdAt)
            if (ChronoUnit.DAYS.between(boardCreatedAt, now) <= 7) {
                redisTemplate.executePipelined { connection ->
                    val stringConn = connection as StringRedisConnection
                    stringConn.zAdd(BOARDS_BY_LIKES_WEEK_KEY, likeCount.toDouble(), toPaddedString(boardId))
                    stringConn.zRemRange(BOARDS_BY_LIKES_WEEK_KEY, 0, -101)
                    null
                }
            }
        } catch (e: Exception) {
            log.error("주간 좋아요순 게시글 ID 리스트 추가 실패: boardId={}, likeCount={}", boardId, likeCount, e)
            throw e
        }
    }

    override fun addToLikesByMonthList(boardId: Long, createdAt: Long, likeCount: Int) {
        try {
            val now = Instant.now()
            val boardCreatedAt = Instant.ofEpochMilli(createdAt)
            if (ChronoUnit.DAYS.between(boardCreatedAt, now) <= 30) {
                redisTemplate.executePipelined { connection ->
                    val stringConn = connection as StringRedisConnection
                    stringConn.zAdd(BOARDS_BY_LIKES_MONTH_KEY, likeCount.toDouble(), toPaddedString(boardId))
                    stringConn.zRemRange(BOARDS_BY_LIKES_MONTH_KEY, 0, -101)
                    null
                }
            }
        } catch (e: Exception) {
            log.error("월간 좋아요순 게시글 ID 리스트 추가 실패: boardId={}, likeCount={}", boardId, likeCount, e)
            throw e
        }
    }

    override fun deleteFromLikesList(boardId: Long) {
        try {
            redisTemplate.executePipelined { connection ->
                val stringConn = connection as StringRedisConnection
                stringConn.zRem(BOARDS_BY_LIKES_DAY_KEY, toPaddedString(boardId))
                stringConn.zRem(BOARDS_BY_LIKES_WEEK_KEY, toPaddedString(boardId))
                stringConn.zRem(BOARDS_BY_LIKES_MONTH_KEY, toPaddedString(boardId))
                null
            }
        } catch (e: Exception) {
            log.error("좋아요순 게시글 ID 리스트 삭제 실패: boardId={}", boardId, e)
            throw e
        }
    }

    override fun readAllByLikesDay(limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_DAY_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("일별 좋아요순 게시글 ID 리스트 조회 실패: limit={}", limit, e)
            throw e
        }
    }

    override fun readAllByLikesWeek(limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_WEEK_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("주간 좋아요순 게시글 ID 리스트 조회 실패: limit={}", limit, e)
            throw e
        }
    }

    override fun readAllByLikesMonth(limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_LIKES_MONTH_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("월간 좋아요순 게시글 ID 리스트 조회 실패: limit={}", limit, e)
            throw e
        }
    }

    override fun addToViewsByDayList(boardId: Long, createdAt: Long, viewCount: Int) {
        try {
            val now = Instant.now()
            val boardCreatedAt = Instant.ofEpochMilli(createdAt)
            if (ChronoUnit.DAYS.between(boardCreatedAt, now) <= 1) {
                redisTemplate.executePipelined { connection ->
                    val stringConn = connection as StringRedisConnection
                    stringConn.zAdd(BOARDS_BY_VIEWS_DAY_KEY, viewCount.toDouble(), toPaddedString(boardId))
                    stringConn.zRemRange(BOARDS_BY_VIEWS_DAY_KEY, 0, -101)
                    null
                }
            }
        } catch (e: Exception) {
            log.error("일별 조회수순 게시글 ID 리스트 추가 실패: boardId={}, viewCount={}", boardId, viewCount, e)
            throw e
        }
    }

    override fun addToViewsByWeekList(boardId: Long, createdAt: Long, viewCount: Int) {
        try {
            val now = Instant.now()
            val boardCreatedAt = Instant.ofEpochMilli(createdAt)
            if (ChronoUnit.DAYS.between(boardCreatedAt, now) <= 7) {
                redisTemplate.executePipelined { connection ->
                    val stringConn = connection as StringRedisConnection
                    stringConn.zAdd(BOARDS_BY_VIEWS_WEEK_KEY, viewCount.toDouble(), toPaddedString(boardId))
                    stringConn.zRemRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, -101)
                    null
                }
            }
        } catch (e: Exception) {
            log.error("주간 조회수순 게시글 ID 리스트 추가 실패: boardId={}, viewCount={}", boardId, viewCount, e)
            throw e
        }
    }

    override fun addToViewsByMonthList(boardId: Long, createdAt: Long, viewCount: Int) {
        try {
            val now = Instant.now()
            val boardCreatedAt = Instant.ofEpochMilli(createdAt)
            if (ChronoUnit.DAYS.between(boardCreatedAt, now) <= 30) {
                redisTemplate.executePipelined { connection ->
                    val stringConn = connection as StringRedisConnection
                    stringConn.zAdd(BOARDS_BY_VIEWS_MONTH_KEY, viewCount.toDouble(), toPaddedString(boardId))
                    stringConn.zRemRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, -101)
                    null
                }
            }
        } catch (e: Exception) {
            log.error("월간 조회수순 게시글 ID 리스트 추가 실패: boardId={}, viewCount={}", boardId, viewCount, e)
            throw e
        }
    }

    override fun deleteFromViewsList(boardId: Long) {
        try {
            redisTemplate.executePipelined { connection ->
                val stringConn = connection as StringRedisConnection
                stringConn.zRem(BOARDS_BY_VIEWS_DAY_KEY, toPaddedString(boardId))
                stringConn.zRem(BOARDS_BY_VIEWS_WEEK_KEY, toPaddedString(boardId))
                stringConn.zRem(BOARDS_BY_VIEWS_MONTH_KEY, toPaddedString(boardId))
                null
            }
        } catch (e: Exception) {
            log.error("조회수순 게시글 ID 리스트 삭제 실패: boardId={}", boardId, e)
            throw e
        }
    }

    override fun readAllByViewsDay(limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_DAY_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("일별 조회수순 게시글 ID 리스트 조회 실패: limit={}", limit, e)
            throw e
        }
    }

    override fun readAllByViewsWeek(limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_WEEK_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("주간 조회수순 게시글 ID 리스트 조회 실패: limit={}", limit, e)
            throw e
        }
    }

    override fun readAllByViewsMonth(limit: Long): List<Long> {
        try {
            return redisTemplate.opsForZSet()
                .reverseRange(BOARDS_BY_VIEWS_MONTH_KEY, 0, limit - 1)
                ?.map { it.toLong() } ?: emptyList()
        } catch (e: Exception) {
            log.error("월간 조회수순 게시글 ID 리스트 조회 실패: limit={}", limit, e)
            throw e
        }
    }

    private fun toPaddedString(id: Long): String {
        return "%019d".format(id)
    }
}
