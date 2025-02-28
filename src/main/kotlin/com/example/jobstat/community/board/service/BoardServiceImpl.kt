package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.BoardConstants
import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.entity.ReadBoard
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
    ): ReadBoard {
        val category =
            categoryId?.let { categoryRepository.findById(it) }
                ?: throw AppException.fromErrorCode(
                    ErrorCode.INVALID_ARGUMENT,
                    BoardConstants.ErrorMessages.CATEGORY_REQUIRED,
                )

        val board = Board.create(title, content, author, password, category, userId)
        return boardRepository.save(board)
    }

    @Transactional(readOnly = true)
    override fun getBoardById(id: Long): ReadBoard = boardRepository.findById(id)

    @Transactional(readOnly = true)
    override fun getBoardsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<ReadBoard> = boardRepository.findByAuthor(author, pageable).map { it as ReadBoard }

    @Transactional(readOnly = true)
    override fun getBoardsByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<ReadBoard> = boardRepository.findByCategory(categoryId, pageable).map { it as ReadBoard }

    @Transactional(readOnly = true)
    override fun getBoardsByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<ReadBoard> = boardRepository.findByAuthorAndCategory(author, categoryId, pageable).map { it as ReadBoard }

    @Transactional(readOnly = true)
    override fun getAllBoards(pageable: Pageable): Page<ReadBoard> = boardRepository.findAll(pageable).map { it as ReadBoard }

    @Transactional(readOnly = true)
    override fun getAllBoardsWithComments(pageable: Pageable): Page<ReadBoard> = boardRepository.findAllWithDetails(pageable).map { it as ReadBoard }

    override fun updateBoard(
        id: Long,
        title: String,
        content: String,
    ): ReadBoard {
        val board = boardRepository.findById(id)
        board.updateContent(title, content)
        return boardRepository.save(board)
    }

    override fun deleteBoard(id: Long) {
        boardRepository.deleteById(id)
    }

    override fun incrementViewCount(boardId: Long): ReadBoard {
        val board = boardRepository.findById(boardId)
        board.incrementViewCount()
        return boardRepository.save(board)
    }

    override fun incrementLikeCount(boardId: Long): ReadBoard {
        val board = boardRepository.findById(boardId)
        board.incrementLikeCount()
        return boardRepository.save(board)
    }

    @Transactional(readOnly = true)
    override fun getTopNBoardsByViews(limit: Int): List<ReadBoard> =
        boardRepository.findTopNByOrderByViewCountDesc(
            limit.coerceAtMost(BoardConstants.MAX_POPULAR_BOARDS_LIMIT),
        )

    @Transactional(readOnly = true)
    override fun searchBoards(
        keyword: String,
        pageable: Pageable,
    ): Page<ReadBoard> = boardRepository.search(keyword, pageable).map { it as ReadBoard }

    @Transactional(readOnly = true)
    override fun countBoardsByAuthor(author: String): Long = boardRepository.countByAuthor(author)

    @Transactional(readOnly = true)
    override fun isBoardTitleDuplicated(
        author: String,
        title: String,
    ): Boolean = boardRepository.existsByAuthorAndTitle(author, title)
}
