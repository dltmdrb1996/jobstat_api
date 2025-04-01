package com.example.jobstat.community_read.service

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.client.*
import com.example.jobstat.community_read.repository.*
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

@Service
class CommunityReadService(
    private val boardIdListRepository: BoardIdListRepository,
    private val boardDetailRepository: BoardDetailRepository,
    private val boardCountRepository: BoardCountRepository,
    private val commentIdListRepository: CommentIdListRepository,
    private val commentDetailRepository: CommentDetailRepository,
    private val boardClient: BoardClient,
    private val commentClient: CommentClient,
    private val redisTemplate: RedisTemplate<String, String>
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    /**
     * 게시글 ID로 조회
     */
    fun getBoardById(boardId: Long): BoardReadModel {
        log.info("게시글 조회: boardId={}", boardId)
        return try {
            boardDetailRepository.read(boardId) ?: fetchAndSaveBoardFromOriginal(boardId)
                ?: throw AppException.fromErrorCode(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    message = "게시글을 찾을 수 없습니다",
                    detailInfo = "boardId: $boardId"
                )
        } catch (e: Exception) {
            log.error("게시글 조회 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "게시글 조회 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 원본 소스에서 게시글 정보를 가져와 저장
     */
    private fun fetchAndSaveBoardFromOriginal(boardId: Long): BoardReadModel? {
        log.info("Redis에 게시글이 없어 원본에서 조회: boardId={}", boardId)
        
        return try {
            val boardDetail = boardClient.getBoardDetail(boardId) 
                ?: throw AppException.fromErrorCode(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    message = "게시글을 찾을 수 없습니다",
                    detailInfo = "boardId: $boardId"
                )
            
            val commentCount = commentClient.getCommentCountByBoardId(boardId)
            val likeCount = likeClient.getLikeCount(boardId)
            
            val boardReadModel = BoardReadModel(
                id = boardDetail.id,
                title = boardDetail.title,
                content = boardDetail.content,
                author = boardDetail.author,
                categoryId = boardDetail.categoryId,
                categoryName = boardDetail.categoryName,
                viewCount = boardDetail.viewCount,
                likeCount = likeCount,
                commentCount = commentCount,
                createdAt = boardDetail.createdAt,
                updatedAt = boardDetail.updatedAt,
                isDeleted = false
            )
            
            // 트랜잭션으로 모든 Redis 작업을 묶음
            redisTemplate.execute { connection ->
                connection.multi()
                
                try {
                    // 상세 정보 저장
                    boardDetailRepository.create(boardReadModel, Duration.ofDays(7))
                    
                    // ID 목록 관련 저장소 업데이트
                    val createdAtMillis = boardDetail.createdAt.toEpochSecond() * 1000
                    boardIdListRepository.add(boardDetail.id, createdAtMillis.toDouble())
                    
                    boardDetail.categoryId?.let {
                        boardIdListRepository.addToCategoryList(it, boardDetail.id, createdAtMillis.toDouble())
                    }
                    
                    // 좋아요 및 조회수 기준 리스트 추가
                    boardIdListRepository.addToLikesByDayList(boardId, createdAtMillis, likeCount)
                    boardIdListRepository.addToLikesByWeekList(boardId, createdAtMillis, likeCount)
                    boardIdListRepository.addToLikesByMonthList(boardId, createdAtMillis, likeCount)
                    
                    boardIdListRepository.addToViewsByDayList(boardId, createdAtMillis, boardDetail.viewCount)
                    boardIdListRepository.addToViewsByWeekList(boardId, createdAtMillis, boardDetail.viewCount)
                    boardIdListRepository.addToViewsByMonthList(boardId, createdAtMillis, boardDetail.viewCount)
                    
                    connection.exec()
                } catch (e: Exception) {
                    connection.discard()
                    throw AppException.fromErrorCode(
                        ErrorCode.REDIS_OPERATION_FAILED,
                        message = "게시글 정보 저장 실패",
                        detailInfo = "boardId: $boardId"
                    )
                }
            }
            
            boardReadModel
        } catch (e: Exception) {
            log.error("원본 데이터 조회 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.EXTERNAL_SERVICE_ERROR,
                message = "원본 데이터 조회 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 모든 게시글 페이징 조회
     */
    fun getAllBoards(pageable: Pageable): Page<BoardReadModel> {
        val offset = pageable.offset
        val limit = pageable.pageSize.toLong()
        
        // ID 목록 가져오기
        val boardIds = boardIdListRepository.readAllByTime(offset, limit)
        
        // ID 목록이 비어있으면 원본에서 조회
        if (boardIds.isEmpty()) {
            return fetchBoardsFromOriginalSource(pageable)
        }
        
        // 상세 정보 가져오기
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        // 누락된 정보가 있으면 원본에서 가져와 저장
        val result = boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
        
        // 전체 게시글 수
        val totalCount = boardCountRepository.read(0) ?: fetchTotalBoardCount()
        
        return PageImpl(result, pageable, totalCount)
    }

    /**
     * 카테고리별 게시글 페이징 조회
     */
    fun getBoardsByCategory(categoryId: Long, pageable: Pageable): Page<BoardReadModel> {
        val offset = pageable.offset
        val limit = pageable.pageSize.toLong()
        
        val boardIds = boardIdListRepository.readAllByCategory(categoryId, offset, limit)
        
        if (boardIds.isEmpty()) {
            return fetchBoardsByCategoryFromOriginal(categoryId, pageable)
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        val result = boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
        
        val totalCount = boardCountRepository.read(categoryId) ?: fetchCategoryBoardCount(categoryId)
        
        return PageImpl(result, pageable, totalCount)
    }

    /**
     * 작성자별 게시글 페이징 조회
     */
    fun getBoardsByAuthor(author: String, pageable: Pageable): Page<BoardReadModel> {
        val offset = pageable.offset
        val limit = pageable.pageSize.toLong()
        
        val boardIds = boardIdListRepository.readAllByAuthor(author, offset, limit)
        
        if (boardIds.isEmpty()) {
            return fetchBoardsByAuthorFromOriginal(author, pageable)
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        val result = boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
        
        return PageImpl(result, pageable, result.size.toLong())
    }

    fun searchBoards(keyword: String, pageable: Pageable): Page<BoardReadModel> {
        val boards = boardClient.searchBoards(keyword, pageable)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    /**
     * 무한 스크롤 방식으로 게시글 목록 조회
     */
    fun getBoardsInfiniteScroll(lastBoardId: Long?, size: Int): List<BoardReadModel> {
        val boardIds = boardIdListRepository.readAllByTimeInfiniteScroll(lastBoardId, size.toLong())
        
        if (boardIds.isEmpty()) {
            return fetchBoardsInfiniteScrollFromOriginal(lastBoardId, size)
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        return boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
    }

    /**
     * 카테고리별 무한 스크롤 게시글 목록 조회
     */
    fun getBoardsByCategoryInfiniteScroll(categoryId: Long, lastBoardId: Long?, size: Int): List<BoardReadModel> {
        val boardIds = boardIdListRepository.readAllByCategoryInfiniteScroll(categoryId, lastBoardId, size.toLong())
        
        if (boardIds.isEmpty()) {
            return fetchBoardsByCategoryInfiniteScrollFromOriginal(categoryId, lastBoardId, size)
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        return boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
    }

    /**
     * 작성자별 무한 스크롤 게시글 목록 조회
     */
    fun getBoardsByAuthorInfiniteScroll(author: String, lastBoardId: Long?, size: Int): List<BoardReadModel> {
        val boardIds = boardIdListRepository.readAllByAuthorInfiniteScroll(author, lastBoardId, size.toLong())
        
        if (boardIds.isEmpty()) {
            return fetchBoardsByAuthorInfiniteScrollFromOriginal(author, lastBoardId, size)
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        return boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
    }

    /**
     * 검색 결과 무한 스크롤 조회
     */
    fun searchBoardsInfiniteScroll(keyword: String, lastBoardId: Long?, size: Int): List<BoardReadModel> {
        return boardClient.searchBoardsInfiniteScroll(keyword, lastBoardId, size)
            .also { boards ->
                boards.forEach { board ->
                    fetchAndSaveBoardFromOriginal(board.id)
                }
            }
    }

    /**
     * 인기 게시글 목록 조회 (좋아요 순) - 일간, 주간, 월간
     */
    fun getTopBoardsByLikes(period: String, limit: Int): List<BoardReadModel> {
        val boardIds = when (period.lowercase()) {
            "day" -> boardIdListRepository.readAllByLikesDay(limit.toLong())
            "week" -> boardIdListRepository.readAllByLikesWeek(limit.toLong())
            "month" -> boardIdListRepository.readAllByLikesMonth(limit.toLong())
            else -> boardIdListRepository.readAllByLikesWeek(limit.toLong()) // 기본값은 주간
        }
        
        if (boardIds.isEmpty()) {
            return fetchTopBoardsFromOriginal(limit, "likes")
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        return boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
    }

    /**
     * 인기 게시글 목록 조회 (조회수 순) - 일간, 주간, 월간
     */
    fun getTopBoardsByViews(period: String, limit: Int): List<BoardReadModel> {
        val boardIds = when (period.lowercase()) {
            "day" -> boardIdListRepository.readAllByViewsDay(limit.toLong())
            "week" -> boardIdListRepository.readAllByViewsWeek(limit.toLong())
            "month" -> boardIdListRepository.readAllByViewsMonth(limit.toLong())
            else -> boardIdListRepository.readAllByViewsWeek(limit.toLong()) // 기본값은 주간
        }
        
        if (boardIds.isEmpty()) {
            return fetchTopBoardsFromOriginal(limit, "views")
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        return boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
    }

    /**
     * 게시글 조회수 증가
     */
    fun incrementBoardViewCount(boardId: Long, userId: String?, createdAt: Long? = null): Int {
        return try {
            val viewCount = viewCountClient.incrementViewCount(boardId, userId)
            updateBoardViewCount(boardId, viewCount, createdAt ?: System.currentTimeMillis())
            viewCount
        } catch (e: Exception) {
            log.error("조회수 증가 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.VIEW_COUNT_UPDATE_FAILED,
                message = "조회수 증가 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 게시글 조회수 업데이트
     */
    fun updateBoardViewCount(boardId: Long, viewCount: Int, createdAt: Long) {
        log.info("게시글 조회수 업데이트: boardId={}, viewCount={}", boardId, viewCount)
        try {
            val board = getBoardById(boardId)
            val updatedBoard = board.copy(viewCount = viewCount)
            boardDetailRepository.update(updatedBoard)
            
            // 기간별 조회수 순위 업데이트
            boardIdListRepository.addToViewsByDayList(boardId, createdAt, viewCount)
            boardIdListRepository.addToViewsByWeekList(boardId, createdAt, viewCount)
            boardIdListRepository.addToViewsByMonthList(boardId, createdAt, viewCount)
            
            log.debug("게시글 조회수 갱신 완료: boardId={}, viewCount={}", boardId, viewCount)
        } catch (e: Exception) {
            log.error("조회수 업데이트 실패: boardId={}", boardId, e)
            throw CommunityReadException.ViewCountUpdateFailed(boardId, e)
        }
    }

    /**
     * 게시글 좋아요 수 업데이트
     */
    fun updateBoardLikeCount(boardId: Long, likeCount: Int, createdAt: Long) {
        log.info("게시글 좋아요 수 업데이트: boardId={}, likeCount={}", boardId, likeCount)
        try {
            val board = getBoardById(boardId)
            val updatedBoard = board.copy(likeCount = likeCount)
            boardDetailRepository.update(updatedBoard)
            
            // 기간별 좋아요 순위 업데이트
            boardIdListRepository.addToLikesByDayList(boardId, createdAt, likeCount)
            boardIdListRepository.addToLikesByWeekList(boardId, createdAt, likeCount)
            boardIdListRepository.addToLikesByMonthList(boardId, createdAt, likeCount)
            
            log.debug("게시글 좋아요 수 갱신 완료: boardId={}, likeCount={}", boardId, likeCount)
        } catch (e: Exception) {
            log.error("좋아요 수 업데이트 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.LIKE_COUNT_UPDATE_FAILED,
                message = "좋아요 수 업데이트 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 사용자별 좋아요 상태 업데이트
     */
    fun updateUserLikeStatus(boardId: Long, userId: String, hasLiked: Boolean) {
        val key = "board:like:user:$boardId:$userId"
        if (hasLiked) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofDays(1))
        } else {
            redisTemplate.delete(key)
        }
    }

    /**
     * 사용자의 게시글 좋아요 여부 확인
     */
    fun hasUserLikedBoard(boardId: Long, userId: String): Boolean {
        val key = "board:like:user:$boardId:$userId"
        return redisTemplate.opsForValue().get(key) != null
    }

    /**
     * 게시글 좋아요 상태 업데이트
     */
    fun updateBoardLike(boardId: Long, userId: String): Int {
        try {
            val hasLiked = likeClient.hasUserLiked(boardId, userId)
            
            val likeCount = if (hasLiked) {
                likeClient.removeLike(boardId, userId)
                likeClient.getLikeCount(boardId)
            } else {
                likeClient.addLike(boardId, userId)
                likeClient.getLikeCount(boardId)
            }
            
            val board = getBoardById(boardId)
            val updatedBoard = board.copy(likeCount = likeCount)
            boardDetailRepository.update(updatedBoard)
            boardIdListRepository.addToPopularList(boardId, likeCount.toDouble())
            
            return likeCount
        } catch (e: Exception) {
            log.error("좋아요 상태 업데이트 실패: boardId={}, userId={}", boardId, userId, e)
            throw AppException.fromErrorCode(
                ErrorCode.LIKE_COUNT_UPDATE_FAILED,
                message = "좋아요 상태 업데이트 실패",
                detailInfo = "boardId: $boardId, userId: $userId"
            )
        }
    }

    /**
     * 게시글 ID로 댓글 목록 조회 (페이징)
     */
    fun getCommentsByBoardId(boardId: Long, pageable: Pageable): Page<CommentReadModel> {
        val offset = pageable.offset
        val limit = pageable.pageSize.toLong()
        
        // Redis에 저장된 댓글 수 확인
        val commentCount = commentIdListRepository.getCommentCount(boardId)
        
        // Redis에 저장된 댓글이 없거나 offset이 50개를 초과하는 경우 원본에서 직접 조회
        if (commentCount == 0L || offset >= 50) {
            return fetchCommentsFromOriginal(boardId, pageable)
        }
        
        // Redis에서 댓글 ID 목록 조회
        val commentIds = commentIdListRepository.readAllByBoard(boardId, offset, limit)
        
        if (commentIds.isEmpty()) {
            return fetchCommentsFromOriginal(boardId, pageable)
        }
        
        val commentModels = commentDetailRepository.readAll(commentIds)
        
        val result = commentIds.mapNotNull { commentId ->
            commentModels[commentId] ?: fetchAndSaveCommentFromOriginal(commentId)
        }.filter { !it.isDeleted }
        
        return PageImpl(result, pageable, result.size.toLong())
    }

    /**
     * 최근 댓글 조회 (페이징)
     */
    fun getRecentComments(pageable: Pageable): Page<CommentReadModel> {
        val offset = pageable.offset
        val limit = pageable.pageSize.toLong()
        
        // 모든 게시글의 최근 댓글을 가져오기 위해 게시글 ID 목록 조회
        val boardIds = boardIdListRepository.readAllByTime(0, 100) // 최근 100개 게시글만 조회
        
        val allComments = boardIds.flatMap { boardId ->
            commentIdListRepository.readAllByBoard(boardId, 0, 10) // 각 게시글당 최근 10개 댓글
        }.distinct()
        
        val commentModels = commentDetailRepository.readAll(allComments)
        
        val result = allComments.mapNotNull { commentId ->
            commentModels[commentId] ?: fetchAndSaveCommentFromOriginal(commentId)
        }.filter { !it.isDeleted }
            .sortedByDescending { it.createdAt }
            .drop(offset.toInt())
            .take(limit.toInt())
        
        return PageImpl(result, pageable, result.size.toLong())
    }

    /**
     * 인기 게시글 조회 (페이징)
     */
    fun getTopBoards(pageable: Pageable): Page<BoardReadModel> {
        val offset = pageable.offset
        val limit = pageable.pageSize.toLong()
        
        // 좋아요 순으로 정렬된 게시글 ID 목록 조회
        val boardIds = boardIdListRepository.readAllByPopularity(offset + limit)
        
        if (boardIds.isEmpty()) {
            return fetchTopBoardsFromOriginal(pageable)
        }
        
        val boardModels = boardDetailRepository.readAll(boardIds)
        
        val result = boardIds.mapNotNull { boardId ->
            boardModels[boardId] ?: fetchAndSaveBoardFromOriginal(boardId)
        }.filter { !it.isDeleted }
        
        return PageImpl(result, pageable, result.size.toLong())
    }

    /**
     * 작성자 활동 조회
     */
    fun getAuthorActivities(author: String, page: Int?): AuthorActivitiesResponseDto? {
        val pageable = PageRequest.of(page ?: 0, CommunityReadConstants.DEFAULT_PAGE_SIZE)
        val boards = getBoardsByAuthor(author, pageable)
        
        return AuthorActivitiesResponseDto(
            boards = boards.content.map { ResponseMapper.toResponse(it) },
            totalCount = boards.totalElements,
            hasNext = boards.hasNext()
        )
    }

    // Private helper methods for fetching from original source
    private fun fetchBoardsFromOriginalSource(pageable: Pageable): Page<BoardReadModel> {
        val boards = boardClient.getBoards(pageable)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    private fun fetchBoardsByCategoryFromOriginal(categoryId: Long, pageable: Pageable): Page<BoardReadModel> {
        val boards = boardClient.getBoardsByCategory(categoryId, pageable)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    private fun fetchBoardsByAuthorFromOriginal(author: String, pageable: Pageable): Page<BoardReadModel> {
        val boards = boardClient.getBoardsByAuthor(author, pageable)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    private fun fetchBoardsInfiniteScrollFromOriginal(lastBoardId: Long?, size: Int): List<BoardReadModel> {
        val boards = boardClient.getBoardsInfiniteScroll(lastBoardId, size)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    private fun fetchBoardsByCategoryInfiniteScrollFromOriginal(categoryId: Long, lastBoardId: Long?, size: Int): List<BoardReadModel> {
        val boards = boardClient.getBoardsByCategoryInfiniteScroll(categoryId, lastBoardId, size)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    private fun fetchBoardsByAuthorInfiniteScrollFromOriginal(author: String, lastBoardId: Long?, size: Int): List<BoardReadModel> {
        val boards = boardClient.getBoardsByAuthorInfiniteScroll(author, lastBoardId, size)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    private fun fetchTopBoardsFromOriginal(limit: Int, type: String): List<BoardReadModel> {
        val boards = boardClient.getTopBoards(limit, type)
        boards.forEach { board ->
            fetchAndSaveBoardFromOriginal(board.id)
        }
        return boards
    }

    private fun fetchCommentsFromOriginal(boardId: Long, pageable: Pageable): Page<CommentReadModel> {
        val comments = commentClient.getCommentsByBoardId(boardId)
        comments.forEach { comment ->
            fetchAndSaveCommentFromOriginal(comment.id)
        }
        return comments
    }

    private fun fetchCommentsByAuthorFromOriginal(author: String): List<CommentReadModel> {
        val comments = commentClient.getCommentsByAuthor(author)
        comments.forEach { comment ->
            fetchAndSaveCommentFromOriginal(comment.id)
        }
        return comments
    }

    private fun fetchRecentCommentsFromOriginal(boardId: Long, limit: Int): List<CommentReadModel> {
        val comments = commentClient.getRecentComments(boardId, limit)
        comments.forEach { comment ->
            fetchAndSaveCommentFromOriginal(comment.id)
        }
        return comments
    }

    private fun fetchAndSaveCommentFromOriginal(commentId: Long): CommentReadModel? {
        val comment = commentClient.getCommentById(commentId) ?: return null
        
        val commentModel = CommentReadModel(
            id = comment.id,
            boardId = comment.boardId,
            content = comment.content,
            author = comment.author,
            path = comment.path,
            articleId = comment.articleId,
            userId = comment.userId,
            createdAt = comment.createdAt,
            updatedAt = comment.updatedAt,
            isDeleted = false
        )
        
        // 댓글 상세 정보 저장
        commentDetailRepository.create(commentModel)
        
        // 게시글별 댓글 ID 목록에 추가 (50개 제한 로직은 저장소에서 처리)
        commentIdListRepository.add(comment.boardId, comment.id, comment.createdAt.toEpochSecond() * 1000)
        
        return commentModel
    }

    private fun fetchTotalBoardCount(): Long {
        val count = boardClient.getTotalBoardCount()
        boardCountRepository.createOrUpdate(0, count)
        return count
    }

    private fun fetchCategoryBoardCount(categoryId: Long): Long {
        val count = boardClient.getCategoryBoardCount(categoryId)
        boardCountRepository.createOrUpdate(categoryId, count)
        return count
    }

    /**
     * 리스트를 Page 객체로 변환
     */
    fun <T> createPageFromList(list: List<T>, pageable: Pageable): Page<T> {
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, list.size)
        val pageContent = list.subList(start, end)
        return PageImpl(pageContent, pageable, list.size.toLong())
    }

    /**
     * 게시글 상세 정보 저장 또는 업데이트
     */
    @Transactional
    fun saveBoardModel(boardModel: BoardReadModel): BoardReadModel {
        log.info("게시글 데이터 저장: boardId={}", boardModel.id)
        
        return try {
            // 기존 게시글 데이터 조회
            val existingBoard = boardDetailRepository.read(boardModel.id)
            
            if (existingBoard == null) {
                createNewBoard(boardModel)
            } else {
                updateExistingBoard(existingBoard, boardModel)
            }
        } catch (e: Exception) {
            log.error("게시글 저장 실패: boardId={}", boardModel.id, e)
            throw AppException.fromErrorCode(
                ErrorCode.TRANSACTION_ERROR,
                message = "게시글 저장 실패",
                detailInfo = "boardId: ${boardModel.id}"
            )
        }
    }

    private fun createNewBoard(boardModel: BoardReadModel): BoardReadModel {
        val createdAtMillis = boardModel.createdAt.toEpochSecond() * 1000
        
        redisTemplate.execute { connection ->
            connection.multi()
            
            try {
                // 상세 정보 저장
                boardDetailRepository.create(boardModel, Duration.ofDays(7))
                
                // ID 리스트에 추가
                boardIdListRepository.add(boardModel.id, createdAtMillis.toDouble())
                
                boardModel.categoryId?.let { categoryId ->
                    boardIdListRepository.addToCategoryList(categoryId, boardModel.id, createdAtMillis.toDouble())
                }
                
                // 좋아요 및 조회수 기준 리스트 추가
                boardIdListRepository.addToLikesByDayList(boardModel.id, createdAtMillis, boardModel.likeCount)
                boardIdListRepository.addToLikesByWeekList(boardModel.id, createdAtMillis, boardModel.likeCount)
                boardIdListRepository.addToLikesByMonthList(boardModel.id, createdAtMillis, boardModel.likeCount)
                
                boardIdListRepository.addToViewsByDayList(boardModel.id, createdAtMillis, boardModel.viewCount)
                boardIdListRepository.addToViewsByWeekList(boardModel.id, createdAtMillis, boardModel.viewCount)
                boardIdListRepository.addToViewsByMonthList(boardModel.id, createdAtMillis, boardModel.viewCount)
                
                connection.exec()
            } catch (e: Exception) {
                connection.discard()
                throw CommunityReadException.TransactionFailed("새 게시글 생성", e)
            }
        }
        
        return boardModel
    }

    private fun updateExistingBoard(existingBoard: BoardReadModel, newBoard: BoardReadModel): BoardReadModel {
        redisTemplate.execute { connection ->
            connection.multi()
            
            try {
                // 상세 정보 업데이트
                boardDetailRepository.update(newBoard)
                
                // 카테고리 변경 처리
                if (existingBoard.categoryId != newBoard.categoryId) {
                    existingBoard.categoryId?.let { oldCategoryId ->
                        boardIdListRepository.deleteFromCategoryList(oldCategoryId, newBoard.id)
                    }
                    newBoard.categoryId?.let { newCategoryId ->
                        val createdAtMillis = newBoard.createdAt.toEpochSecond() * 1000
                        boardIdListRepository.addToCategoryList(newCategoryId, newBoard.id, createdAtMillis.toDouble())
                    }
                }
                
                // 좋아요 및 조회수 기준 리스트 업데이트
                val createdAtMillis = newBoard.createdAt.toEpochSecond() * 1000
                
                if (existingBoard.likeCount != newBoard.likeCount) {
                    boardIdListRepository.addToLikesByDayList(newBoard.id, createdAtMillis, newBoard.likeCount)
                    boardIdListRepository.addToLikesByWeekList(newBoard.id, createdAtMillis, newBoard.likeCount)
                    boardIdListRepository.addToLikesByMonthList(newBoard.id, createdAtMillis, newBoard.likeCount)
                }
                
                if (existingBoard.viewCount != newBoard.viewCount) {
                    boardIdListRepository.addToViewsByDayList(newBoard.id, createdAtMillis, newBoard.viewCount)
                    boardIdListRepository.addToViewsByWeekList(newBoard.id, createdAtMillis, newBoard.viewCount)
                    boardIdListRepository.addToViewsByMonthList(newBoard.id, createdAtMillis, newBoard.viewCount)
                }
                
                connection.exec()
            } catch (e: Exception) {
                connection.discard()
                throw CommunityReadException.TransactionFailed("게시글 업데이트", e)
            }
        }
        
        return newBoard
    }

    /**
     * 댓글 생성
     */
    @Transactional
    fun createComment(commentModel: CommentReadModel) {
        log.info("댓글 생성: commentId={}, boardId={}", commentModel.id, commentModel.boardId)
        
        try {
            redisTemplate.execute { connection ->
                connection.multi()
                
                try {
                    // 댓글 상세 정보 저장
                    commentDetailRepository.create(commentModel, Duration.ofDays(30))
                    
                    // 게시글의 댓글 ID 리스트에 추가 (50개 제한 로직은 저장소에서 처리)
                    val sortValue = commentModel.createdAt.toEpochSecond() * 1000
                    commentIdListRepository.add(commentModel.boardId, commentModel.id, sortValue)
                    
                    // 게시글의 댓글 수 업데이트
                    val board = getBoardById(commentModel.boardId)
                    val updatedBoard = board.copy(commentCount = board.commentCount + 1)
                    boardDetailRepository.update(updatedBoard)
                    
                    connection.exec()
                } catch (e: Exception) {
                    connection.discard()
                    throw AppException.fromErrorCode(
                        ErrorCode.TRANSACTION_ERROR,
                        message = "댓글 생성 실패",
                        detailInfo = "commentId: ${commentModel.id}, boardId: ${commentModel.boardId}"
                    )
                }
            }
            
            log.debug("댓글 생성 완료: commentId={}, boardId={}", commentModel.id, commentModel.boardId)
        } catch (e: Exception) {
            log.error("댓글 생성 실패: commentId={}, boardId={}", commentModel.id, commentModel.boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.TRANSACTION_ERROR,
                message = "댓글 생성 실패",
                detailInfo = "commentId: ${commentModel.id}, boardId: ${commentModel.boardId}"
            )
        }
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    fun deleteComment(commentId: Long) {
        log.info("댓글 삭제: commentId={}", commentId)
        
        try {
            val comment = commentDetailRepository.read(commentId) 
                ?: throw CommunityReadException.CommentNotFound(commentId)
            
            redisTemplate.execute { connection ->
                connection.multi()
                
                try {
                    // 댓글 상세 정보 삭제
                    commentDetailRepository.delete(commentId)
                    
                    // 게시글의 댓글 ID 리스트에서 제거
                    commentIdListRepository.delete(comment.boardId, commentId)
                    
                    // 게시글의 댓글 수 업데이트
                    val board = getBoardById(comment.boardId)
                    val updatedBoard = board.copy(commentCount = maxOf(0, board.commentCount - 1))
                    boardDetailRepository.update(updatedBoard)
                    
                    connection.exec()
                } catch (e: Exception) {
                    connection.discard()
                    throw AppException.fromErrorCode(
                        ErrorCode.TRANSACTION_ERROR,
                        message = "댓글 삭제 실패",
                        detailInfo = "commentId: $commentId"
                    )
                }
            }
            
            log.debug("댓글 삭제 완료: commentId={}, boardId={}", commentId, comment.boardId)
        } catch (e: Exception) {
            log.error("댓글 삭제 실패: commentId={}", commentId, e)
            throw AppException.fromErrorCode(
                ErrorCode.TRANSACTION_ERROR,
                message = "댓글 삭제 실패",
                detailInfo = "commentId: $commentId"
            )
        }
    }

    /**
     * 게시글 삭제
     */
    fun deleteBoard(boardId: Long) {
        try {
            // 상세 정보 조회
            val board = boardDetailRepository.read(boardId)
                ?: throw AppException.fromErrorCode(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    message = "게시글을 찾을 수 없습니다",
                    detailInfo = "boardId: $boardId"
                )
            
            redisTemplate.execute { connection ->
                connection.multi()
                
                try {
                    // 상세 정보 삭제
                    boardDetailRepository.delete(boardId)
                    
                    // 시간순 리스트에서 삭제
                    boardIdListRepository.delete(boardId)
                    
                    // 카테고리 리스트에서 삭제
                    board.categoryId?.let { categoryId ->
                        boardIdListRepository.deleteFromCategoryList(categoryId, boardId)
                    }
                    
                    // 좋아요/조회수 리스트에서 삭제
                    boardIdListRepository.deleteFromLikesList(boardId)
                    boardIdListRepository.deleteFromViewsList(boardId)
                    
                    connection.exec()
                } catch (e: Exception) {
                    connection.discard()
                    throw CommunityReadException.TransactionFailed("게시글 삭제", e)
                }
            }
        } catch (e: Exception) {
            log.error("게시글 삭제 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "게시글 삭제 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 게시글 소프트 삭제
     */
    fun softDeleteBoard(boardId: Long) {
        try {
            // 상세 정보 조회
            val board = boardDetailRepository.read(boardId)
                ?: throw AppException.fromErrorCode(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    message = "게시글을 찾을 수 없습니다",
                    detailInfo = "boardId: $boardId"
                )
            
            // 삭제 상태로 업데이트
            val updatedBoard = board.copy(isDeleted = true)
            boardDetailRepository.update(updatedBoard)
            
            // 리스트에서는 완전히 제거
            boardIdListRepository.delete(boardId)
            
            board.categoryId?.let { categoryId ->
                boardIdListRepository.deleteFromCategoryList(categoryId, boardId)
            }
            
            boardIdListRepository.deleteFromLikesList(boardId)
            boardIdListRepository.deleteFromViewsList(boardId)
        } catch (e: Exception) {
            log.error("게시글 소프트 삭제 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "게시글 소프트 삭제 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 게시글 댓글 수 증가
     */
    fun incrementBoardCommentCount(boardId: Long): Int {
        try {
            val board = getBoardById(boardId)
            val updatedCount = board.commentCount + 1
            val updatedBoard = board.copy(commentCount = updatedCount)
            boardDetailRepository.update(updatedBoard)
            return updatedCount
        } catch (e: Exception) {
            log.error("댓글 수 증가 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 수 증가 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 게시글 댓글 수 감소
     */
    fun decrementBoardCommentCount(boardId: Long): Int {
        try {
            val board = getBoardById(boardId)
            val updatedCount = maxOf(0, board.commentCount - 1)
            val updatedBoard = board.copy(commentCount = updatedCount)
            boardDetailRepository.update(updatedBoard)
            return updatedCount
        } catch (e: Exception) {
            log.error("댓글 수 감소 실패: boardId={}", boardId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 수 감소 실패",
                detailInfo = "boardId: $boardId"
            )
        }
    }

    /**
     * 댓글 ID로 댓글 조회
     */
    fun getCommentById(commentId: Long): CommentReadModel? {
        return try {
            commentDetailRepository.read(commentId)
        } catch (e: Exception) {
            log.error("댓글 조회 실패: commentId={}", commentId, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 조회 실패",
                detailInfo = "commentId: $commentId"
            )
        }
    }

    /**
     * 댓글 업데이트
     */
    fun updateComment(commentModel: CommentReadModel) {
        log.info("댓글 업데이트: commentId={}", commentModel.id)
        
        try {
            // 댓글 상세 정보 업데이트
            commentDetailRepository.update(commentModel)
            log.debug("댓글 업데이트 완료: commentId={}", commentModel.id)
        } catch (e: Exception) {
            log.error("댓글 업데이트 실패: commentId={}", commentModel.id, e)
            throw AppException.fromErrorCode(
                ErrorCode.REDIS_OPERATION_FAILED,
                message = "댓글 업데이트 실패",
                detailInfo = "commentId: ${commentModel.id}"
            )
        }
    }

    /**
     * 작성자별 댓글 조회 (원본 소스에서 직접 조회)
     */
    fun getCommentsByAuthorFromClient(author: String, pageable: Pageable): Page<CommentReadModel> {
        log.info("원본 소스에서 작성자별 댓글 조회: author={}", author)
        try {
            return commentClient.getCommentsByAuthor(author, pageable)
        } catch (e: Exception) {
            log.error("작성자별 댓글 조회 실패: author={}", author, e)
            throw AppException.fromErrorCode(
                ErrorCode.EXTERNAL_SERVICE_ERROR,
                message = "작성자별 댓글 조회 실패",
                detailInfo = "author: $author"
            )
        }
    }
}