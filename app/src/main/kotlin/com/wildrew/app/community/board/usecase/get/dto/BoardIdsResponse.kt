package com.wildrew.app.community.board.usecase.get.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "BoardIdsResponse", description = "게시글 ID 목록 조회 공통 응답 모델")
data class BoardIdsResponse(
    @field:Schema(description = "게시글 ID 목록", example = "[\"1\", \"2\", \"3\"]")
    val ids: List<String>,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)
