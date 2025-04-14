package com.example.jobstat.community.board.repository

import com.example.jobstat.community.board.entity.Board
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * 게시글 관련 데이터 액세스를 위한 리포지토리 인터페이스
 */
internal interface BoardRepository {
    // ========== 기본 CRUD 작업 ==========

    fun save(board: Board): Board

    fun findById(id: Long): Board

    fun findAll(pageable: Pageable): Page<Board>

    fun deleteById(id: Long)

    // ========== 기본 조회 작업 ==========

    fun existsById(id: Long): Boolean

    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board>

    fun findByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    /**
     * 조회수 기준 상위 게시글 조회
     */
    fun findTopNByOrderByViewCountDesc(limit: Int): List<Board>

    /**
     * 댓글 포함 모든 게시글 조회
     */
    fun findAllWithDetails(pageable: Pageable): Page<Board>

    /**
     * 키워드로 게시글 검색
     */
    fun search(
        keyword: String,
        pageable: Pageable,
    ): Page<Board>

    fun countByAuthor(author: String): Long

    /**
     * 작성자와 제목으로 게시글 존재 여부 확인
     */
    fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean

    fun findViewCountById(id: Long): Int?

    fun findLikeCountById(id: Long): Int?

    fun findAllByCategoryId(categoryId: Long): List<Board>

    fun findAllByIds(ids: List<Long>): List<Board>

    // ========== 커서 기반 페이징 조회 작업 ==========

    /**
     * 특정 ID 이후의 게시글 조회 (커서 기반 페이징)
     */
    fun findBoardsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    /**
     * 특정 카테고리에서 ID 이후의 게시글 조회 (커서 기반 페이징)
     */
    fun findBoardsByCategoryAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board>

    /**
     * 특정 작성자의 ID 이후 게시글 조회 (커서 기반 페이징)
     */
    fun findBoardsByAuthorAfter(
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

    // ========== 업데이트 작업 ==========

    fun updateViewCount(
        boardId: Long,
        count: Int,
    )

    fun updateLikeCount(
        boardId: Long,
        count: Int,
    )

    // ========== 랭킹 관련 작업 ==========

    /**
     * 좋아요 기준 게시글 ID 랭킹 조회 (오프셋 기반 페이징)
     */
    fun findBoardIdsRankedByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long>

    /**
     * 조회수 기준 게시글 ID 랭킹 조회 (오프셋 기반 페이징)
     */
    fun findBoardIdsRankedByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long>

    /**
     * 좋아요 기준 특정 ID 이후 게시글 ID 랭킹 조회 (커서 기반 페이징)
     */
    fun findBoardIdsRankedByLikesAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>

    /**
     * 조회수 기준 특정 ID 이후 게시글 ID 랭킹 조회 (커서 기반 페이징)
     */
    fun findBoardIdsRankedByViewsAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastBoardId: Long?,
        limit: Int,
    ): List<Long>

    /**
     * 조회수 기준 게시글 랭킹 결과 조회 (점수 포함)
     */
    fun findBoardRankingsByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult>

    /**
     * 좋아요 기준 게시글 랭킹 결과 조회 (점수 포함)
     */
    fun findBoardRankingsByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult>
}

/**
 * 게시글 랭킹 쿼리 결과 인터페이스
 */
interface BoardRankingQueryResult {
    fun getBoardId(): Long

    fun getScore(): Long
}
