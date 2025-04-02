package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.community.board.repository.CategoryRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
internal class BoardServiceImpl(
    private val boardRepository: BoardRepository,
    private val categoryRepository: CategoryRepository,
) : BoardService {
    override fun createBoard(
        title: String,
        content: String,
        author: String,
        categoryId: Long?,
        password: String?,
        userId: Long?,
    ): Board {
        val category =
            categoryId?.let { categoryRepository.findById(it) }
                ?: throw AppException.fromErrorCode(
                    ErrorCode.INVALID_ARGUMENT,
                    BoardConstants.ErrorMessages.CATEGORY_REQUIRED,
                )

        return Board.create(title, content, author, password, category, userId)
            .let(boardRepository::save)
    }

    @Transactional(readOnly = true)
    override fun getBoard(id: Long): Board = boardRepository.findById(id)
    

    @Transactional(readOnly = true)
    override fun getBoardsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board> = boardRepository.findByAuthor(author, pageable).map { it as Board }

    @Transactional(readOnly = true)
    override fun getBoardsByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardRepository.findByCategory(categoryId, pageable).map { it as Board }

    @Transactional(readOnly = true)
    override fun getBoardsByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardRepository.findByAuthorAndCategory(author, categoryId, pageable).map { it as Board }

    @Transactional(readOnly = true)
    override fun getAllBoards(pageable: Pageable): Page<Board> = boardRepository.findAll(pageable).map { it as Board }

    @Transactional(readOnly = true)
    override fun getAllBoardsWithComments(pageable: Pageable): Page<Board> = boardRepository.findAllWithDetails(pageable).map { it as Board }

    override fun updateBoard(
        id: Long,
        title: String,
        content: String,
    ): Board = boardRepository.findById(id).apply {
        updateContent(title, content)
    }

    override fun deleteBoard(id: Long) {
        boardRepository.deleteById(id)
    }

    override fun incrementViewCount(boardId: Long): Board = 
        boardRepository.findById(boardId).apply {
            incrementViewCount()
        }

    override fun incrementLikeCount(boardId: Long): Board = 
        boardRepository.findById(boardId).apply {
            incrementLikeCount()
        }

    @Transactional(readOnly = true)
    override fun getTopNBoardsByViews(limit: Int): List<Board> =
        boardRepository.findTopNByOrderByViewCountDesc(
            limit.coerceAtMost(BoardConstants.MAX_POPULAR_BOARDS_LIMIT),
        )

    @Transactional(readOnly = true)
    override fun searchBoards(
        keyword: String,
        pageable: Pageable,
    ): Page<Board> = boardRepository.search(keyword, pageable).map { it as Board }

    @Transactional(readOnly = true)
    override fun countBoardsByAuthor(author: String): Long = boardRepository.countByAuthor(author)

    @Transactional(readOnly = true)
    override fun getBoardsByIds(ids: List<Long>): List<Board> = boardRepository.findAllByIds(ids)
}
