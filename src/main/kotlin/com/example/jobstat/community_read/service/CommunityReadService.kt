package com.example.jobstat.community_read.service

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CommunityReadService {
    // --- 게시글 관련 조회 ---
    // 단일 게시글 조회
    fun getBoardByIdWithFetch(boardId: Long): BoardReadModel

    fun getBoardByIdWithComments(boardId: Long): BoardReadModel

    fun getBoardByIdsWithFetch(boardIds: List<Long>): List<BoardReadModel>

    // 최신 게시글 조회
    fun getLatestBoardsByOffset(pageable: Pageable): Page<BoardReadModel>

    fun getLatestBoardsByCursor(
        lastBoardId: Long?,
        limit: Long,
    ): List<BoardReadModel>

    // 카테고리별 게시글 조회
    fun getCategoryBoardsByOffset(
        categoryId: Long,
        pageable: Pageable,
    ): Page<BoardReadModel>

    fun getCategoryBoardsByCursor(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Long,
    ): List<BoardReadModel>

    // 랭킹별 게시글 조회

    /**
     * 지표(좋아요/조회수)와 기간(일/주/월)에 따른 게시글 랭킹을 오프셋 기반으로 조회합니다.
     * @param metric 조회할 지표 (LIKES, VIEWS)
     * @param period 조회할 기간 (DAY, WEEK, MONTH)
     * @param pageable 페이지 정보
     * @return 페이징 처리된 게시글 목록
     */
    fun getRankedBoardsByOffset(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        pageable: Pageable,
    ): Page<BoardReadModel>

    /**
     * 지표(좋아요/조회수)와 기간(일/주/월)에 따른 게시글 랭킹을 커서 기반으로 조회합니다.
     * @param metric 조회할 지표 (LIKES, VIEWS)
     * @param period 조회할 기간 (DAY, WEEK, MONTH)
     * @param lastBoardId 마지막으로 조회된 게시글 ID (다음 페이지 요청 시 사용)
     * @param lastScore 마지막으로 조회된 게시글의 점수 (DB Fallback 시 정확도 향상을 위해 사용될 수 있음)
     * @param limit 조회할 개수
     * @return 게시글 목록
     */
    fun getRankedBoardsByCursor(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        lastBoardId: Long?,
        limit: Long,
    ): List<BoardReadModel>

    // --- 댓글 관련 조회 ---
    // 게시글별 댓글 조회
    fun getCommentsByBoardIdByOffset(
        boardId: Long,
        pageable: Pageable,
    ): Page<CommentReadModel>

    fun getCommentsByBoardIdByCursor(
        boardId: Long,
        lastCommentId: Long?,
        limit: Long,
    ): List<CommentReadModel>

    // 단일 댓글 조회
    fun getCommentById(commentId: Long): CommentReadModel

    fun getCommentsByIds(commentIds: List<Long>): List<CommentReadModel>
}
