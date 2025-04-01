package com.example.jobstat.community_read.repository.temp//package com.example.jobstat.community_read.repository
//
//import com.example.jobstat.community_read.model.BoardReadModel
//import com.example.jobstat.core.error.AppException
//import com.example.jobstat.core.error.ErrorCode
//import org.slf4j.LoggerFactory
//import org.springframework.data.redis.connection.RedisConnectionFactory
//import org.springframework.data.redis.core.RedisTemplate
//import org.springframework.data.redis.core.script.RedisScript
//import org.springframework.stereotype.Repository
//import java.time.Duration
//
//@Repository
//class RedisBoardReadRepository(
//    private val redisTemplate: RedisTemplate<String, Any>,
//    private val redisConnectionFactory: RedisConnectionFactory
//) : BoardReadRepository {
//
//    companion object {
//        private const val BOARD_KEY_PREFIX = "board:"
//        private const val AUTHOR_INDEX_PREFIX = "author-boards:"
//        private const val CATEGORY_INDEX_PREFIX = "category-boards:"
//        private const val BOARD_LIST_KEY = "all-boards"
//        private const val POPULAR_BOARDS_KEY = "popular-boards"
//        private const val RECENT_BOARDS_KEY = "recent-boards"
//
//        // 캐시 TTL 설정
//        private const val DEFAULT_TTL = 3600L  // 1시간
//        private const val POPULAR_TTL = 600L   // 10분
//        private const val LIST_TTL = 300L      // 5분
//    }
//
//    private val valueOps = redisTemplate.opsForValue()
//    private val setOps = redisTemplate.opsForSet()
//    private val zSetOps = redisTemplate.opsForZSet()
//    private val listOps = redisTemplate.opsForList()
//    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
//
//    override fun save(boardReadModel: BoardReadModel): BoardReadModel {
//        try {
//            val boardKey = getBoardKey(boardReadModel.id)
//            valueOps.set(boardKey, boardReadModel, Duration.ofSeconds(DEFAULT_TTL))
//
//            // 작성자 인덱스 업데이트
//            boardReadModel.author?.let { author ->
//                setOps.add(getAuthorIndexKey(author), boardReadModel.id.toString())
//                redisTemplate.expire(getAuthorIndexKey(author), Duration.ofSeconds(DEFAULT_TTL))
//            }
//
//            // 카테고리 인덱스 업데이트
//            boardReadModel.categoryId?.let { categoryId ->
//                setOps.add(getCategoryIndexKey(categoryId), boardReadModel.id.toString())
//                redisTemplate.expire(getCategoryIndexKey(categoryId), Duration.ofSeconds(DEFAULT_TTL))
//            }
//
//            // 인기 게시글 인덱스 업데이트 (좋아요 수 기준)
//            if (!boardReadModel.isDeleted) {
//                zSetOps.add(POPULAR_BOARDS_KEY, boardReadModel.id.toString(),
//                        boardReadModel.likeCount.toDouble())
//                redisTemplate.expire(POPULAR_BOARDS_KEY, Duration.ofSeconds(POPULAR_TTL))
//            }
//
//            // 전체 게시글 리스트 캐시 무효화
//            redisTemplate.delete(BOARD_LIST_KEY)
//
//            // 최근 게시글 리스트 업데이트
//            if (!boardReadModel.isDeleted) {
//                listOps.leftPush(RECENT_BOARDS_KEY, boardReadModel.id.toString())
//                listOps.trim(RECENT_BOARDS_KEY, 0, 99)  // 최근 100개만 유지
//                redisTemplate.expire(RECENT_BOARDS_KEY, Duration.ofSeconds(LIST_TTL))
//            }
//
//            return boardReadModel
//        } catch (e: Exception) {
//            log.error("게시글 저장 실패: boardId={}", boardReadModel.id, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "게시글 저장 실패",
//                detailInfo = "boardId: ${boardReadModel.id}"
//            )
//        }
//    }
//
//    override fun findById(boardId: Long): BoardReadModel? {
//        try {
//            val boardKey = getBoardKey(boardId)
//            return valueOps.get(boardKey) as? BoardReadModel
//        } catch (e: Exception) {
//            log.error("게시글 조회 실패: boardId={}", boardId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "게시글 조회 실패",
//                detailInfo = "boardId: $boardId"
//            )
//        }
//    }
//
//    override fun findAll(): List<BoardReadModel> {
//        try {
//            val keys = redisTemplate.keys("$BOARD_KEY_PREFIX*")
//            return keys.mapNotNull { key -> valueOps.get(key) as? BoardReadModel }
//        } catch (e: Exception) {
//            log.error("전체 게시글 조회 실패", e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "전체 게시글 조회 실패",
//                detailInfo = "전체 게시글 조회"
//            )
//        }
//    }
//
//    override fun findByAuthor(author: String): List<BoardReadModel> {
//        try {
//            val boardIds = setOps.members(getAuthorIndexKey(author))
//            return boardIds?.mapNotNull { boardId ->
//                findById(boardId.toString().toLong())
//            } ?: emptyList()
//        } catch (e: Exception) {
//            log.error("작성자별 게시글 조회 실패: author={}", author, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "작성자별 게시글 조회 실패",
//                detailInfo = "author: $author"
//            )
//        }
//    }
//
//    override fun findByCategoryId(categoryId: Long): List<BoardReadModel> {
//        try {
//            val boardIds = setOps.members(getCategoryIndexKey(categoryId))
//            return boardIds?.mapNotNull { boardId ->
//                findById(boardId.toString().toLong())
//            } ?: emptyList()
//        } catch (e: Exception) {
//            log.error("카테고리별 게시글 조회 실패: categoryId={}", categoryId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "카테고리별 게시글 조회 실패",
//                detailInfo = "categoryId: $categoryId"
//            )
//        }
//    }
//
//    override fun delete(boardId: Long) {
//        try {
//            val board = findById(boardId) ?: return
//
//            // 작성자 인덱스에서 제거
//            board.author?.let { author ->
//                setOps.remove(getAuthorIndexKey(author), boardId.toString())
//            }
//
//            // 카테고리 인덱스에서 제거
//            board.categoryId?.let { categoryId ->
//                setOps.remove(getCategoryIndexKey(categoryId), boardId.toString())
//            }
//
//            // 게시글 삭제
//            val boardKey = getBoardKey(boardId)
//            redisTemplate.delete(boardKey)
//        } catch (e: Exception) {
//            log.error("게시글 삭제 실패: boardId={}", boardId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "게시글 삭제 실패",
//                detailInfo = "boardId: $boardId"
//            )
//        }
//    }
//
//    override fun incrementViewCount(boardId: Long, amount: Int): Int {
//        try {
//            val board = findById(boardId) ?: return 0
//            val newViewCount = board.viewCount + amount
//            save(board.copy(viewCount = newViewCount))
//            return newViewCount
//        } catch (e: Exception) {
//            log.error("조회수 증가 실패: boardId={}", boardId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.VIEW_COUNT_UPDATE_FAILED,
//                message = "조회수 증가 실패",
//                detailInfo = "boardId: $boardId"
//            )
//        }
//    }
//
//    override fun incrementLikeCount(boardId: Long, amount: Int): Int {
//        try {
//            val board = findById(boardId) ?: return 0
//            val newLikeCount = board.likeCount + amount
//            save(board.copy(likeCount = newLikeCount))
//            return newLikeCount
//        } catch (e: Exception) {
//            log.error("좋아요 수 증가 실패: boardId={}", boardId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.LIKE_COUNT_UPDATE_FAILED,
//                message = "좋아요 수 증가 실패",
//                detailInfo = "boardId: $boardId"
//            )
//        }
//    }
//
//    override fun decrementLikeCount(boardId: Long, amount: Int): Int {
//        try {
//            val board = findById(boardId) ?: return 0
//            val newLikeCount = board.likeCount - amount
//            val finalLikeCount = if (newLikeCount < 0) 0 else newLikeCount
//            save(board.copy(likeCount = finalLikeCount))
//            return finalLikeCount
//        } catch (e: Exception) {
//            log.error("좋아요 수 감소 실패: boardId={}", boardId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.LIKE_COUNT_UPDATE_FAILED,
//                message = "좋아요 수 감소 실패",
//                detailInfo = "boardId: $boardId"
//            )
//        }
//    }
//
//    override fun incrementCommentCount(boardId: Long, amount: Int): Int {
//        try {
//            val board = findById(boardId) ?: return 0
//            val newCommentCount = board.commentCount + amount
//            val finalCommentCount = if (newCommentCount < 0) 0 else newCommentCount
//            save(board.copy(commentCount = finalCommentCount))
//            return finalCommentCount
//        } catch (e: Exception) {
//            log.error("댓글 수 증가 실패: boardId={}", boardId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.COMMENT_COUNT_UPDATE_FAILED,
//                message = "댓글 수 증가 실패",
//                detailInfo = "boardId: $boardId"
//            )
//        }
//    }
//
//    // 조회수 기준 인기 게시글 조회
//    override fun findTopByViews(limit: Int): List<BoardReadModel> {
//        try {
//            val result = findAll()
//                .filter { !it.isDeleted }
//                .sortedByDescending { it.viewCount }
//                .take(limit)
//
//            return result
//        } catch (e: Exception) {
//            log.error("조회수 기준 인기 게시글 조회 실패: limit={}", limit, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "조회수 기준 인기 게시글 조회 실패",
//                detailInfo = "limit: $limit"
//            )
//        }
//    }
//
//    // 좋아요 기준 인기 게시글 조회 (zSet 활용)
//    override fun findTopByLikes(limit: Int): List<BoardReadModel> {
//        try {
//            val topBoardIds = zSetOps.reverseRange(POPULAR_BOARDS_KEY, 0, limit - 1.toLong())
//
//            return topBoardIds?.mapNotNull { boardId ->
//                findById(boardId.toString().toLong())
//            }?.filter { !it.isDeleted } ?: findAll()
//                .filter { !it.isDeleted }
//                .sortedByDescending { it.likeCount }
//                .take(limit)
//        } catch (e: Exception) {
//            log.error("좋아요 기준 인기 게시글 조회 실패: limit={}", limit, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.REDIS_OPERATION_FAILED,
//                message = "좋아요 기준 인기 게시글 조회 실패",
//                detailInfo = "limit: $limit"
//            )
//        }
//    }
//
//    // 캐시 무효화
//    override fun invalidateCache(boardId: Long) {
//        try {
//            val boardKey = getBoardKey(boardId)
//            redisTemplate.delete(boardKey)
//            redisTemplate.delete(BOARD_LIST_KEY)
//            log.info("게시글 캐시 무효화: boardId={}", boardId)
//        } catch (e: Exception) {
//            log.error("게시글 캐시 무효화 실패: boardId={}", boardId, e)
//            throw AppException.fromErrorCode(
//                ErrorCode.CACHE_INVALIDATION_FAILED,
//                message = "게시글 캐시 무효화 실패",
//                detailInfo = "boardId: $boardId"
//            )
//        }
//    }
//
//    // 전체 캐시 무효화
//    override fun invalidateAllCaches() {
//        try {
//            val script = RedisScript.of<Long>("""
//                local keys = redis.call('keys', ARGV[1])
//                local count = 0
//                for i, key in ipairs(keys) do
//                    redis.call('del', key)
//                    count = count + 1
//                end
//                return count
//            """, Long::class.java)
//
//            redisTemplate.execute(script, listOf(), "$BOARD_KEY_PREFIX*")
//            redisTemplate.delete(BOARD_LIST_KEY)
//            redisTemplate.delete(POPULAR_BOARDS_KEY)
//            redisTemplate.delete(RECENT_BOARDS_KEY)
//            log.info("모든 게시글 캐시 무효화 완료")
//        } catch (e: Exception) {
//            log.error("전체 캐시 무효화 실패", e)
//            throw AppException.fromErrorCode(
//                ErrorCode.CACHE_INVALIDATION_FAILED,
//                message = "전체 캐시 무효화 실패",
//                detailInfo = "전체 캐시 무효화"
//            )
//        }
//    }
//
//    private fun getBoardKey(boardId: Long): String = "$BOARD_KEY_PREFIX$boardId"
//
//    private fun getAuthorIndexKey(author: String): String = "$AUTHOR_INDEX_PREFIX$author"
//
//    private fun getCategoryIndexKey(categoryId: Long): String = "$CATEGORY_INDEX_PREFIX$categoryId"
//}
//
