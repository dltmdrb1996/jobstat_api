package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.BoardReadModel
import org.springframework.data.redis.connection.StringRedisConnection

interface BoardDetailRepository {
    /**
     * 게시글 상세 정보 조회
     */
    fun findBoardDetail(boardId: Long): BoardReadModel?

    /**
     * 여러 게시글 상세 정보 조회
     */
    fun findBoardDetails(boardIds: List<Long>): Map<Long, BoardReadModel>

    // 저장 관련 메소드

    /**
     * 게시글 상세 정보 저장
     */
    fun saveBoardDetail(
        board: BoardReadModel,
        eventTs: Long,
    )

    /**
     * 여러 게시글 상세 정보 저장
     */
    fun saveBoardDetails(
        boards: List<BoardReadModel>,
        eventTs: Long,
    )

    // 파이프라인 관련 메소드

    /**
     * 파이프라인에서 게시글 상세 정보 저장
     */
    fun saveBoardDetailInPipeline(
        conn: StringRedisConnection,
        board: BoardReadModel,
    )

//    /**
//     * 파이프라인에서 게시글 내용 업데이트
//     */
//    fun updateBoardContentInPipeline(
//        conn: StringRedisConnection,
//        boardId: Long,
//        title: String,
//        content: String,
//    )
}
