package com.wildrew.app.community.board.repository

import com.wildrew.app.community.board.entity.Board
import com.wildrew.app.community.board.fixture.BoardFixture
import com.wildrew.app.community.board.utils.model.BoardRankingQueryResult
import com.wildrew.app.community.board.utils.model.BoardRankingQueryResultImpl
import com.wildrew.app.utils.base.BaseFakeRepository
import org.springframework.data.domain.*
import java.time.LocalDateTime
import kotlin.math.min

class FakeBoardRepository : BoardRepository {
    private val baseRepo =
        object : BaseFakeRepository<Board, BoardFixture>() {
            override fun fixture(): BoardFixture = BoardFixture.aBoard()

            override fun createNewEntity(entity: Board): Board {
                val currentId = entity.id
                if (currentId == 0L) {
                    try {
                        setEntityId(entity, nextId())
                    } catch (e: Exception) {
                        System.err.println("ERROR: Failed to set ID for Board entity via reflection. Ensure ID is set before saving or Fixture handles it.")
                        throw IllegalStateException("Failed to assign ID to Board entity.", e)
                    }
                }
                return entity
            }

            // 업데이트는 원본 객체 그대로 사용
            override fun updateEntity(entity: Board): Board = entity
        }

    override fun save(board: Board): Board {
        val savedBoard = baseRepo.save(board)
        return savedBoard
    }

    override fun findById(id: Long): Board = baseRepo.findById(id)

    override fun existsById(id: Long): Boolean = baseRepo.existsById(id)

