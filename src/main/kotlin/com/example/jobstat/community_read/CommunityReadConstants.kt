package com.example.jobstat.community_read

/**
 * 커뮤니티 Read 상수
 */
object CommunityReadConstants {
    /**
     * 게시글 페이징 기본 설정
     */
    const val DEFAULT_PAGE_SIZE = 10
    const val MAX_PAGE_SIZE = 50
    
    /**
     * 댓글 페이징 기본 설정
     */
    const val DEFAULT_COMMENT_PAGE_SIZE = 10
    const val MAX_COMMENT_PAGE_SIZE = 30
    
    /**
     * 인기 게시글 관련 설정
     */
    const val DEFAULT_TOP_BOARDS_LIMIT = 10
    const val MAX_POPULAR_BOARDS_LIMIT = 30
    
    /**
     * Redis 키 관련 설정
     */
    object RedisKeys {
        const val BOARD_KEY_PREFIX = "board:"
        const val COMMENT_KEY_PREFIX = "comment:"
        const val AUTHOR_INDEX_PREFIX = "author-boards:"
        const val CATEGORY_INDEX_PREFIX = "category-boards:"
        const val BOARD_COMMENTS_PREFIX = "board-comments:"
        const val AUTHOR_COMMENTS_PREFIX = "author-comments:"
        
        /**
         * Redis 키 TTL 설정 (초 단위)
         */
        const val DEFAULT_TTL = 86400 // 24시간
        const val POPULAR_BOARDS_TTL = 3600 // 1시간
    }
    
    /**
     * 캐시 관련 설정
     */
    object CacheNames {
        const val BOARD_DETAIL = "boardDetail"
        const val BOARD_LIST = "boardList"
        const val BOARD_TOP = "boardTop"
        const val COMMENT_LIST = "commentList"
        const val BOARD_STATS = "boardStats"
    }
} 