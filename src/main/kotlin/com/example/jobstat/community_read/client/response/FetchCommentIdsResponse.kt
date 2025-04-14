// file: src/main/kotlin/com/example/jobstat/community_read/client/response/FetchCommentIdsResponse.kt
package com.example.jobstat.community_read.client.response

/**
 * 댓글 ID 목록 응답 클래스 (Command 서버 API 응답 DTO)
 */
data class FetchCommentIdsResponse(
    val ids: List<String>,
    val hasNext: Boolean,
) {
    companion object {
        /**
         * Command 서버 응답 DTO의 String ID 목록을 내부에서 사용할 Long ID 목록으로 변환합니다.
         */
        fun from(response: FetchCommentIdsResponse): List<Long> {
            return response.ids.mapNotNull { it.toLongOrNull() } // 변환 실패 시 null 제거
        }

        /**
         * Command 서버 응답 DTO의 hasNext 값을 반환합니다. (Offset 기반 Fallback 시 사용)
         */
        fun hasNext(response: FetchCommentIdsResponse): Boolean = response.hasNext
    }
}
