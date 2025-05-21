package com.example.jobstat.community_read.repository.fake

import com.example.jobstat.community_read.repository.BoardIdListRepository
import com.example.jobstat.community_read.repository.impl.RedisBoardIdListRepository
import com.example.jobstat.community_read.repository.impl.RedisBoardIdListRepository.Companion.getCategoryKey
import com.example.jobstat.core.core_event.model.payload.board.BoardRankingUpdatedEventPayload
import com.example.jobstat.statistics_read.core.core_model.BoardRankingPeriod
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

internal class FakeBoardIdListRepository : BoardIdListRepository {
    private val zsets = ConcurrentHashMap<String, NavigableMap<Double, String>>()

    private fun getMap(key: String): NavigableMap<Double, String> = zsets.computeIfAbsent(key) { Collections.synchronizedNavigableMap(TreeMap<Double, String>()) }

    private fun getRangeDescByRank(
        key: String,
        start: Long,
        end: Long,
    ): List<Long> {
        val map = getMap(key)
        val descendingValues = map.descendingMap().values.toList()
        val s = start.toInt()
        val e = min(end.toInt() + 1, descendingValues.size)
        return if (s >= descendingValues.size || s < 0 || s >= e) {
            emptyList()
        } else {
            descendingValues.subList(s, e).mapNotNull { it.toLongOrNull() }
        }
    }

    private fun getRangeByScoreDesc(
        key: String,
        offset: Long,
        limit: Long,
    ): List<Long> {
        val map = getMap(key)
        val descendingValues = map.descendingMap().values.toList()
        val start = offset.toInt()
        val end = min(start + limit.toInt(), descendingValues.size)
        return if (start >= descendingValues.size || start < 0 || start >= end) {
            emptyList()
        } else {
            descendingValues.subList(start, end).mapNotNull { it.toLongOrNull() }
        }
    }

    private fun getRangeByCursorDesc(
        key: String,
        lastMemberId: Long?,
        limit: Long,
    ): List<Long> {
        val map = getMap(key)
        val descendingEntries = map.descendingMap().entries.toList()
        val rank = if (lastMemberId == null) -1 else descendingEntries.indexOfFirst { it.value == lastMemberId.toString() }
        if (rank == -1 && lastMemberId != null) return emptyList()
        val startIdx = rank + 1
        val endIdx = rank + limit.toInt()
        return getRangeDescByRank(key, startIdx.toLong(), endIdx.toLong())
    }

    override fun getAllBoardsKey() = RedisBoardIdListRepository.ALL_BOARDS_KEY

    override fun readAllByTimeByOffset(pageable: Pageable): Page<Long> = PageImpl(getRangeByScoreDesc(getAllBoardsKey(), pageable.offset, pageable.pageSize.toLong()), pageable, getMap(getAllBoardsKey()).size.toLong())

    override fun readAllByCategoryByOffset(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Long> = PageImpl(getRangeByScoreDesc(getCategoryKey(categoryId), pageable.offset, pageable.pageSize.toLong()), pageable, getMap(getCategoryKey(categoryId)).size.toLong())

    override fun readAllByLikesDayByOffset(pageable: Pageable): Page<Long> = PageImpl(getRangeByScoreDesc(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.DAY]!!, pageable.offset, pageable.pageSize.toLong()), pageable, getMap(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.DAY]!!).size.toLong())

    override fun readAllByLikesWeekByOffset(pageable: Pageable): Page<Long> = PageImpl(getRangeByScoreDesc(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.WEEK]!!, pageable.offset, pageable.pageSize.toLong()), pageable, getMap(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.WEEK]!!).size.toLong())

    override fun readAllByLikesMonthByOffset(pageable: Pageable): Page<Long> = PageImpl(getRangeByScoreDesc(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.MONTH]!!, pageable.offset, pageable.pageSize.toLong()), pageable, getMap(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.MONTH]!!).size.toLong())

    override fun readAllByViewsDayByOffset(pageable: Pageable): Page<Long> =
        PageImpl(
            getRangeByScoreDesc(
                RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.DAY]!!,
                pageable.offset,
                pageable.pageSize.toLong(),
            ),
            pageable,
            getMap(RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.DAY]!!).size.toLong(),
        )

    override fun readAllByViewsWeekByOffset(pageable: Pageable): Page<Long> =
        PageImpl(
            getRangeByScoreDesc(
                RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.WEEK]!!,
                pageable.offset,
                pageable.pageSize.toLong(),
            ),
            pageable,
            getMap(RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.WEEK]!!).size.toLong(),
        )

    override fun readAllByViewsMonthByOffset(pageable: Pageable): Page<Long> =
        PageImpl(
            getRangeByScoreDesc(
                RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.MONTH]!!,
                pageable.offset,
                pageable.pageSize.toLong(),
            ),
            pageable,
            getMap(RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.MONTH]!!).size.toLong(),
        )

    override fun readAllByTimeByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(getAllBoardsKey(), lastBoardId, limit)

    override fun readAllByCategoryByCursor(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(getCategoryKey(categoryId), lastBoardId, limit)

    override fun readAllByLikesDayByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.DAY]!!, lastBoardId, limit)

    override fun readAllByLikesWeekByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.WEEK]!!, lastBoardId, limit)

    override fun readAllByLikesMonthByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(RedisBoardIdListRepository.likeRankingKeys[BoardRankingPeriod.MONTH]!!, lastBoardId, limit)

    override fun readAllByViewsDayByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.DAY]!!, lastBoardId, limit)

    override fun readAllByViewsWeekByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.WEEK]!!, lastBoardId, limit)

    override fun readAllByViewsMonthByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(RedisBoardIdListRepository.viewRankingKeys[BoardRankingPeriod.MONTH]!!, lastBoardId, limit)

    fun internalAdd(
        key: String,
        score: Double,
        member: String,
        limit: Long,
    ) {
        val map = getMap(key)
        map.entries.removeIf { it.value == member && it.key != score }
        map[score] = member
        while (map.size > limit) {
            map.remove(map.firstKey())
        }
    }

    override fun addBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        score: Double,
    ) {
        internalAdd(getAllBoardsKey(), score, boardId.toString(), RedisBoardIdListRepository.ALL_BOARD_LIMIT_SIZE)
    }

    override fun addBoardToCategoryInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long,
        score: Double,
    ) {
        internalAdd(getCategoryKey(categoryId), score, boardId.toString(), RedisBoardIdListRepository.CATEGORY_LIMIT_SIZE)
    }

    fun internalRemove(
        key: String,
        member: String,
    ) {
        getMap(key).entries.removeIf { it.value == member }
    }

    override fun removeBoardInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        categoryId: Long?,
    ) {
        internalRemove(getAllBoardsKey(), boardId.toString())
        if (categoryId != null) {
            internalRemove(getCategoryKey(categoryId), boardId.toString())
        }
    }

    override fun replaceRankingListInPipeline(
        conn: StringRedisConnection,
        key: String,
        rankings: List<BoardRankingUpdatedEventPayload.RankingEntry>,
    ) {
        val map = getMap(key)
        map.clear()
        rankings.forEach { map[it.score] = it.boardId.toString() }
        while (map.size > RedisBoardIdListRepository.RANKING_LIMIT_SIZE) {
            map.remove(map.firstKey())
        }
    }

    fun getMapForKey(key: String): NavigableMap<Double, String> = zsets[key] ?: TreeMap()

    fun clear() {
        zsets.clear()
    }
}
