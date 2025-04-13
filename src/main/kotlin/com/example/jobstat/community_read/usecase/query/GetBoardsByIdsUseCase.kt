package com.example.jobstat.community_read.usecase.query

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.community_read.model.BoardResponseDto
import com.example.jobstat.community_read.service.CommunityReadService
import com.example.jobstat.core.usecase.impl.ValidUseCase
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Validator
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 게시글 ID 목록으로 게시글 조회 유스케이스
 */
@Service
class GetBoardsByIdsUseCase(
    private val communityReadService: CommunityReadService,
    validator: Validator
) : ValidUseCase<GetBoardsByIdsUseCase.Request, GetBoardsByIdsUseCase.Response>(validator) {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun execute(request: Request): Response {
        log.info("게시글 ID 목록으로 조회 요청: boardIds=${request.boardIds}")
        val boards = communityReadService.getBoardByIdsWithFetch(request.boardIds)
        log.info("게시글 조회 완료: 총 ${boards.size}개")
        return Response(
            items = BoardResponseDto.from(boards),
            totalCount = boards.size.toLong()
        )
    }

    /**
     * 요청 데이터 클래스
     */
    @Schema(
        name = "GetBoardsByIdsRequest", 
        description = "게시글 ID 목록 조회 요청 모델"
    )
    data class Request(
        @field:Schema(
            description = "조회할 게시글 ID 목록", 
            example = "[1, 2, 3, 4, 5]",
            required = true
        )
        @field:NotEmpty(message = "게시글 ID 목록은 비어있을 수 없습니다")
        @field:Size(max = 100, message = "게시글 ID 목록은 최대 100개까지만 가능합니다")
        val boardIds: List<Long>
    )

    /**
     * 응답 데이터 클래스
     */
    @Schema(
        name = "GetBoardsByIdsResponse",
        description = "게시글 ID 목록 조회 응답 모델"
    )
    data class Response(
        @field:Schema(
            description = "게시글 목록"
        )
        val items: List<BoardResponseDto>,

        @field:Schema(
            description = "총 게시글 수", 
            example = "5",
            minimum = "0"
        )
        val totalCount: Long
    )

}