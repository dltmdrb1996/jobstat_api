package com.example.jobstat.community.internal.service

import com.example.jobstat.community.internal.entity.ReadBoard
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BoardService {
    fun createBoard(
        title: String,
        content: String,
        author: String,
        categoryId: Long?,
        password: String?,
        userId: Long? = null,
    ): ReadBoard

    fun getBoardById(id: Long): ReadBoard

    fun getBoardsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<ReadBoard>

    fun getBoardsByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<ReadBoard>

    fun getBoardsByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<ReadBoard>

    fun getAllBoards(pageable: Pageable): Page<ReadBoard>

    fun getAllBoardsWithComments(pageable: Pageable): Page<ReadBoard>

    fun updateBoard(
        id: Long,
        title: String,
        content: String,
    ): ReadBoard

    fun deleteBoard(id: Long)

    fun incrementViewCount(boardId: Long): ReadBoard

    fun incrementLikeCount(boardId: Long): ReadBoard

    fun getTopNBoardsByViews(limit: Int): List<ReadBoard>

    fun searchBoards(
        keyword: String,
        pageable: Pageable,
    ): Page<ReadBoard>

    fun countBoardsByAuthor(author: String): Long

    fun isBoardTitleDuplicated(
        author: String,
        title: String,
    ): Boolean
}
