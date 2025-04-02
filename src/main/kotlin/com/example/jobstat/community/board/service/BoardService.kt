package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.entity.Board
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

internal interface BoardService {
    fun createBoard(
        title: String,
        content: String,
        author: String,
        categoryId: Long?,
        password: String?,
        userId: Long? = null,
    ): Board

    fun getBoard(id: Long): Board

    fun getBoardsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board>

    fun getBoardsByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun getBoardsByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun getAllBoards(pageable: Pageable): Page<Board>

    fun getAllBoardsWithComments(pageable: Pageable): Page<Board>

    fun getTopNBoardsByViews(limit: Int): List<Board>

    fun searchBoards(
        keyword: String,
        pageable: Pageable,
    ): Page<Board>

    fun countBoardsByAuthor(author: String): Long

    fun updateBoard(
        id: Long,
        title: String,
        content: String,
    ): Board

    fun deleteBoard(id: Long)

    fun incrementViewCount(boardId: Long): Board

    fun incrementLikeCount(boardId: Long): Board
    
    fun getBoardsByIds(ids: List<Long>): List<Board>
}
