package com.wildrew.jobstat.community_read.client.response

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(
    name = "GetCommentsByBoardIdResponse",
    description = "게시글별 댓글 목록 조회 응답 모델",
)
data class GetCommentsByBoardIdResponse(
    @Schema(
        description = "댓글 목록",
    )
    val items: Page<CommentListItem>,
    @Schema(
        description = "전체 댓글 수",
        example = "42",
    )
    val totalCount: Long,
    @Schema(
        description = "다음 페이지 존재 여부",
        example = "true",
    )
    val hasNext: Boolean,
)
