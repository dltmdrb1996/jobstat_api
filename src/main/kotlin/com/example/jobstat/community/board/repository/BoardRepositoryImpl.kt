package com.example.jobstat.community.board.repository

import com.example.jobstat.community.board.entity.Board
import com.example.jobstat.community.board.utils.model.BoardRankingQueryResult
import com.example.jobstat.core.core_jpa_base.orThrowNotFound
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

internal interface BoardJpaRepository : JpaRepository<Board, Long> {
    fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board>

    @Query("SELECT b FROM Board b ORDER BY b.viewCount DESC LIMIT :limit")
    fun findTopNByOrderByViewCountDesc(
        @Param("limit") limit: Int,
    ): List<Board>

    @Query(
        """
        SELECT DISTINCT b 
        FROM Board b 
        LEFT JOIN FETCH b.comments c 
        LEFT JOIN FETCH b.category 
        WHERE (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) 
            OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """,
    )
    fun search(
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<Board>

    @Query("SELECT DISTINCT b FROM Board b LEFT JOIN FETCH b.comments LEFT JOIN FETCH b.category")
    fun findAllWithDetails(pageable: Pageable): Page<Board>

    @Query("SELECT b FROM Board b WHERE b.category.id = :categoryId")
    fun findByCategory(
        @Param("categoryId") categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    @Query("SELECT b FROM Board b WHERE b.author = :author AND b.category.id = :categoryId")
    fun findByAuthorAndCategory(
        @Param("author") author: String,
        @Param("categoryId") categoryId: Long,
        pageable: Pageable,
    ): Page<Board>

    fun countByAuthor(author: String): Long

    fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean

    @Query("SELECT b.viewCount FROM Board b WHERE b.id = :boardId")
    fun findViewCountById(
        @Param("boardId") boardId: Long,
    ): Int?

    @Query("SELECT b.likeCount FROM Board b WHERE b.id = :boardId")
    fun findLikeCountById(
        @Param("boardId") boardId: Long,
    ): Int?

    @Modifying
    @Query("UPDATE Board b SET b.viewCount = b.viewCount + :count WHERE b.id = :boardId")
    fun updateViewCount(
        @Param("boardId") boardId: Long,
        @Param("count") count: Int,
    )

    @Modifying
    @Query("UPDATE Board b SET b.likeCount = b.likeCount + :count WHERE b.id = :boardId")
    fun updateLikeCount(
        @Param("boardId") boardId: Long,
        @Param("count") count: Int,
    )

    @Query("SELECT b FROM Board b WHERE b.category.id = :categoryId")
    fun findAllByCategoryId(
        @Param("categoryId") categoryId: Long,
    ): List<Board>

    @Query("SELECT b FROM Board b WHERE b.id IN :ids")
    fun findAllByIdIn(
        @Param("ids") ids: List<Long>,
    ): List<Board>

    // 다음 메서드들은 cursor-based 페이지네이션을 위한 메서드입니다.
    @Query("SELECT b FROM Board b WHERE (:lastBoardId IS NULL OR b.id < :lastBoardId) ORDER BY b.id DESC")
    fun findBoardsAfter(
        @Param("lastBoardId") lastBoardId: Long?,
        pageable: Pageable,
    ): List<Board>

    @Query("SELECT b FROM Board b WHERE b.category.id = :categoryId AND (:lastBoardId IS NULL OR b.id < :lastBoardId) ORDER BY b.id DESC")
    fun findBoardsByCategoryAfter(
        @Param("categoryId") categoryId: Long,
        @Param("lastBoardId") lastBoardId: Long?,
        pageable: Pageable,
    ): List<Board>

    @Query("SELECT b FROM Board b WHERE b.author = :author AND (:lastBoardId IS NULL OR b.id < :lastBoardId) ORDER BY b.id DESC")
    fun findBoardsByAuthorAfter(
        @Param("author") author: String,
        @Param("lastBoardId") lastBoardId: Long?,
        pageable: Pageable,
    ): List<Board>

    @Query(
        """
        SELECT b FROM Board b 
        WHERE (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) 
           OR LOWER(b.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
           AND (:lastBoardId IS NULL OR b.id < :lastBoardId)
        ORDER BY b.id DESC
    """,
    )
    fun searchBoardsAfter(
        @Param("keyword") keyword: String,
        @Param("lastBoardId") lastBoardId: Long?,
        pageable: Pageable,
    ): List<Board>

    @Query(
        """
        SELECT b.id FROM Board b
        WHERE b.createdAt BETWEEN :startTime AND :endTime
          AND ((:lastId IS NULL)
               OR (b.likeCount < :lastScore)
               OR (b.likeCount = :lastScore AND b.id < :lastId))
        ORDER BY b.likeCount DESC, b.id DESC
        """,
    )
    fun findBoardIdsRankedByLikesAfter(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        @Param("lastScore") lastScore: Int?,
        @Param("lastId") lastId: Long?,
        pageable: Pageable,
    ): List<Long>

    @Query(
        """
        SELECT b.id FROM Board b
        WHERE b.createdAt BETWEEN :startTime AND :endTime
          AND ((:lastId IS NULL)
               OR (b.viewCount < :lastScore)
               OR (b.viewCount = :lastScore AND b.id < :lastId))
        ORDER BY b.viewCount DESC, b.id DESC
        """,
    )
    fun findBoardIdsRankedByViewsAfter(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        @Param("lastScore") lastScore: Int?,
        @Param("lastId") lastId: Long?,
        pageable: Pageable,
    ): List<Long>

    @Query(
        """
        SELECT b.id FROM Board b
        WHERE b.createdAt BETWEEN :startTime AND :endTime
        ORDER BY b.likeCount DESC, b.id DESC
    """,
    )
    fun findBoardIdsRankedByLikes(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long> // ID만 반환하도록 수정

    // Offset 기반 - 조회수 순
    @Query(
        """
        SELECT b.id FROM Board b
        WHERE b.createdAt BETWEEN :startTime AND :endTime
        ORDER BY b.viewCount DESC, b.id DESC
    """,
    )
    fun findBoardIdsRankedByViews(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long> // ID만 반환하도록 수정

    @Query("SELECT b.id as boardId, b.likeCount as score FROM Board b WHERE b.createdAt BETWEEN :startTime AND :endTime ORDER BY b.likeCount DESC, b.id DESC")
    fun findBoardRankingsByLikes(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult>

    @Query("SELECT b.id as boardId, b.viewCount as score FROM Board b WHERE b.createdAt BETWEEN :startTime AND :endTime ORDER BY b.viewCount DESC, b.id DESC")
    fun findBoardRankingsByViews(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult>
}

@Repository
internal class BoardRepositoryImpl(
    private val boardJpaRepository: BoardJpaRepository,
) : BoardRepository {
    override fun save(board: Board): Board = boardJpaRepository.save(board)

    override fun findById(id: Long): Board = boardJpaRepository.findById(id).orThrowNotFound("Board", id)

    override fun existsById(id: Long): Boolean = boardJpaRepository.existsById(id)

    override fun findByAuthor(
        author: String,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.findByAuthor(author, pageable)

    override fun findAll(pageable: Pageable): Page<Board> = boardJpaRepository.findAll(pageable)

    override fun deleteById(id: Long) = boardJpaRepository.deleteById(id)

    override fun findTopNByOrderByViewCountDesc(limit: Int): List<Board> = boardJpaRepository.findTopNByOrderByViewCountDesc(limit)

    override fun findAllWithDetails(pageable: Pageable): Page<Board> = boardJpaRepository.findAllWithDetails(pageable)

    override fun search(
        keyword: String,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.search(keyword, pageable)

    override fun findByCategory(
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.findByCategory(categoryId, pageable)

    override fun findByAuthorAndCategory(
        author: String,
        categoryId: Long,
        pageable: Pageable,
    ): Page<Board> = boardJpaRepository.findByAuthorAndCategory(author, categoryId, pageable)

    override fun countByAuthor(author: String): Long = boardJpaRepository.countByAuthor(author)

    override fun existsByAuthorAndTitle(
        author: String,
        title: String,
    ): Boolean = boardJpaRepository.existsByAuthorAndTitle(author, title)

    override fun findViewCountById(id: Long): Int? = boardJpaRepository.findViewCountById(id)

    override fun findLikeCountById(id: Long): Int? = boardJpaRepository.findLikeCountById(id)

    override fun updateViewCount(
        boardId: Long,
        count: Int,
    ) {
        boardJpaRepository.updateViewCount(boardId, count)
    }

    override fun updateLikeCount(
        boardId: Long,
        count: Int,
    ) {
        boardJpaRepository.updateLikeCount(boardId, count)
    }

    override fun findAllByCategoryId(categoryId: Long): List<Board> = boardJpaRepository.findAllByCategoryId(categoryId)

    override fun findAllByIds(ids: List<Long>): List<Board> = boardJpaRepository.findAllByIdIn(ids)

    override fun findBoardsAfter(
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardJpaRepository.findBoardsAfter(lastBoardId, Pageable.ofSize(limit))

    override fun findBoardsByCategoryAfter(
        categoryId: Long,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardJpaRepository.findBoardsByCategoryAfter(categoryId, lastBoardId, Pageable.ofSize(limit))

    override fun findBoardsByAuthorAfter(
        author: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardJpaRepository.findBoardsByAuthorAfter(author, lastBoardId, Pageable.ofSize(limit))

    override fun searchBoardsAfter(
        keyword: String,
        lastBoardId: Long?,
        limit: Int,
    ): List<Board> = boardJpaRepository.searchBoardsAfter(keyword, lastBoardId, Pageable.ofSize(limit))

    override fun findBoardIdsRankedByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long> = boardJpaRepository.findBoardIdsRankedByLikes(startTime, endTime, pageable)

    override fun findBoardIdsRankedByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<Long> = boardJpaRepository.findBoardIdsRankedByViews(startTime, endTime, pageable)

    override fun findBoardIdsRankedByLikesAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastScore: Int?,
        lastId: Long?,
        limit: Int,
    ): List<Long> =
        boardJpaRepository.findBoardIdsRankedByLikesAfter(
            startTime,
            endTime,
            lastScore,
            lastId,
            PageRequest.of(0, limit),
        )

    override fun findBoardIdsRankedByViewsAfter(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        lastScore: Int?,
        lastId: Long?,
        limit: Int,
    ): List<Long> =
        boardJpaRepository.findBoardIdsRankedByViewsAfter(
            startTime,
            endTime,
            lastScore,
            lastId,
            PageRequest.of(0, limit),
        )

    override fun findBoardRankingsByLikes(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult> = boardJpaRepository.findBoardRankingsByLikes(startTime, endTime, pageable)

    override fun findBoardRankingsByViews(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable,
    ): Page<BoardRankingQueryResult> = boardJpaRepository.findBoardRankingsByViews(startTime, endTime, pageable)
}
