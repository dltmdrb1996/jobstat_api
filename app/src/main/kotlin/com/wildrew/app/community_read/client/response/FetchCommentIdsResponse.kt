package com.wildrew.app.community_read.client.response

data class FetchCommentIdsResponse(
    val ids: List<String>,
    val hasNext: Boolean,
) {
    companion object {
        fun from(response: FetchCommentIdsResponse): List<Long> {
            return response.ids.mapNotNull { it.toLongOrNull() } // 변환 실패 시 null 제거
        }

        fun hasNext(response: FetchCommentIdsResponse): Boolean = response.hasNext
    }
}
