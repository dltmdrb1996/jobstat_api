package com.example.jobstat.community_read.client

import com.example.jobstat.community_read.client.response.*
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

    fun fetchBoardById(boardId: Long): FetchBoardByIdResponse? {
        return executeGet(
            uri = "/api/v1/boards/$boardId",
            responseType = FetchBoardByIdResponse::class.java,
            logContext = "BoardClient.getBoardById"
        )
    }

    fun fetchBoardsByIds(boardIds: List<Long>): FetchBoardsByIdsResponse? {
        val request = mapOf("boardIds" to boardIds)

        return executePost(
            uri = "/api/v1/boards/bulk",
            body = request,
            responseType = FetchBoardsByIdsResponse::class.java,
            logContext = "BoardClient.getBoardsByIds"
        )
    }


}