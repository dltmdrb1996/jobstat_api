package com.example.jobstat.community.comment

/**
 * 댓글 관련 상수
 */
object CommentConstants {
    /**
     * 기본 페이지 크기
     */
    const val DEFAULT_PAGE_SIZE = 20
    
    /**
     * 최근 댓글 조회 시 기본 개수
     */
    const val DEFAULT_RECENT_COMMENTS_SIZE = 5
    
    /**
     * 최대 댓글 내용 길이
     */
    const val MAX_CONTENT_LENGTH = 500

    // 댓글 제한 관련
    const val MIN_CONTENT_LENGTH = 1
    const val MAX_AUTHOR_LENGTH = 50

    // 비밀번호 관련
    const val MIN_PASSWORD_LENGTH = 4
    const val MAX_PASSWORD_LENGTH = 15
    const val ENCODED_PASSWORD_LENGTH = 60

    object ErrorMessages {
        const val CONTENT_REQUIRED = "댓글 내용은 필수입니다"
        const val INVALID_CONTENT = "댓글 내용은 ${MIN_CONTENT_LENGTH}자 이상 ${MAX_CONTENT_LENGTH}자 이하여야 합니다"
        const val AUTHOR_REQUIRED = "댓글 작성자는 필수입니다"
        const val PASSWORD_REQUIRED = "비로그인 상태에서는 비밀번호 설정이 필수입니다"
        const val INVALID_PASSWORD = "비밀번호는 ${MIN_PASSWORD_LENGTH}자에서 ${MAX_PASSWORD_LENGTH}자 사이여야 합니다"
        const val UNAUTHORIZED_UPDATE = "본인의 댓글만 수정할 수 있습니다"
        const val UNAUTHORIZED_DELETE = "본인의 댓글만 삭제할 수 있습니다"
        const val AUTHENTICATION_FAILURE = "인증에 실패했습니다"
    }
}
