package com.example.jobstat.community_read.service

import com.example.jobstat.community_read.client.BoardClient
import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.repository.RedisBoardDetailRepository
import com.example.jobstat.community_read.repository.RedisBoardIdListRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.event.payload.board.*
import com.example.jobstat.community_read.repository.RedisBoardCountRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

/**
 * CommunityReadService는 Command 핸들러(게시글 생성/삭제/좋아요/수정/조회수 업데이트)
 * 의 이벤트를 받아 Redis의 Read Model(목록, 상세, 전체 카운트)를 동기화하는 역할을 합니다.
 *
 * - 각 이벤트는 eventTs를 포함하여, 이미 최신 상태가 반영된 경우(이전 eventTs보다 작으면) 무시합니다.
 * - 목록 업데이트는 RedisBoardIdListRepository를 통해 최신순, 카테고리별, 좋아요/조회수 순 랭킹 등을 갱신합니다.
 * - 상세 데이터는 RedisBoardDetailRepository를 사용하여 JSON 직렬화 방식으로 저장·조회하며,
 *   eventTs를 통해 최신 업데이트인지 확인합니다.
 * - 전체 게시글 수 등은 RedisBoardCountRepository를 통해 관리합니다.
 */
@Service
class CommunityReadService(
    private val redisTemplate: StringRedisTemplate,
    private val idListRepository: RedisBoardIdListRepository,
    private val detailRepository: RedisBoardDetailRepository,
    private val countRepository: RedisBoardCountRepository,
    private val boardClient: BoardClient
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    // -------------------------------
    // Command 처리 (핸들러 호출용)
    // -------------------------------

    /**
     * 게시글 생성 이벤트 처리
     */
    fun handleBoardCreated(payload: BoardCreatedEventPayload) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection

            // 1. 상세 정보 저장 (JSON 직렬화 방식)
            with(payload.toReadModel()) {
                val score = eventTs.toDouble()
                detailRepository.saveBoardDetailInPipeline(stringConn, this, payload.eventTs)
                // 2. 최신순 목록 갱신
                idListRepository.addBoardInPipeline(stringConn, id, eventTs, score)
                // 3. 카테고리별 목록 갱신 (카테고리 존재 시)
                idListRepository.addBoardToCategoryInPipeline(stringConn, id, categoryId, payload.eventTs, score)
                // 4. 전체 게시글 수 증가 (Count repository)
                countRepository.applyCountInPipeline(stringConn, 1, eventTs)
            }
            null
        }
    }

    /**
     * 게시글 수정 이벤트 처리 (제목, 내용 업데이트)
     */
    fun handleBoardUpdated(payload: BoardUpdatedEventPayload) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection

            // 리포지토리 메소드 호출로 책임 위임
            detailRepository.updateBoardContentInPipeline(
                stringConn,
                payload.boardId,
                payload.title,
                payload.content,
                payload.eventTs
            )
            null
        }
    }

    /**
     * 게시글 삭제 이벤트 처리
     */
    fun handleBoardDeleted(payload: BoardDeletedEventPayload) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection

            // 1. 최신순 목록 및 카테고리 목록에서 제거
            idListRepository.removeBoardInPipeline(stringConn, payload.boardId, payload.categoryId, payload.eventTs)
            // 2. 상세 정보 삭제 (직렬화된 JSON key 삭제)

            stringConn.del(RedisBoardDetailRepository.detailKey(payload.boardId))
            stringConn.del(RedisBoardDetailRepository.detailStateKey(payload.boardId))
            // 3. 전체 게시글 수 감소
            countRepository.applyCountInPipeline(stringConn, -1, payload.eventTs)
            null
        }
    }

    fun handleBoardLiked(payload: BoardLikedEventPayload) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val detailKey = RedisBoardDetailRepository.detailKey(payload.boardId)

            idListRepository.addLikesRankingInPipeline(
                stringConn,
                payload.boardId,
                payload.eventTs,
                payload.likeCount.toDouble()
            )

            // 상세 정보의 likeCount 업데이트 - 현재 count로 직접 설정
            stringConn.hSet(detailKey, "likeCount", payload.likeCount.toString())
            log.debug("Set like count for boardId={} to {}", payload.boardId, payload.likeCount)
            null
        }
    }

    /**
     * 조회수 업데이트 이벤트 처리
     * payload의 현재 viewCount 값을 기준으로 랭킹 업데이트
     */
    fun handleBoardViewed(payload: BoardViewedEventPayload) {
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            val detailKey = RedisBoardDetailRepository.detailKey(payload.boardId)

            // 직접적인 조회수 설정 (delta 계산 없이)
            idListRepository.addViewsRankingInPipeline(
                stringConn,
                payload.boardId,
                payload.eventTs,
                payload.viewCount.toDouble()
            )

            // 상세 정보의 viewCount 업데이트 - 현재 count로 직접 설정
            stringConn.hSet(detailKey, "viewCount", payload.viewCount.toString())

            log.debug("Set view count for boardId={} to {}", payload.boardId, payload.viewCount)
            null
        }
    }


    // -------------------------------
    // Query 처리 (읽기)
    // -------------------------------

    /**
     * 단건 조회 (Redis에서 읽고, 없으면 DB(Fallback)에서 가져온 후 Redis 갱신)
     */
    fun getBoardByIdWithFetch(boardId: Long): BoardReadModel? {
        val cached = detailRepository.findBoardDetail(boardId)
        if (cached != null) {
            return cached
        }
        val fromDb = boardClient.fetchBoardById(boardId)?.toBoardReadModel()
            ?: throw AppException.fromErrorCode(ErrorCode.INVALID_RESPONSE)
        detailRepository.saveBoardDetail(fromDb, System.currentTimeMillis())
        return fromDb
    }

    /**
     * bulk 조회
     */
    fun getBoardByIdsWithFetch(boardIds: List<Long>): List<BoardReadModel> {
        if (boardIds.isEmpty()) return emptyList()

        val redisMap = detailRepository.findBoardDetails(boardIds)
        val foundIds = redisMap.keys
        val result = mutableListOf<BoardReadModel>()
        result.addAll(redisMap.values)

        val missingIds = boardIds.filter { !foundIds.contains(it) }
        if (missingIds.isNotEmpty()) {
            val fromDb = boardClient.fetchBoardsByIds(missingIds)?.toBoardReadModels()
                ?: throw AppException.fromErrorCode(ErrorCode.INVALID_RESPONSE)
            detailRepository.saveBoardDetails(fromDb, System.currentTimeMillis())
            result.addAll(fromDb)
        }
        val map = result.associateBy { it.id }
        return boardIds.mapNotNull { map[it] }
    }

    // 목록 쿼리
    fun getLatestBoards(offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByTime(offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getLatestBoardsInfiniteScroll(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByTimeInfiniteScroll(lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getCategoryBoards(categoryId: Long, offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByCategory(categoryId, offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getCategoryBoardsInfiniteScroll(categoryId: Long, lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByCategoryInfiniteScroll(categoryId, lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByLikesDay(offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByLikesDay(offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByLikesDayInfiniteScroll(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByLikesDayInfiniteScroll(lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByLikesWeek(offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByLikesWeek(offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByLikesWeekInfiniteScroll(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByLikesWeekInfiniteScroll(lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByLikesMonth(offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByLikesMonth(offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByLikesMonthInfiniteScroll(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByLikesMonthInfiniteScroll(lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByViewsDay(offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByViewsDay(offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByViewsDayInfiniteScroll(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByViewsDayInfiniteScroll(lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByViewsWeek(offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByViewsWeek(offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByViewsWeekInfiniteScroll(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByViewsWeekInfiniteScroll(lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByViewsMonth(offset: Long, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByViewsMonth(offset, limit)
        return getBoardByIdsWithFetch(ids)
    }

    fun getBoardsByViewsMonthInfiniteScroll(lastBoardId: Long?, limit: Long): List<BoardReadModel> {
        val ids = idListRepository.readAllByViewsMonthInfiniteScroll(lastBoardId, limit)
        return getBoardByIdsWithFetch(ids)
    }
}