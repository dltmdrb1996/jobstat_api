package com.example.jobstat.community.board.service

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.repository.BoardRankingQueryResult
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 게시글 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
internal interface BoardService {
    // ========== 게시글 CRUD 작업 ==========

    fun createBoard(
        title: String,
        content: String,
        author: String,
        categoryId: Long,
        password: String? = null,
        userId: Long? = null,
    ): Board

    fun updateBoard(
        id: Long,
        title: String,
        content: String,
    ): Board

    fun deleteBoard(id: Long)

    // ========== 기본 게시글 조회 작업 ==========

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

    /**
     * 조회수 기준 상위 게시글 조회
     */
    fun getTopNBoardsByViews(limit: Int): List<Board>

    fun searchBoards(
        keyword: String,
        pageable: Pageable,
    ): Page<Board>

    /**
     * 작성자별 게시글 수 조회
     */
    fun countBoardsByAuthor(author: String): Long

    fun getBoardsByIds(ids: List<Long>): List<Board>

    // ========== 커서 기반 페이징 조회 작업 ==========

    /**
     * 특정 ID 이후의 게시글 조회 (커서 기반 페이징)
     */
    fun getBoardsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    /**
     * 특정 카테고리에서 ID 이후의 게시글 조회 (커서 기반 페이징)
     */
    fun getBoardsByCategoryAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    /**
     * 특정 작성자의 ID 이후 게시글 조회 (커서 기반 페이징)
     */
    fun getBoardsByAuthorAfter(
        author: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    /**
     * 키워드 검색 결과에서 ID 이후 게시글 조회 (커서 기반 페이징)
     */
    fun searchBoardsAfter(
        keyword: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    // ========== 랭킹 관련 작업 ==========

    /**
     * 지표 및 기간별 게시글 ID 랭킹 조회 (오프셋 기반 페이징)
     */
    fun getBoardIdsRankedByMetric(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        pageable: Pageable,
    ): Page<Long>

    /**
     * 지표 및 기간별 특정 ID 이후 게시글 ID 랭킹 조회 (커서 기반 페이징)
     */
    fun getBoardIdsRankedByMetricAfter(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>

    /**
     * 지표 및 기간별 게시글 랭킹 결과 조회 (점수 포함)
     */
    fun getBoardRankingsForPeriod(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        limit: Long,
    ): List<BoardRankingQueryResult>
}
