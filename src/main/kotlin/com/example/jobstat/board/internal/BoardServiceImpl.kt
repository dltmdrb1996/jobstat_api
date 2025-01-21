package com.example.jobstat.board.internal

import com.example.jobstat.board.BoardService
import com.example.jobstat.board.internal.entity.Board
import com.example.jobstat.board.internal.repository.BoardRepository
import org.springframework.stereotype.Service

@Service
internal class BoardServiceImpl(
    private val boardRepository: BoardRepository,
) : BoardService {
    override fun createBoard(
        title: String,
        content: String,
        author: String,
    ): Board {
        val board = Board.create(title, content, author)
        return boardRepository.save(board)
    }

    override fun getBoardById(id: Long): Board? = boardRepository.findById(id)

    override fun getBoardsByAuthor(author: String): List<Board> = boardRepository.findByAuthor(author)

    override fun getAllBoards(): List<Board> = boardRepository.findAll()

    override fun updateBoard(board: Board): Board = boardRepository.save(board)

    override fun deleteBoard(id: Long) {
        boardRepository.deleteById(id)
    }

    override fun incrementViewCount(boardId: Long) {
        val board = boardRepository.findById(boardId)
        board.incrementViewCount()
        boardRepository.save(board)
    }

    override fun incrementLikeCount(boardId: Long): Int {
        val board = boardRepository.findById(boardId)
        board.incrementLikeCount()
        boardRepository.save(board)
        return board.likeCount
    }

    override fun getTopNBoardsByViews(n: Int): List<Board> = boardRepository.findTopNByOrderByViewCountDesc(n)

    override fun searchBoards(keyword: String): List<Board> = boardRepository.findByTitleContainingOrContentContaining(keyword)
}
