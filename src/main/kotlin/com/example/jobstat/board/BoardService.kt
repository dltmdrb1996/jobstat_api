package com.example.jobstat.board

import com.example.jobstat.board.internal.entity.Board

interface BoardService {
    fun createBoard(
        title: String,
        content: String,
        author: String,
    ): Board

    fun getBoardById(id: Long): Board?

    fun getBoardsByAuthor(author: String): List<Board>

    fun getAllBoards(): List<Board>

    fun updateBoard(board: Board): Board

    fun deleteBoard(id: Long)

    fun incrementViewCount(boardId: Long)

    fun incrementLikeCount(boardId: Long): Int

    fun getTopNBoardsByViews(n: Int): List<Board>

    fun searchBoards(keyword: String): List<Board>
}
