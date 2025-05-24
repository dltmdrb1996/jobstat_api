package com.wildrew.app.community_read.repository.fake

import com.wildrew.app.community_read.repository.CommentIdListRepository
import com.wildrew.app.community_read.repository.impl.RedisCommentIdListRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.StringRedisConnection
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class FakeCommentIdListRepository : CommentIdListRepository {
    private val zsets = ConcurrentHashMap<String, NavigableMap<Double, String>>()

    private fun getMap(boardId: Long): NavigableMap<Double, String> = zsets.computeIfAbsent(RedisCommentIdListRepository.getBoardCommentsKey(boardId)) { Collections.synchronizedNavigableMap(TreeMap<Double, String>()) }

    private fun getRangeDescByRank(
        boardId: Long,
        start: Long,
        end: Long,
    ): List<Long> {
        val map = getMap(boardId)
        val descendingValues = map.descendingMap().values.toList()
        val s = start.toInt()
        val e = min(end.toInt() + 1, descendingValues.size)
        return if (s >= descendingValues.size || s < 0 || s >= e) emptyList() else descendingValues.subList(s, e).mapNotNull { it.toLongOrNull() }
    }

    private fun getRangeByScoreDesc(
        boardId: Long,
        offset: Long,
        limit: Long,
    ): List<Long> {
        val map = getMap(boardId)
        val descendingValues = map.descendingMap().values.toList()
        val start = offset.toInt()
        val end = min(start + limit.toInt(), descendingValues.size)
        return if (start >= descendingValues.size || start < 0 || start >= end) emptyList() else descendingValues.subList(start, end).mapNotNull { it.toLongOrNull() }
    }

    private fun getRangeByCursorDesc(
        boardId: Long,
        lastMemberId: Long?,
        limit: Long,
    ): List<Long> {
        val map = getMap(boardId)
        val descendingEntries = map.descendingMap().entries.toList()
        val rank = if (lastMemberId == null) -1 else descendingEntries.indexOfFirst { it.value == lastMemberId.toString() }
        if (rank == -1 && lastMemberId != null) return emptyList()
        val startIdx = rank + 1
        val endIdx = rank + limit.toInt()
        return getRangeDescByRank(boardId, startIdx.toLong(), endIdx.toLong())
    }

    private fun getRangeByScoreDescInfinite(
        boardId: Long,
        lastCommentId: Long,
        limit: Long,
    ): List<Long> {
        val map = getMap(boardId)
        val paddedLastId = "%019d".format(lastCommentId)
        val lastScore = map.entries.find { it.value == paddedLastId }?.key ?: return emptyList()

        val results =
            map
                .descendingMap()
                .entries
                .filter { it.key < lastScore }
                .take(limit.toInt())
                .mapNotNull { it.value.toLongOrNull() }
        return results
    }

    override fun readAllByBoard(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long> {
        val content = getRangeDescByRank(boardId, pageable.offset, pageable.offset + pageable.pageSize - 1)
        return PageImpl(content, pageable, getMap(boardId).size.toLong())
    }

    override fun readAllByBoardInfiniteScroll(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long> =
        if (lastCommentId == null) {
            getRangeDescByRank(boardId, 0, limit - 1)
        } else {
            getRangeByScoreDescInfinite(boardId, lastCommentId, limit)
        }

    override fun readCommentsByBoardId(
        boardId: Long,
        pageable: Pageable,
    ): Page<Long> {
        val content = getRangeByScoreDesc(boardId, pageable.offset, pageable.pageSize.toLong())
        return PageImpl(content, pageable, getMap(boardId).size.toLong())
    }

    override fun readCommentsByBoardIdByCursor(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<Long> = getRangeByCursorDesc(boardId, lastCommentId, limit)

    override fun getBoardCommentsKey(boardId: Long): String = RedisCommentIdListRepository.getBoardCommentsKey(boardId)

    fun internalAdd(
        boardId: Long,
        commentId: Long,
        score: Double,
    ) {
        val map = getMap(boardId)
        val member = commentId.toString()
        map.entries.removeIf { it.value == member && it.key != score }
        map[score] = member
        while (map.size > RedisCommentIdListRepository.COMMENT_LIMIT_SIZE) {
            map.remove(map.firstKey())
        }
    }

    override fun addCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
        score: Double,
    ) {
        internalAdd(boardId, commentId, score)
    }

    fun internalRemove(
        boardId: Long,
        commentId: Long,
    ) {
        val map = getMap(boardId)
        map.entries.removeIf { it.value == commentId.toString() }
    }

    override fun removeCommentInPipeline(
        conn: StringRedisConnection,
        boardId: Long,
        commentId: Long,
    ) {
        internalRemove(boardId, commentId)
    }

    private fun toPaddedString(id: Long): String = "%019d".format(id)

    override fun add(
        boardId: Long,
        commentId: Long,
        sortValue: Double,
    ) {
        val map = getMap(boardId)
        val member = toPaddedString(commentId)
        val currentScore = map.entries.find { it.value == member }?.key
        if (currentScore == null || currentScore != sortValue) {
            map.entries.removeIf { it.value == member }
            map[sortValue] = member
        }
        while (map.size > RedisCommentIdListRepository.COMMENT_LIMIT_SIZE) {
            map.remove(map.firstKey())
        }
    }

    override fun delete(
        boardId: Long,
        commentId: Long,
    ) {
        val map = getMap(boardId)
        map.entries.removeIf { it.value == toPaddedString(commentId) }
    }

    fun getMapForBoard(boardId: Long): NavigableMap<Double, String> = getMap(boardId)

    fun clear() {
        zsets.clear()
    }
}
