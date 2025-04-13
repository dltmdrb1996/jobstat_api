package com.example.jobstat.community_read.service

import com.example.jobstat.community_read.client.BoardClient
import com.example.jobstat.community_read.client.CommentClient
import com.example.jobstat.community_read.client.response.FetchCommentIdsResponse
import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.repository.BoardCountRepository
import com.example.jobstat.community_read.repository.BoardDetailRepository
import com.example.jobstat.community_read.repository.BoardIdListRepository
import com.example.jobstat.community_read.repository.CommentCountRepository
import com.example.jobstat.community_read.repository.CommentDetailRepository
import com.example.jobstat.community_read.repository.CommentIdListRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.state.BoardRankingMetric
import com.example.jobstat.core.state.BoardRankingPeriod
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
// import org.springframework.data.redis.core.StringRedisTemplate // redisTemplate은 제공된 서비스 로직에서 사용되지 않으므로, 다른 곳에서 필요하지 않다면 제거하는 것을 고려하세요
import org.springframework.stereotype.Service

@Service
class CommunityReadServiceImpl(
    private val boardIdListRepository: BoardIdListRepository,
    private val boardDetailRepository: BoardDetailRepository,
    private val boardClient: BoardClient,
    private val commentIdListRepository: CommentIdListRepository,
    private val commentDetailRepository: CommentDetailRepository,
    private val commentClient: CommentClient
) : CommunityReadService {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun getBoardByIdWithFetch(boardId: Long): BoardReadModel {
        return boardDetailRepository.findBoardDetail(boardId) ?: fetchAndCacheBoard(boardId)
    }

    private fun fetchAndCacheBoard(boardId: Long): BoardReadModel {
        log.debug("게시글 상세 캐시 미스: {}. Command 서버에서 조회합니다.", boardId)
        val fromDb = boardClient.fetchBoardById(boardId)
            ?: throw AppException.fromErrorCode(ErrorCode.RESOURCE_NOT_FOUND, "게시글을 찾을 수 없습니다: $boardId")
        boardDetailRepository.saveBoardDetail(fromDb, System.currentTimeMillis())
        return fromDb
    }

    override fun getBoardByIdWithComments(boardId: Long): BoardReadModel {
        val board = getBoardByIdWithFetch(boardId) // 내부적으로 캐시 우선 조회 사용

        // pageSize를 설정 가능하게 만들거나 상수로 사용하는 것을 고려하세요
        val commentIdsPage = commentIdListRepository.readCommentsByBoardId(boardId, PageRequest.of(0, 100))
        val commentIds = commentIdsPage.content // 첫 페이지에서 ID 가져오기

        if (commentIds.isNotEmpty()) {
            // 이 댓글들의 상세 정보 조회 (내부적으로 캐시 우선 조회 사용)
            board.comments = getCommentsByIds(commentIds)
        }
        // 참고: 이 로직은 댓글의 *첫* 페이지만 가져옵니다. 더 많은 댓글이 필요하다면,
        // API 디자인 조정이 필요하거나 호출자가 댓글을 별도로 페이징해야 할 수 있습니다.
        return board
    }

    override fun getBoardByIdsWithFetch(boardIds: List<Long>): List<BoardReadModel> {
        return fetchDetailsWithCache(
            ids = boardIds,
            findInCache = boardDetailRepository::findBoardDetails,
            fetchFromSource = boardClient::fetchBoardsByIds,
            saveToCache = boardDetailRepository::saveBoardDetails,
            idExtractor = { board -> board.id }, // <<< 게시글 ID 추출기 제공
            entityName = "Board"
        )
    }


    override fun getCommentById(commentId: Long): CommentReadModel {
        return commentDetailRepository.findCommentDetail(commentId) ?: fetchAndCacheComment(commentId)
    }

    private fun fetchAndCacheComment(commentId: Long): CommentReadModel {
        log.debug("댓글 상세 캐시 미스: {}. Command 서버에서 조회합니다.", commentId)
        val fromDb = commentClient.fetchCommentById(commentId)
            ?: throw AppException.fromErrorCode(ErrorCode.RESOURCE_NOT_FOUND, "댓글을 찾을 수 없습니다: $commentId")
        // 잠재적인 시간 기반 캐시 로직을 위해 eventTs 사용
        commentDetailRepository.saveCommentDetail(fromDb, fromDb.eventTs)
        return fromDb
    }


    override fun getCommentsByIds(commentIds: List<Long>): List<CommentReadModel> {
        return fetchDetailsWithCache(
            ids = commentIds,
            findInCache = commentDetailRepository::findCommentDetails,
            fetchFromSource = commentClient::fetchCommentsByIds,
            saveToCache = commentDetailRepository::saveCommentDetails,
            idExtractor = { comment -> comment.id }, // <<< 댓글 ID 추출기 제공
            entityName = "Comment"
        )
    }

    /**
     * ID 목록에 대한 상세 정보를 조회하며, 캐시 우선 조회 전략을 활용합니다.
     * @param T 상세 모델의 타입 (예: BoardReadModel, CommentReadModel).
     * @param ids 상세 정보를 조회할 ID 목록.
     * @param findInCache 캐시에서 기존 상세 정보를 찾는 함수. Input: List<Long>, Output: Map<Long, T>.
     * @param fetchFromSource 원본(DB/클라이언트)에서 누락된 상세 정보를 가져오는 함수. Input: List<Long>, Output: List<T>?.
     * @param saveToCache 새로 가져온 상세 정보를 캐시에 저장하는 함수. Input: List<T>, Long (타임스탬프).
     * @param idExtractor T 타입의 객체를 받아 Long ID를 반환하는 함수. // <<< Function to get the ID from T
     * @param entityName 로깅 목적의 엔티티 이름 (예: "Board", "Comment").
     * @return 입력 ID에 해당하는 상세 정보 목록 (순서 유지).
     */
    private fun <T : Any> fetchDetailsWithCache(
        ids: List<Long>,
        findInCache: (List<Long>) -> Map<Long, T>,
        fetchFromSource: (List<Long>) -> List<T>?,
        saveToCache: (List<T>, Long) -> Unit,
        idExtractor: (T) -> Long, // T로부터 ID를 가져오는 함수
        entityName: String
    ): List<T> {
        if (ids.isEmpty()) return emptyList()

        val cachedMap = findInCache(ids)
        val foundIds = cachedMap.keys
        val results = mutableListOf<T>()
        results.addAll(cachedMap.values)

        val missingIds = ids.filter { !foundIds.contains(it) }
        if (missingIds.isNotEmpty()) {
            log.debug("{} 상세 캐시 미스 ID: {}. 원본에서 조회합니다.", entityName, missingIds)
            val fetchedItems = fetchFromSource(missingIds) ?: emptyList()

            if (fetchedItems.isNotEmpty()) {
                log.debug("원본에서 {}개의 {} 상세 정보를 가져왔습니다. 캐시에 저장합니다.", fetchedItems.size, entityName.lowercase())
                saveToCache(fetchedItems, System.currentTimeMillis())
                results.addAll(fetchedItems)
            } else {
                log.warn("원본 조회 결과, 누락된 {} ID에 대한 결과가 없습니다: {}", entityName.lowercase(), missingIds)
            }
        }

        // 제공된 idExtractor를 사용하여 원본 순서 유지
        // dynamic 타입이나 리플렉션 필요 없음!
        val resultMap = results.associateBy { idExtractor(it) } // <<< idExtractor 사용
        return ids.mapNotNull { resultMap[it] }
    }

    // --- 오프셋 기반 페이징 메소드 ---

    override fun getLatestBoardsByOffset(pageable: Pageable): Page<BoardReadModel> {
        return getPageWithFallbackInternal(
            readFromCache = { boardIdListRepository.readAllByTimeByOffset(pageable) },
            fetchFromDb = { fetchLatestBoardIds(pageable) }, // 원본 pageable 기반의 폴백(대체) 조회
            fetchDetails = ::getBoardByIdsWithFetch,
            pageable = pageable,
            logMessagePrefix = "최신 게시글 오프셋"
        )
    }

    override fun getCategoryBoardsByOffset(categoryId: Long, pageable: Pageable): Page<BoardReadModel> {
        return getPageWithFallbackInternal(
            readFromCache = { boardIdListRepository.readAllByCategoryByOffset(categoryId, pageable) },
            fetchFromDb = { fetchCategoryBoardIds(categoryId, pageable) },
            fetchDetails = ::getBoardByIdsWithFetch,
            pageable = pageable,
            logMessagePrefix = "카테고리 $categoryId 게시글 오프셋"
        )
    }

    override fun getRankedBoardsByOffset(metric: BoardRankingMetric, period: BoardRankingPeriod, pageable: Pageable): Page<BoardReadModel> {
        val (cacheReader, dbFetcher) = getRankedBoardIdFetchers(metric, period, pageable)
        return getPageWithFallbackInternal(
            readFromCache = cacheReader,
            fetchFromDb = dbFetcher,
            fetchDetails = ::getBoardByIdsWithFetch,
            pageable = pageable,
            logMessagePrefix = "${period.name} ${metric.name} 랭킹 게시글 오프셋"
        )
    }

    override fun getCommentsByBoardIdByOffset(boardId: Long, pageable: Pageable): Page<CommentReadModel> {
        // 댓글의 특정 폴백 로직은 약간 다를 수 있음 (예: isLast 확인)
        // 여기서 제네릭 헬퍼를 사용하는 것은 크기 확인으로 충분하거나 약간의 동작 변경을 수용한다고 가정합니다.
        // 만약 정확한 'isLast' 확인이 중요하다면, 이 메소드는 헬퍼 대신 커스텀 로직이 필요할 수 있습니다.
        // 우선 헬퍼를 사용해 봅시다.

        // 수정: 'isLast'를 포함한 폴백 조건에 대한 커스텀 확인 필요
        val pageFromCache = commentIdListRepository.readCommentsByBoardId(boardId, pageable)
        val pageSize = pageable.pageSize
        val needsFallback = pageFromCache.isEmpty || (pageFromCache.content.size < pageSize && !pageFromCache.isLast)

        return getPageWithFallbackInternal(
            readFromCache = { pageFromCache }, // 이미 읽은 페이지 전달
            fetchFromDb = {
                // Command 서버로부터 폴백(대체) ID 목록 조회
                log.debug("댓글 ID 캐시 폴백 (BoardID: {}, Page: {}), command 서버에서 조회합니다.", boardId, pageable.pageNumber)
                val fallbackResponse = commentClient.fetchCommentIdsByBoardId(boardId, pageable.pageNumber, pageSize)
                fallbackResponse?.let { FetchCommentIdsResponse.from(it) } ?: emptyList()
            },
            fetchDetails = ::getCommentsByIds, // 댓글 상세 정보 조회기 사용
            pageable = pageable,
            logMessagePrefix = "게시글 $boardId 댓글 오프셋",
            forceFallback = needsFallback // 미리 계산된 폴백 조건 전달
        )
    }

    // --- 커서 기반 페이징 메소드 ---

    override fun getLatestBoardsByCursor(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        return getItemsWithFallbackInternal(
            readFromCache = { boardIdListRepository.readAllByTimeByCursor(lastBoardId, limit) },
            fetchFromDb = { cachedIds -> // 캐시에서 찾은 ID들을 받음
                val lastIdFromCache = cachedIds.lastOrNull()
                val effectiveLastId = lastIdFromCache ?: lastBoardId // DB 커서 결정
                log.debug("최신 게시글 커서 폴백 (Limit: {}), command 서버에서 ID {} 이후로 조회합니다.", limit, effectiveLastId)
                if (effectiveLastId != null) {
                    fetchLatestBoardIdsAfter(effectiveLastId, limit.toInt())
                } else {
                    fetchLatestBoardIds(PageRequest.of(0, limit.toInt())) // 커서가 없으면 첫 페이지 조회
                }
            },
            fetchDetails = ::getBoardByIdsWithFetch,
            limit = limit,
            logMessage = "최신 게시글 커서 (LastID: $lastBoardId, Limit: $limit)"
        )
    }

    override fun getCategoryBoardsByCursor(categoryId: Long, lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        return getItemsWithFallbackInternal(
            readFromCache = { boardIdListRepository.readAllByCategoryByCursor(categoryId, lastBoardId, limit) },
            fetchFromDb = { cachedIds ->
                val lastIdFromCache = cachedIds.lastOrNull()
                val effectiveLastId = lastIdFromCache ?: lastBoardId
                log.debug("카테고리 {} 게시글 커서 폴백 (Limit: {}), command 서버에서 ID {} 이후로 조회합니다.", categoryId, limit, effectiveLastId)
                if (effectiveLastId != null) {
                    fetchCategoryBoardIdsAfter(categoryId, effectiveLastId, limit.toInt())
                } else {
                    fetchCategoryBoardIds(categoryId, PageRequest.of(0, limit.toInt()))
                }
            },
            fetchDetails = ::getBoardByIdsWithFetch,
            limit = limit,
            logMessage = "카테고리 $categoryId 게시글 커서 (LastID: $lastBoardId, Limit: $limit)"
        )
    }

    override fun getRankedBoardsByCursor(metric: BoardRankingMetric, period: BoardRankingPeriod, lastBoardId: Long?, lastScore: Double?, limit: Long): List<BoardReadModel> {
        val (cacheReader, dbFallbackFetcher) = getRankedBoardIdFetchersCursor(metric, period, lastBoardId, lastScore, limit)

        return getItemsWithFallbackInternal(
            readFromCache = cacheReader,
            fetchFromDb = dbFallbackFetcher, // cachedIds를 올바르게 사용하는 람다 전달
            fetchDetails = ::getBoardByIdsWithFetch,
            limit = limit,
            logMessage = "${period.name} ${metric.name} 랭킹 게시글 커서 (LastID: $lastBoardId, LastScore: $lastScore, Limit: $limit)"
        )
    }

    override fun getCommentsByBoardIdByCursor(boardId: Long, lastCommentId: Long?, limit: Long): List<CommentReadModel> {
        return getItemsWithFallbackInternal(
            readFromCache = { commentIdListRepository.readCommentsByBoardIdByCursor(boardId, lastCommentId, limit) },
            fetchFromDb = { cachedIds ->
                val lastIdFromCache = cachedIds.lastOrNull()
                val effectiveLastId = lastIdFromCache ?: lastCommentId
                log.debug("게시글 {} 댓글 커서 폴백 (Limit: {}), command 서버에서 ID {} 이후로 조회합니다.", boardId, limit, effectiveLastId)
                // Command 서버로부터 폴백(대체) ID 목록 조회
                val fallbackResponse = commentClient.fetchCommentIdsByBoardIdAfter(boardId, effectiveLastId, limit.toInt())
                fallbackResponse?.let { FetchCommentIdsResponse.from(it) } ?: emptyList()
            },
            fetchDetails = ::getCommentsByIds, // 댓글 상세 정보 조회기 사용
            limit = limit,
            logMessage = "게시글 $boardId 댓글 커서 (LastID: $lastCommentId, Limit: $limit)"
        )
    }

    // --- 내부 ID 조회 로직 (클라이언트/DB 호출) ---
    // 이 메소드들은 대체로 동일하며, 로그 메시지만 개선될 수 있음

    private fun fetchLatestBoardIds(pageable: Pageable): List<Long> {
        log.debug("DB 조회: 최신 게시글 ID 목록 page={}, size={}", pageable.pageNumber, pageable.pageSize)
        return boardClient.fetchLatestBoardIds(pageable.pageNumber, pageable.pageSize) ?: emptyList()
    }

    private fun fetchLatestBoardIdsAfter(lastBoardId: Long?, limit: Int): List<Long> {
        log.debug("DB 조회: ID {} 이후 최신 게시글 ID 목록 limit={}", lastBoardId, limit)
        return boardClient.fetchLatestBoardIdsAfter(lastBoardId, limit) ?: emptyList()
    }

    private fun fetchCategoryBoardIds(categoryId: Long, pageable: Pageable): List<Long> {
        log.debug("DB 조회: 카테고리 {} 게시글 ID 목록 page={}, size={}", categoryId, pageable.pageNumber, pageable.pageSize)
        return boardClient.fetchCategoryBoardIds(categoryId, pageable.pageNumber, pageable.pageSize) ?: emptyList()
    }

    private fun fetchCategoryBoardIdsAfter(categoryId: Long, lastBoardId: Long?, limit: Int): List<Long> {
        log.debug("DB 조회: ID {} 이후 카테고리 {} 게시글 ID 목록 limit={}", lastBoardId, categoryId, limit)
        return boardClient.fetchCategoryBoardIdsAfter(categoryId, lastBoardId, limit) ?: emptyList()
    }

    private fun fetchBoardIdsByLikes(period: String, pageable: Pageable): List<Long> {
        log.debug("DB 조회: 좋아요 순 게시글 ID 목록 period={}, page={}, size={}", period, pageable.pageNumber, pageable.pageSize)
        return boardClient.fetchBoardIdsByLikes(period, pageable.pageNumber, pageable.pageSize) ?: emptyList()
    }

    private fun fetchBoardIdsByLikesAfter(period: String, lastBoardId: Long?, lastScore: Double?, limit: Int): List<Long> {
        log.debug("DB 조회: ID {}, score {} 이후 좋아요 순 게시글 ID 목록 period={}, limit={}", lastBoardId, lastScore, period, limit)
        return boardClient.fetchBoardIdsByLikesAfter(period, lastBoardId, lastScore, limit) ?: emptyList()
    }

    private fun fetchBoardIdsByViews(period: String, pageable: Pageable): List<Long> {
        log.debug("DB 조회: 조회수 순 게시글 ID 목록 period={}, page={}, size={}", period, pageable.pageNumber, pageable.pageSize)
        return boardClient.fetchBoardIdsByViews(period, pageable.pageNumber, pageable.pageSize) ?: emptyList()
    }

    private fun fetchBoardIdsByViewsAfter(period: String, lastBoardId: Long?, lastScore: Double?, limit: Int): List<Long> {
        log.debug("DB 조회: ID {}, score {} 이후 조회수 순 게시글 ID 목록 period={}, limit={}", lastBoardId, lastScore, period, limit)
        return boardClient.fetchBoardIdsByViewsAfter(period, lastBoardId, lastScore, limit) ?: emptyList()
    }

    // --- 랭킹 게시글 조회기 헬퍼 ---

    private fun getRankedBoardIdFetchers(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        pageable: Pageable
    ): Pair<() -> Page<Long>, () -> List<Long>> {
        val periodStr = period.toParamString()
        val cacheReader: () -> Page<Long> = when (metric) {
            BoardRankingMetric.LIKES -> when (period) {
                BoardRankingPeriod.DAY -> { -> boardIdListRepository.readAllByLikesDayByOffset(pageable) }
                BoardRankingPeriod.WEEK -> { -> boardIdListRepository.readAllByLikesWeekByOffset(pageable) }
                BoardRankingPeriod.MONTH -> { -> boardIdListRepository.readAllByLikesMonthByOffset(pageable) }
            }
            BoardRankingMetric.VIEWS -> when (period) {
                BoardRankingPeriod.DAY -> { -> boardIdListRepository.readAllByViewsDayByOffset(pageable) }
                BoardRankingPeriod.WEEK -> { -> boardIdListRepository.readAllByViewsWeekByOffset(pageable) }
                BoardRankingPeriod.MONTH -> { -> boardIdListRepository.readAllByViewsMonthByOffset(pageable) }
            }
        }
        val dbFetcher: () -> List<Long> = when (metric) {
            BoardRankingMetric.LIKES -> { -> fetchBoardIdsByLikes(periodStr, pageable) }
            BoardRankingMetric.VIEWS -> { -> fetchBoardIdsByViews(periodStr, pageable) }
        }
        return Pair(cacheReader, dbFetcher)
    }

    private fun getRankedBoardIdFetchersCursor(
        metric: BoardRankingMetric,
        period: BoardRankingPeriod,
        lastBoardId: Long?,
        lastScore: Double?,
        limit: Long
    ): Pair<() -> List<Long>, (List<Long>) -> List<Long>> {
        val periodStr = period.toParamString()
        val limitInt = limit.toInt()

        val cacheReader: () -> List<Long> = when (metric) {
            BoardRankingMetric.LIKES -> when (period) {
                BoardRankingPeriod.DAY -> { -> boardIdListRepository.readAllByLikesDayByCursor(lastBoardId, limit) }
                BoardRankingPeriod.WEEK -> { -> boardIdListRepository.readAllByLikesWeekByCursor(lastBoardId, limit) }
                BoardRankingPeriod.MONTH -> { -> boardIdListRepository.readAllByLikesMonthByCursor(lastBoardId, limit) }
            }
            BoardRankingMetric.VIEWS -> when (period) {
                BoardRankingPeriod.DAY -> { -> boardIdListRepository.readAllByViewsDayByCursor(lastBoardId, limit) }
                BoardRankingPeriod.WEEK -> { -> boardIdListRepository.readAllByViewsWeekByCursor(lastBoardId, limit) }
                BoardRankingPeriod.MONTH -> { -> boardIdListRepository.readAllByViewsMonthByCursor(lastBoardId, limit) }
            }
        }

        val dbFallbackFetcher: (List<Long>) -> List<Long> = { cachedIds ->
            val lastIdFromCache = cachedIds.lastOrNull()
            val effectiveLastId = lastIdFromCache ?: lastBoardId // 캐시 결과를 우선 사용
            log.debug("${period.name} ${metric.name} 랭킹 게시글 커서 폴백, command 서버에서 ID: {}, Score: {} 이후로 조회합니다.", effectiveLastId, lastScore)
            if (effectiveLastId != null) {
                when (metric) {
                    BoardRankingMetric.LIKES -> fetchBoardIdsByLikesAfter(periodStr, effectiveLastId, lastScore, limitInt)
                    BoardRankingMetric.VIEWS -> fetchBoardIdsByViewsAfter(periodStr, effectiveLastId, lastScore, limitInt)
                }
            } else {
                // 첫 페이지 조회
                when (metric) {
                    BoardRankingMetric.LIKES -> fetchBoardIdsByLikes(periodStr, PageRequest.of(0, limitInt))
                    BoardRankingMetric.VIEWS -> fetchBoardIdsByViews(periodStr, PageRequest.of(0, limitInt))
                }
            }
        }

        return Pair(cacheReader, dbFallbackFetcher)
    }

    // --- 제네릭 페이징 헬퍼 메소드 ---

    /**
     * 오프셋 기반 페이징을 위한 내부 헬퍼 (캐시 폴백 기능 포함).
     * @param T 최종 모델의 타입 (예: BoardReadModel).
     * @param readFromCache 캐시에서 ID 페이지를 읽는 함수.
     * @param fetchFromDb DB/클라이언트에서 폴백(대체) ID를 가져오는 함수.
     * @param fetchDetails ID 목록으로부터 전체 모델을 가져오는 함수.
     * @param pageable 원본 pageable 요청.
     * @param logMessagePrefix 로깅 메시지 접두사.
     * @param forceFallback 캐시 크기가 기준을 충족하더라도 폴백 로जिक을 강제하는 선택적 플래그 (댓글의 isLast 확인에 사용됨). // 댓글 특정 로직을 위해 추가됨
     * @return Page<T>
     */
    private fun <T : Any> getPageWithFallbackInternal(
        readFromCache: () -> Page<Long>,
        fetchFromDb: () -> List<Long>,
        fetchDetails: (List<Long>) -> List<T>,
        pageable: Pageable,
        logMessagePrefix: String,
        forceFallback: Boolean = false
    ): Page<T> {
        val pageFromCache = readFromCache()
        val needsFallback = forceFallback || pageFromCache.isEmpty || pageFromCache.content.size < pageable.pageSize

        if (needsFallback) {
            log.debug("{}: 캐시 미스 또는 불충분한 크기 (찾음: {}, 요청: {}). 폴백을 실행합니다.", logMessagePrefix, pageFromCache.content.size, pageable.pageSize)

            val additionalIds = fetchFromDb()
            // 캐시 결과를 먼저 병합하고, 그 다음 폴백 결과 병합, 고유성 보장 및 크기 제한
            val mergedIds = (pageFromCache.content + additionalIds).distinct().take(pageable.pageSize)

            if (mergedIds.isEmpty()) {
                log.debug("{}: 캐시나 폴백에서 ID를 찾을 수 없습니다.", logMessagePrefix)
                // 빈 페이지 반환, 캐시에 totalElements가 있었다면 원본 값 유지, 아니면 0
                return PageImpl(emptyList(), pageable, if (pageFromCache.totalElements > 0 && pageFromCache.isEmpty) pageFromCache.totalElements else 0)
            }

            log.debug("{}: 병합된 ID {}개에 대한 상세 정보를 조회합니다.", logMessagePrefix, mergedIds.size)
            val details = fetchDetails(mergedIds)
            // 폴백 후 캐시의 totalElements는 부정확할 수 있으나, 원본 로직 유지.
            // 더 정확한 개수는 별도의 DB 조회가 필요함.
            return PageImpl(details, pageable, pageFromCache.totalElements)
        } else {
            // 캐시 히트 또는 충분한 데이터
            if (pageFromCache.isEmpty) {
                log.debug("{}: 캐시 히트되었지만, ID를 찾을 수 없습니다.", logMessagePrefix)
                // 캐시의 전체 개수가 있다면 존중
                return PageImpl(emptyList(), pageable, pageFromCache.totalElements)
            }

            log.debug("{}: 캐시 히트 및 충분한 크기 (찾음: {}). 상세 정보를 조회합니다.", logMessagePrefix, pageFromCache.content.size)
            val details = fetchDetails(pageFromCache.content)
            return PageImpl(details, pageable, pageFromCache.totalElements)
        }
    }

    /**
     * 커서 기반 페이징을 위한 내부 헬퍼 (캐시 폴백 기능 포함).
     * @param T 최종 모델의 타입 (예: BoardReadModel).
     * @param readFromCache 커서/limit 기반으로 캐시에서 ID 목록을 읽는 함수.
     * @param fetchFromDb 초기에 캐시된 ID를 입력받아 폴백(대체) ID를 가져오는 함수. // 캐시 결과 받음
     * @param fetchDetails ID 목록으로부터 전체 모델을 가져오는 함수.
     * @param limit 요청된 최대 아이템 수.
     * @param logMessage 로깅 기본 메시지.
     * @return List<T>
     */
    private fun <T : Any> getItemsWithFallbackInternal(
        readFromCache: () -> List<Long>,
        fetchFromDb: (cachedIds: List<Long>) -> List<Long>,
        fetchDetails: (List<Long>) -> List<T>,
        limit: Long,
        logMessage: String
    ): List<T> {
        val idsFromCache = readFromCache()
        val limitInt = limit.toInt() // take()를 위해 Int 사용

        if (idsFromCache.size < limitInt) {
            log.debug("{}: 캐시 미스 또는 불충분한 크기 (찾음: {}, 요청: {}). 폴백을 실행합니다.", logMessage, idsFromCache.size, limitInt)

            // 캐시된 ID들을 폴백 함수에 전달
            val idsFromDb = fetchFromDb(idsFromCache)
            // 캐시 결과를 먼저 병합하고, 그 다음 폴백 결과 병합, 고유성 보장 및 크기 제한
            val mergedIds = (idsFromCache + idsFromDb).distinct().take(limitInt)

            if (mergedIds.isEmpty()) {
                log.debug("{}: 캐시나 폴백에서 ID를 찾을 수 없습니다.", logMessage)
                return emptyList()
            }

            log.debug("{}: 병합된 ID {}개에 대한 상세 정보를 조회합니다.", logMessage, mergedIds.size)
            return fetchDetails(mergedIds)
        } else {
            // 충분한 데이터로 캐시 히트
            if (idsFromCache.isEmpty()) {
                log.debug("{}: 캐시 히트되었지만, ID를 찾을 수 없습니다.", logMessage)
                return emptyList()
            }

            log.debug("{}: 캐시 히트 및 충분한 크기 (찾음: {}). 상세 정보를 조회합니다.", logMessage, idsFromCache.size)
            return fetchDetails(idsFromCache)
        }
    }
}