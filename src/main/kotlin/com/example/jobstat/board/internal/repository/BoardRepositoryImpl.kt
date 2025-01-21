package com.example.jobstat.board.internal.repository

import com.example.jobstat.board.internal.entity.Board
import com.example.jobstat.core.extension.orThrowNotFound
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

interface BoardJpaRepository : JpaRepository<Board, Long> {
    fun findByAuthor(author: String): List<Board>

    @Query("SELECT b FROM Board b ORDER BY b.viewCount DESC LIMIT :n")
    fun findTopNByOrderByViewCountDesc(n: Int): List<Board>

    fun findByTitleContainingOrContentContaining(
        title: String,
        content: String,
    ): List<Board>
}

@Repository
internal class BoardRepositoryImpl(
    private val boardJpaRepository: BoardJpaRepository,
) : BoardRepository {
    override fun save(board: Board): Board = boardJpaRepository.save(board)

    override fun findById(id: Long): Board = boardJpaRepository.findById(id).orThrowNotFound("Board", id)

    override fun findByAuthor(author: String): List<Board> = boardJpaRepository.findByAuthor(author)

    override fun findAll(): List<Board> = boardJpaRepository.findAll()

    override fun deleteById(id: Long) = boardJpaRepository.deleteById(id)

    override fun delete(board: Board) = boardJpaRepository.delete(board)

    override fun findTopNByOrderByViewCountDesc(n: Int): List<Board> = boardJpaRepository.findTopNByOrderByViewCountDesc(n)

    override fun findByTitleContainingOrContentContaining(keyword: String): List<Board> =
        boardJpaRepository.findByTitleContainingOrContentContaining(keyword, keyword)
}
