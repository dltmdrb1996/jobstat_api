package com.example.jobstat.community_read.client

import com.example.jobstat.community_read.client.response.BoardResponseDto
import com.example.jobstat.community_read.client.response.BoardListResponseDto
import com.example.jobstat.community_read.client.response.BoardDetailResponseDto
import com.example.jobstat.community_read.client.response.BoardStatsResponseDto
import com.example.jobstat.community_read.client.response.TopBoardsResponseDto
import com.example.jobstat.community_read.client.response.BulkBoardsResponseDto
import com.example.jobstat.core.base.BaseClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 게시판 원본 데이터를 조회하는 클라이언트
 * 외부 마이크로서비스와 HTTP로 통신
 */
@Component
class BoardClient : BaseClient() {
    
    @Value("\${endpoints.board-service.url:http://localhost:8080}")
    private lateinit var boardServiceUrl: String
    
    override fun getServiceUrl(): String = boardServiceUrl

    /**
     * 게시글 생성
     */
    fun createBoard(title: String, content: String, author: String, categoryId: Long? = null): BoardResponseDto? {
        val request = mapOf(
            "title" to title,
            "content" to content,
            "author" to author,
            "categoryId" to categoryId
        )
        
        return executePost(
            uri = "/api/v1/boards",
            body = request,
            responseType = BoardResponseDto::class.java,
            logContext = "BoardClient.createBoard"
        )
    }

    /**
     * 게시글 ID로 게시글 상세 조회
     */
    fun getBoardDetail(boardId: Long, commentPage: Int? = null): BoardDetailResponseDto? {
        val queryParams = if (commentPage != null) {
            mapOf("commentPage" to commentPage)
        } else {
            emptyMap()
        }
        
        val uri = buildUri("/api/v1/boards/$boardId", queryParams)
        
        return executeGet(
            uri = uri,
            responseType = BoardDetailResponseDto::class.java,
            logContext = "BoardClient.getBoardDetail"
        )
    }

    /**
     * 모든 게시글 목록 조회
     */
    fun getAllBoards(page: Int? = null, categoryId: Long? = null, author: String? = null, keyword: String? = null): BoardListResponseDto? {
        val queryParams = mutableMapOf<String, Any?>()
        if (page != null) queryParams["page"] = page
        if (categoryId != null) queryParams["categoryId"] = categoryId
        if (author != null) queryParams["author"] = author
        if (keyword != null) queryParams["keyword"] = keyword
        
        val uri = buildUri("/api/v1/boards", queryParams)
        
        return executeGet(
            uri = uri,
            responseType = BoardListResponseDto::class.java,
            logContext = "BoardClient.getAllBoards"
        )
    }

    /**
     * 작성자별 게시글 목록 조회
     */
    fun getBoardsByAuthor(author: String, page: Int? = null): BoardListResponseDto? {
        return getAllBoards(page = page, author = author)
    }

    /**
     * 카테고리별 게시글 목록 조회
     */
    fun getBoardsByCategory(categoryId: Long, page: Int? = null): BoardListResponseDto? {
        return getAllBoards(page = page, categoryId = categoryId)
    }

    /**
     * 키워드로 게시글 검색
     */
    fun searchBoards(keyword: String, page: Int? = null): BoardListResponseDto? {
        return getAllBoards(page = page, keyword = keyword)
    }

    /**
     * 인기 게시글 목록 조회
     */
    fun getTopBoards(limit: Int? = 10): TopBoardsResponseDto? {
        val queryParams = limit?.let { mapOf("limit" to it) } ?: emptyMap()
        
        val uri = buildUri("/api/v1/boards/top", queryParams)
        
        return executeGet(
            uri = uri,
            responseType = TopBoardsResponseDto::class.java,
            logContext = "BoardClient.getTopBoards"
        )
    }

    /**
     * 작성자별 게시글 수 조회 (현재 API에 구현되어 있지 않음 - 추가 필요)
     */
    fun getBoardCountByAuthor(author: String): Int {
        // 실제 API가 없으므로 작성자별 게시글 목록을 가져와서 totalCount를 반환하도록 수정
        val boardList = getBoardsByAuthor(author)
        return boardList?.totalCount?.toInt() ?: 0
    }

    /**
     * 게시글 통계 조회
     */
    fun getBoardStats(author: String, boardId: Long): BoardStatsResponseDto? {
        val queryParams = mapOf("boardId" to boardId)
        
        val uri = buildUri("/api/v1/authors/$author/boards/stats", queryParams)
        
        return executeGet(
            uri = uri,
            responseType = BoardStatsResponseDto::class.java,
            logContext = "BoardClient.getBoardStats"
        )
    }
    
    /**
     * 여러 게시글 ID로 게시글 목록 한번에 조회
     */
    fun getBoardsByIds(boardIds: List<Long>): BulkBoardsResponseDto? {
        val request = mapOf("boardIds" to boardIds)
        
        return executePost(
            uri = "/api/v1/boards/bulk",
            body = request,
            responseType = BulkBoardsResponseDto::class.java,
            logContext = "BoardClient.getBoardsByIds"
        )
    }
}