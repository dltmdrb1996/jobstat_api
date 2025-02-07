package com.example.jobstat.board.fake.repository

import com.example.jobstat.board.fake.BoardFixture
import com.example.jobstat.board.internal.entity.Board
import com.example.jobstat.board.internal.repository.BoardRepository
import com.example.jobstat.utils.IndexManager
import com.example.jobstat.utils.base.BaseFakeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

internal class FakeBoardRepository : BoardRepository {
    private val baseRepo =
        object : BaseFakeRepository<Board, BoardFixture>() {
            override fun fixture() = BoardFixture.aBoard()

            override fun createNewEntity(entity: Board): Board {
                if (!isValidId(entity.id)) {
                    setEntityId(entity, nextId())
                }
                return entity
            }

            override fun updateEntity(entity: Board): Board = entity

            override fun clearAdditionalState() {
                authorTitleIndex.clear()
            }
        }
    private val authorTitleIndex = IndexManager<Pair<String, String>, Long>()

    override fun save(board: Board): Board {
        val savedBoard = baseRepo.save(board)
        authorTitleIndex.put(Pair(savedBoard.author, savedBoard.title), savedBoard.id)
        return savedBoard
    }

    override fun findById(id: Long): Board = baseRepo.findById(id)

    override fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board> {
        val boards = baseRepo.findAll().filter { it.author == author }
        return PageImpl(applyPageable(boards, pageable), pageable, boards.size.toLong())
    }

    override fun findAll(pageable: Pageable): Page<Board> {
        val boards = applyPageable(baseRepo.findAll(), pageable)
        return PageImpl(boards, pageable, baseRepo.findAll().size.toLong())
    }

    override fun deleteById(id: Long) {
        val board = baseRepo.findByIdOrNull(id) ?: return
        delete(board)
    }

    override fun delete(board: Board) {
        authorTitleIndex.remove(Pair(board.author, board.title))
        baseRepo.delete(board)
    }

    override fun findTopNByOrderByViewCountDesc(n: Int): List<Board> = baseRepo.findAll().sortedByDescending { it.viewCount }.take(n)

    override fun search(
        keyword: String?,
        pageable: Pageable,
    ): Page<Board> {
        val filtered =
            if (keyword == null) {
                baseRepo.findAll()
            } else {
                baseRepo.findAll().filter {
                    it.title.contains(keyword, ignoreCase = true) ||
                        it.content.contains(keyword, ignoreCase = true)
                }
            }
        val boards = applyPageable(filtered, pageable)
        return PageImpl(boards, pageable, filtered.size.toLong())
    }

    override fun findAllWithDetails(pageable: Pageable): Page<Board> = findAll(pageable)

    override fun findByCategoryIdWithComments(categoryId: Long): List<Board> = baseRepo.findAll().filter { it.category.id == categoryId }

    override fun findByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> {
        val filtered = baseRepo.findAll().filter { it.category.id == categoryId }
        return PageImpl(applyPageable(filtered, pageable), pageable, filtered.size.toLong())
    }

    override fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> {
        val filtered = baseRepo.findAll().filter { it.author == author && it.category.id == categoryId }
        return PageImpl(applyPageable(filtered, pageable), pageable, filtered.size.toLong())
    }

    override fun countByAuthor(author: String): Long = baseRepo.findAll().count { it.author == author }.toLong()

    override fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean = authorTitleIndex.get(Pair(author, title)) != null

    private fun <T> applyPageable(
        list: List<T>,
        pageable: Pageable,
    ): List<T> {
        val start = pageable.pageNumber * pageable.pageSize
        val end = minOf(start + pageable.pageSize, list.size)
        return if (start <= end) list.subList(start, end) else emptyList()
    }

    fun clear() {
        baseRepo.clear()
        authorTitleIndex.clear()
    }
}