    override fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board> {
        val filtered = baseRepo.findAll().filter { it.author == author }
        val content = applyPageable(filtered, pageable) // 아래 페이징 헬퍼 사용
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findAll(pageable: Pageable): Page<Board> {
        val allBoards = baseRepo.findAll()
        val content = applyPageable(allBoards, pageable)
        return PageImpl(content, pageable, allBoards.size.toLong())
    }

    override fun deleteById(id: Long) {
        baseRepo.deleteById(id)
    }

    override fun findTopNByOrderByViewCountDesc(limit: Int): List<Board> =
        baseRepo
            .findAll()
            .sortedWith(
                compareByDescending<Board> { it.viewCount }
                    .thenByDescending { it.id },
            ).take(limit)

    override fun findAllWithDetails(pageable: Pageable): Page<Board> = findAll(pageable)

    override fun search(
        keyword: String,
        pageable: Pageable,
    ): Page<Board> {
        val filtered =
            baseRepo.findAll().filter {
                it.title.contains(keyword, ignoreCase = true) || it.content.contains(keyword, ignoreCase = true)
            }
        val content = applyPageable(filtered, pageable)
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> {
        val filtered = baseRepo.findAll().filter { it.category.id == categoryId }
        val content = applyPageable(filtered, pageable)
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> {
        val filtered = baseRepo.findAll().filter { it.author == author && it.category.id == categoryId }
        val content = applyPageable(filtered, pageable)
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun countByAuthor(author: String): Long = baseRepo.findAll().count { it.author == author }.toLong()

    override fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean = baseRepo.findAll().any { it.author == author && it.title == title }

    override fun findViewCountById(id: Long): Int? = baseRepo.findByIdOrNull(id)?.viewCount

    override fun findLikeCountById(id: Long): Int? = baseRepo.findByIdOrNull(id)?.likeCount

    override fun updateViewCount(
        boardId: Long,
        count: Int,
    ) {
        baseRepo.findByIdOrNull(boardId)?.let {
            it.incrementViewCount(count)
            baseRepo.save(it)
        }
    }

    override fun updateLikeCount(
        boardId: Long,
        count: Int,
    ) {
        baseRepo.findByIdOrNull(boardId)?.let {
            it.incrementLikeCount(count)
            baseRepo.save(it)
        }
    }

    override fun findAllByCategoryId(categoryId: Long): List<Board> = baseRepo.findAll().filter { it.category.id == categoryId }

    override fun findAllByIds(ids: List<Long>): List<Board> = ids.mapNotNull { baseRepo.findByIdOrNull(it) }

    override fun findBoardsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> {
        val sorted = baseRepo.findAll().sortedByDescending { it.id }
        val startIndex = if (lastBoardId == null) 0 else sorted.indexOfFirst { it.id == lastBoardId } + 1
        return if (startIndex in sorted.indices) {
            sorted.subList(startIndex, min(startIndex + limit, sorted.size))
        } else {
            emptyList()
        }
    }

    override fun findBoardsByCategoryAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> {
        val filtered = baseRepo.findAll().filter { it.category.id == categoryId }
        val sorted = filtered.sortedByDescending { it.id }
        val startIndex = if (lastBoardId == null) 0 else sorted.indexOfFirst { it.id == lastBoardId } + 1
        return if (startIndex in sorted.indices) {
            sorted.subList(startIndex, min(startIndex + limit, sorted.size))
        } else {
            emptyList()
        }
    }

    override fun findBoardsByAuthorAfter(
        author: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> {
        val filtered = baseRepo.findAll().filter { it.author == author }
        val sorted = filtered.sortedByDescending { it.id }
        val startIndex = if (lastBoardId == null) 0 else sorted.indexOfFirst { it.id == lastBoardId } + 1
        return if (startIndex in sorted.indices) {
            sorted.subList(startIndex, min(startIndex + limit, sorted.size))
        } else {
            emptyList()
        }
    }

    override fun searchBoardsAfter(
        keyword: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> {
        val filtered =
            baseRepo.findAll().filter {
                it.title.contains(keyword, ignoreCase = true) || it.content.contains(keyword, ignoreCase = true)
            }
        val sorted = filtered.sortedByDescending { it.id }
        val startIndex = if (lastBoardId == null) 0 else sorted.indexOfFirst { it.id == lastBoardId } + 1
        return if (startIndex in sorted.indices) {
            sorted.subList(startIndex, min(startIndex + limit, sorted.size))
        } else {
            emptyList()
        }
    }

    override fun findBoardIdsRankedByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long> {
        val filtered = baseRepo.findAll().filter { it.createdAt in startTime..endTime }
        val sorted = filtered.sortedWith(compareByDescending<Board> { it.likeCount }.thenByDescending { it.id })

        val start = pageable.offset.toInt()
        val end = min(start + pageable.pageSize, sorted.size)

        val contentIds =
            if (start < sorted.size && start < end) {
                sorted.subList(start, end).map { it.id }
            } else {
                emptyList()
            }
        return PageImpl(contentIds, pageable, sorted.size.toLong())
    }

    override fun findBoardIdsRankedByLikesAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastScore: Int?,
        lastId: Long?,
        limit: Int,
    ): List<Long> {
        val filtered = baseRepo.findAll().filter { it.createdAt in startTime..endTime }
        val sorted = filtered.sortedWith(compareByDescending<Board> { it.likeCount }.thenByDescending { it.id })

        val condition: (Board) -> Boolean = { board ->
            if (lastId == null || lastScore == null) {
                true
            } else {
                (board.likeCount < lastScore) || (board.likeCount == lastScore && board.id < lastId)
            }
        }
        val filteredList = sorted.filter(condition)
        val resultIds = filteredList.take(limit).map { it.id }
        return resultIds
    }

    override fun findBoardIdsRankedByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long> {
        val filtered = baseRepo.findAll().filter { it.createdAt in startTime..endTime }
        val sorted = filtered.sortedWith(compareByDescending<Board> { it.viewCount }.thenByDescending { it.id })

        val start = pageable.offset.toInt()
        val end = min(start + pageable.pageSize, sorted.size)
        val contentIds =
            if (start < sorted.size && start < end) {
                sorted.subList(start, end).map { it.id }
            } else {
                emptyList()
            }
        return PageImpl(contentIds, pageable, filtered.size.toLong())
    }

    override fun findBoardIdsRankedByViewsAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastScore: Int?,
        lastId: Long?,
        limit: Int,
    ): List<Long> {
        val filtered = baseRepo.findAll().filter { it.createdAt in startTime..endTime }
        val sorted = filtered.sortedWith(compareByDescending<Board> { it.viewCount }.thenByDescending { it.id })

        return sorted
            .filter { board ->
                if (lastId == null || lastScore == null) {
                    true
                } else {
                    (board.viewCount < lastScore) || (board.viewCount == lastScore && board.id < lastId)
                }
            }.take(limit)
            .map { it.id }
    }

    override fun findBoardRankingsByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult> {
        val filtered = baseRepo.findAll().filter { it.createdAt in startTime..endTime }
        val sorted = filtered.sortedWith(compareByDescending<Board> { it.likeCount }.thenByDescending { it.id })

        val start = pageable.offset.toInt()
        val end = min(start + pageable.pageSize, sorted.size)
        val content =
            if (start < sorted.size && start < end) {
                sorted.subList(start, end).map { BoardRankingQueryResultImpl(it.id, it.likeCount) }
            } else {
                emptyList()
            }
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findBoardRankingsByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult> {
        val filtered = baseRepo.findAll().filter { it.createdAt in startTime..endTime }
        val sorted = filtered.sortedWith(compareByDescending<Board> { it.viewCount }.thenByDescending { it.id })

        val start = pageable.offset.toInt()
        val end = min(start + pageable.pageSize, sorted.size)
        val content =
            if (start < sorted.size && start < end) {
                sorted.subList(start, end).map { BoardRankingQueryResultImpl(it.id, it.viewCount) }
            } else {
                emptyList()
            }
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    private fun <T> applyPageable(
        list: List<T>,
        pageable: Pageable,
    ): List<T> {
        val sortedList =
            if (pageable.sort.isSorted) {
                list.sortedWith { o1, o2 ->
                    pageable.sort
                        .map { order ->
                            compareValuesBy(o1, o2) { entity ->
                                when (order.property) {
                                    "id" -> (entity as? Board)?.id ?: 0L
                                    "createdAt" -> (entity as? Board)?.createdAt?.toString() ?: ""
                                    "likeCount" -> (entity as? Board)?.likeCount ?: 0
                                    "viewCount" -> (entity as? Board)?.viewCount ?: 0
                                    else -> 0
                                }
                            }.let { if (order.isDescending) -it else it }
                        }.sum()
                }
            } else {
                list.sortedByDescending { (it as? Board)?.id ?: 0L }
            }

        val start = pageable.offset.toInt()
        val end = min(start + pageable.pageSize, sortedList.size)
        return if (start < sortedList.size && start < end) sortedList.subList(start, end) else emptyList()
    }

    fun saveWithTimestamp(
        board: Board,
        createdAt: LocalDateTime,
    ): Board {
        baseRepo.setCreatedAt(board, createdAt)
        return baseRepo.save(board)
    }

    fun clear() {
        baseRepo.clear()
    }
}
