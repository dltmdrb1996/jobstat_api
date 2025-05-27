package com.wildrew.jobstat.community.board.utils

object BoardConstants {
    // 페이징 관련
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_POPULAR_BOARDS_LIMIT = 100
    const val RANKING_LIMIT_SIZE = 100

    // 게시글 제한 관련
    const val MAX_TITLE_LENGTH = 100
    const val MIN_TITLE_LENGTH = 2
    const val MAX_CONTENT_LENGTH = 5000
    const val MIN_CONTENT_LENGTH = 10
    const val MAX_AUTHOR_LENGTH = 50

    // 비밀번호 관련
    const val MIN_PASSWORD_LENGTH = 4
    const val MAX_PASSWORD_LENGTH = 15
    const val ENCODED_PASSWORD_LENGTH = 60 // bcrypt 인코딩된 비밀번호 길이

    object ErrorMessages {
        const val TITLE_REQUIRED = "제목은 비워둘 수 없습니다"
        const val INVALID_TITLE = "제목은 ${MIN_TITLE_LENGTH}자에서 ${MAX_TITLE_LENGTH}자 사이여야 합니다"
        const val CONTENT_REQUIRED = "내용은 비워둘 수 없습니다"
        const val INVALID_CONTENT = "내용은 ${MIN_CONTENT_LENGTH}자에서 ${MAX_CONTENT_LENGTH}자 사이여야 합니다"
        const val AUTHOR_REQUIRED = "작성자는 비워둘 수 없습니다"
        const val INVALID_PASSWORD = "비밀번호는 ${MIN_PASSWORD_LENGTH}자에서 ${MAX_PASSWORD_LENGTH}자 사이여야 합니다"
        const val UNAUTHORIZED_UPDATE = "본인의 게시글만 수정할 수 있습니다"
        const val CATEGORY_REQUIRED = "카테고리는 설정은 필수입니다"
    }
}
