package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.client.BoardClient
import com.example.jobstat.community_read.client.response.BoardReadResponse
import com.example.jobstat.community_read.client.mapper.ResponseMapper
import com.example.jobstat.community_read.repository.BoardDetailRepository
import com.example.jobstat.community_read.repository.BoardIdListRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * 여러 게시글 ID로 게시글 조회 유스케이스
 * 캐시에 없는 게시글은 bulk fetch로 한 번에, 있는 게시글은 캐시에서 가져옴
 */
@Component
class GetBoardsByIdsUseCase(
    private val boardDetailRepository: BoardDetailRepository,
    private val boardIdListRepository: BoardIdListRepository,
    private val boardClient: BoardClient
) {
    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    @Transactional(readOnly = true)
    operator fun invoke(request: Request): Response {
        // 1. 캐시에서 조회
        val cachedBoards = boardDetailRepository.readAll(request.boardIds)

        // 2. 캐시에 없는 ID 목록 추출
        val missingIds = request.boardIds.filter { !cachedBoards.containsKey(it) }

        if (missingIds.isNotEmpty()) {
            log.info("캐시에 없는 게시글 발견: 총 {}개 중 {}개 누락", request.boardIds.size, missingIds.size)

            // 3. 원본 소스에서 bulk fetch
            val fetchedBoards = fetchBoardsFromOriginal(missingIds)

            // 4. 결과 합치기
            return Response(
                items = (cachedBoards.values + fetchedBoards)
                    .filter { !it.isDeleted }
                    .map { ResponseMapper.toResponse(it) }
            )
        }

        // 캐시에 모두 존재하는 경우
        log.info("캐시에서 모든 게시글 로드 완료: 총 {}개", cachedBoards.size)
        return Response(
            items = cachedBoards.values
                .filter { !it.isDeleted }
                .map { ResponseMapper.toResponse(it) }
        )
    }

    /**
     * 원본 소스에서 게시글 bulk fetch 후 캐시에 저장
     */
    private fun fetchBoardsFromOriginal(boardIds: List<Long>): List<BoardReadModel> {
        if (boardIds.isEmpty()) return emptyList()

        log.info("원본 소스에서 게시글 일괄 조회: boardIds={}", boardIds)

        val response = boardClient.getBoardsByIds(boardIds)
            ?: throw AppException.fromErrorCode(
                ErrorCode.EXTERNAL_SERVICE_ERROR,
                message = "원본 소스에서 게시글 조회 실패",
                detailInfo = "boardIds: $boardIds"
            )

        val fetchedBoards = response.boards.map { dto ->
            BoardReadModel(
                id = dto.id,
                title = dto.title,
                content = dto.content,
                author = dto.author,
                categoryId = dto.categoryId,
                viewCount = dto.viewCount,
                likeCount = dto.likeCount ?: 0,
                commentCount = dto.commentCount ?: 0,
                createdAt = LocalDateTime.parse(dto.createdAt),
            )
        }

        // 캐시에 저장
        fetchedBoards.forEach { board ->
            // 상세 정보 캐시에 저장
            boardDetailRepository.create(board, Duration.ofDays(7))

            // ID 목록 캐시에 저장
            // asia/seoul 기준으로 생성일을 밀리초로 변환하여 저장
            val sortValue = board.createdAt.toEpochSecond()* 1000
            boardIdListRepository.add(board.id, sortValue.toDouble())

            // 카테고리 ID 목록 캐시에 저장
            board.categoryId?.let { categoryId ->
                boardIdListRepository.addToCategoryList(categoryId, board.id, sortValue.toDouble())
            }

            // 좋아요/조회수 목록 캐시에 저장
            boardIdListRepository.addToLikesByDayList(board.id, sortValue, board.likeCount)
            boardIdListRepository.addToLikesByWeekList(board.id, sortValue, board.likeCount)
            boardIdListRepository.addToLikesByMonthList(board.id, sortValue, board.likeCount)

            boardIdListRepository.addToViewsByDayList(board.id, sortValue, board.viewCount)
            boardIdListRepository.addToViewsByWeekList(board.id, sortValue, board.viewCount)
            boardIdListRepository.addToViewsByMonthList(board.id, sortValue, board.viewCount)
        }

        log.info("원본 소스에서 {}개 게시글 조회 및 캐시 저장 완료", fetchedBoards.size)
        return fetchedBoards
    }

    data class Request(
        val boardIds: List<Long>
    )

    data class Response(
        val items: List<BoardReadResponse>
    )
} 