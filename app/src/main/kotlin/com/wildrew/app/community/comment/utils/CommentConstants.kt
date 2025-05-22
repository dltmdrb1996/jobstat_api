package com.wildrew.app.community.comment.utils

/**
 * 댓글 관련 상수 정의
 */
object CommentConstants {
    // 댓글 컨텐츠 길이 제한
    const val MIN_CONTENT_LENGTH = 1
    const val MAX_CONTENT_LENGTH = 1000

    // 작성자 길이 제한
    const val MIN_AUTHOR_LENGTH = 2
    const val MAX_AUTHOR_LENGTH = 30

    // 비밀번호 길이 제한
    const val MIN_PASSWORD_LENGTH = 4
    const val MAX_PASSWORD_LENGTH = 15

    // 암호화된 비밀번호 저장 길이
    const val ENCODED_PASSWORD_LENGTH = 100

    // 페이지네이션 기본값
    const val DEFAULT_PAGE_SIZE = 20

    // 에러 메시지
    object ErrorMessages {
        const val CONTENT_REQUIRED = "댓글 내용은 필수입니다"
        const val PASSWORD_REQUIRED = "비밀번호는 필수입니다"
        const val INVALID_CONTENT = "댓글 내용은 1자 이상 1000자 이하여야 합니다"
        const val AUTHOR_REQUIRED = "작성자 이름은 필수입니다"
        const val UNAUTHORIZED_UPDATE = "본인의 댓글만 수정할 수 있습니다"
        const val UNAUTHORIZED_DELETE = "본인의 댓글만 삭제할 수 있습니다"
        const val INVALID_AUTHOR = "작성자 이름은 2자 이상 30자 이하여야 합니다"
        const val INVALID_PASSWORD = "비밀번호는 4자 이상 15자 이하여야 합니다"
        const val BOARD_REQUIRED = "댓글을 작성할 게시글이 필요합니다"
    }
}
