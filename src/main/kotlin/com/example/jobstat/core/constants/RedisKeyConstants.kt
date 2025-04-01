package com.example.jobstat.core.constants

/**
 * Redis 키 관리를 위한 상수 정의
 * 모든 Redis 키는 이 클래스를 통해 관리하여 일관성 확보
 */
object RedisKeyConstants {
    // 기본 네임스페이스
    private const val NS_COMMUNITY = "community"
    private const val NS_BOARD = "$NS_COMMUNITY:board"
    private const val NS_COMMENT = "$NS_COMMUNITY:comment"
    private const val NS_CATEGORY = "$NS_COMMUNITY:category"
    private const val NS_COUNTER = "$NS_COMMUNITY:counter"

    // 구분자
    private const val SEPARATOR = ":"

    // 보드 관련 키
    object Board {
        // 게시글 상세 정보
        fun detailKey(boardId: Long) = "$NS_BOARD$SEPARATOR$boardId$SEPARATOR:detail"

        // 게시글 목록 (시간순)
        const val ALL_BOARDS_LIST = "$NS_BOARD:list:all"

        // 카테고리별 게시글 목록
        fun categoryBoardsKey(categoryId: Long) = "$NS_CATEGORY$SEPARATOR$categoryId$SEPARATOR:boards"

        // 작성자별 게시글 목록
        fun authorBoardsKey(author: String) = "$NS_BOARD:author$SEPARATOR$author"

        // 인기 게시글 (조회수 기준)
        object Views {
            const val DAY = "$NS_BOARD:popular:views:day"
            const val WEEK = "$NS_BOARD:popular:views:week"
            const val MONTH = "$NS_BOARD:popular:views:month"
        }

        // 인기 게시글 (좋아요 기준)
        object Likes {
            const val DAY = "$NS_BOARD:popular:likes:day"
            const val WEEK = "$NS_BOARD:popular:likes:week"
            const val MONTH = "$NS_BOARD:popular:likes:month"
        }
    }

    // 댓글 관련 키
    object Comment {
        // 댓글 상세 정보
        fun detailKey(commentId: Long) = "$NS_COMMENT$SEPARATOR$commentId$SEPARATOR:detail"

        // 게시글별 댓글 목록
        fun boardCommentsKey(boardId: Long) = "$NS_BOARD$SEPARATOR$boardId$SEPARATOR:comments"

        // 작성자별 댓글 목록
        fun authorCommentsKey(author: String) = "$NS_COMMENT:author$SEPARATOR$author"
    }

    // 카운터 관련 키
    object Counter {
        // 조회수 카운터 관련
        const val viewCountKeyPrefix = "$NS_COUNTER:view$SEPARATOR"
        fun viewCountKey(boardId: Long) = "$viewCountKeyPrefix$boardId"

        // 좋아요 수 카운터 관련
        const val likeCountKeyPrefix = "$NS_COUNTER:like$SEPARATOR"
        fun likeCountKey(boardId: Long) = "$likeCountKeyPrefix$boardId"

        // 좋아요 사용자 목록
        fun likeUsersKey(boardId: Long) = "$NS_COUNTER:like$SEPARATOR$boardId$SEPARATOR:users"

        // 일일 좋아요 사용자 기록
        fun dailyLikeUserKey(userId: String, boardId: Long) =
            "$NS_COUNTER:like:daily$SEPARATOR$userId$SEPARATOR$boardId"

        // 대기 업데이트 목록
        const val PENDING_UPDATES = "$NS_COUNTER:pending-updates"

        // 카테고리별 게시글 수
        fun categoryCountKey(categoryId: Long) = "$NS_CATEGORY$SEPARATOR$categoryId$SEPARATOR:count"
    }

    /**
     * ID를 Redis에 저장하기 위한 패딩 문자열로 변환
     * Sorted Set에서 사용할 때 정렬 순서 보장
     */
    fun toPaddedString(id: Long): String = "%019d".format(id)
}