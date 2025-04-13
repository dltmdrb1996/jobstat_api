package com.example.jobstat.community_read.client.response

/**
 * 게시글 ID 목록 응답 클래스
 */
data class FetchBoardIdsResponse(
    val ids: List<String>,
    val hasNext: Boolean
) {
    companion object {
        fun from(response: FetchBoardIdsResponse): List<Long> {
            return response.ids?.map { it.toLongOrNull() ?: throw IllegalArgumentException("Invalid ID format") }
                ?: emptyList()
        }
    }
} 