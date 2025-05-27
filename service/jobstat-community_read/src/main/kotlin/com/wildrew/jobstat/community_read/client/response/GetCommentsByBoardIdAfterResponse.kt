package com.wildrew.jobstat.community_read.client.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "GetCommentsByBoardIdAfterResponse",
    description = "게시글별 커서 기반 댓글 목록 조회 응답 모델",
)
data class GetCommentsByBoardIdAfterResponse(
    @Schema(
        description = "댓글 목록",
    )
    val items: List<CommentItem>,
    @Schema(
        description = "다음 페이지 존재 여부",
        example = "true",
    )
    val hasNext: Boolean,
)
