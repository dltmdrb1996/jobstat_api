package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.repository.BoardRankingQueryResult
import com.example.jobstat.community.board.repository.BoardRepository
import com.example.jobstat.community.board.repository.CategoryRepository
import com.example.jobstat.community.board.utils.BoardConstants
import com.example.jobstat.core.constants.CoreConstants
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.extension.orThrowNotFound
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 게시글 관련 비즈니스 로직을 처리하는 서비스 구현체
 */
@Service
internal class BoardServiceImpl(
    private val boardRepository: BoardRepository,
    private val categoryRepository: CategoryRepository,
) : BoardService {
    // ========== 게시글 CRUD 작업 ==========

    @Transactional
    override fun createBoard(
        title: String,
        content: String,
        author: String,
        categoryId: Long,
        password: String?,
        userId: Long?,
    ): Board {
        val category = categoryRepository.findById(categoryId)
        val board = Board.create(title, content, author, password, category, userId)
        return boardRepository.save(board)
    }

    @Transactional
    override fun updateBoard(
        id: Long,
        title: String,
        content: String,
    ): Board {
        val board = getBoardEntityWithNotFoundCheck(id)
        board.updateContent(title, content)
        return boardRepository.save(board)
    }

    @Transactional
    override fun deleteBoard(id: Long) {
        if (!boardRepository.existsById(id)) {
            AppException.fromErrorCode(
                errorCode = ErrorCode.RESOURCE_NOT_FOUND,
                message = "게시글이 존재하지 않습니다.",
            )
        }
        boardRepository.deleteById(id)
    }

    // ========== 기본 게시글 조회 작업 ==========

    @Transactional(readOnly = true)
    override fun getBoard(id: Long): Board = getBoardEntityWithNotFoundCheck(id)

    @Transactional(readOnly = true)
    override fun getBoardsByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board> = boardRepository.findByAuthor(author, pageable)

    @Transactional(readOnly = true)
    override fun getBoardsByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardRepository.findByCategory(categoryId, pageable)

    @Transactional(readOnly = true)
    override fun getBoardsByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardRepository.findByAuthorAndCategory(author, categoryId, pageable)

    @Transactional(readOnly = true)
    override fun getAllBoards(pageable: Pageable): Page<Board> = boardRepository.findAll(pageable)

    @Transactional(readOnly = true)
    override fun getAllBoardsWithComments(pageable: Pageable): Page<Board> = boardRepository.findAllWithDetails(pageable)

    @Transactional(readOnly = true)
    override fun getTopNBoardsByViews(limit: Int): List<Board> =
        boardRepository.findTopNByOrderByViewCountDesc(
            limit.coerceAtMost(BoardConstants.MAX_POPULAR_BOARDS_LIMIT),
        )

    @Transactional(readOnly = true)
    override fun searchBoards(
        keyword: String,
        pageable: Pageable,
    ): Page<Board> = boardRepository.search(keyword, pageable)

    @Transactional(readOnly = true)
    override fun countBoardsByAuthor(author: String): Long = boardRepository.countByAuthor(author)

    @Transactional(readOnly = true)
    override fun getBoardsByIds(ids: List<Long>): List<Board> = boardRepository.findAllByIds(ids)

    // ========== 커서 기반 페이징 조회 작업 ==========

    @Transactional(readOnly = true)
    override fun getBoardsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardRepository.findBoardsAfter(lastBoardId, limit)

    @Transactional(readOnly = true)
    override fun getBoardsByCategoryAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardRepository.findBoardsByCategoryAfter(categoryId, lastBoardId, limit)

    @Transactional(readOnly = true)
    override fun getBoardsByAuthorAfter(
        author: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardRepository.findBoardsByAuthorAfter(author, lastBoardId, limit)

    @Transactional(readOnly = true)
    override fun searchBoardsAfter(
        keyword: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardRepository.searchBoardsAfter(keyword, lastBoardId, limit)

    // ========== 랭킹 관련 작업 ==========

    @Transactional(readOnly = true)
    override fun getBoardIdsRankedByMetric(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        pageable: Pageable,
    ): Page<Long> {
        val (startTime, endTime) = calculateBoardRankingPeriod(period)
        return when (metric) {
            BoardRankingMetric.LIKES -> boardRepository.findBoardIdsRankedByLikes(startTime, endTime, pageable)
            BoardRankingMetric.VIEWS -> boardRepository.findBoardIdsRankedByViews(startTime, endTime, pageable)
        }
    }

    @Transactional(readOnly = true)
    override fun getBoardIdsRankedByMetricAfter(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long> {
        val (startTime, endTime) = calculateBoardRankingPeriod(period)

        return when (metric) {
            BoardRankingMetric.LIKES -> boardRepository.findBoardIdsRankedByLikesAfter(startTime, endTime, lastBoardId, limit)
            BoardRankingMetric.VIEWS -> boardRepository.findBoardIdsRankedByViewsAfter(startTime, endTime, lastBoardId, limit)
        }
    }

    @Transactional(readOnly = true)
    override fun getBoardRankingsForPeriod(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        limit: Long,
    ): List<BoardRankingQueryResult> {
        val (startTime, endTime) = calculateBoardRankingPeriod(period)
        val pageable = PageRequest.of(0, limit.coerceAtMost(CoreConstants.RANKING_LIMIT_SIZE).toInt())

        return when (metric) {
            BoardRankingMetric.LIKES -> boardRepository.findBoardRankingsByLikes(startTime, endTime, pageable).content
            BoardRankingMetric.VIEWS -> boardRepository.findBoardRankingsByViews(startTime, endTime, pageable).content
        }
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * 기간 Enum('DAY', 'WEEK', 'MONTH')을 기준으로 시작/종료 LocalDateTime 계산
     */
    private fun calculateBoardRankingPeriod(period: BoardRankingPeriod): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDateTime.now()
        val endTime = now

        val startTime =
            when (period) {
                BoardRankingPeriod.DAY -> now.minusDays(1) // Typically last 24 hours
                BoardRankingPeriod.WEEK -> now.minusDays(7)
                BoardRankingPeriod.MONTH -> now.minusMonths(1) // Or minusDays(30) depending on definition
            }
        return Pair(startTime, endTime)
    }

    /**
     * ID로 게시글 조회 후 없으면 예외 발생
     */
    private fun getBoardEntityWithNotFoundCheck(id: Long): Board =
        boardRepository.findById(id).orThrowNotFound(
            entityName = "Board",
            id = id,
        )
}
