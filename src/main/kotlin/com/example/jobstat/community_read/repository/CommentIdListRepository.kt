package com.example.jobstat.community_read.repository

/**
 * 댓글 ID 리스트 저장소 인터페이스
 */
interface CommentIdListRepository {

    /**
     * 게시글별 댓글 ID 리스트에 추가
     */
    fun add(boardId: Long, commentId: Long, sortValue: Double)

    /**
     * 게시글별 댓글 ID 리스트에서 삭제
     */
    fun delete(boardId: Long, commentId: Long)

    /**
     * 게시글별 댓글 ID 리스트 조회
     */
    fun readAllByBoard(boardId: Long, offset: Long, limit: Long): List<Long>

    /**
     * 무한 스크롤 방식의 게시글별 댓글 ID 리스트 조회
     */
    fun readAllByBoardInfiniteScroll(boardId: Long, lastCommentId: Long?, limit: Long): List<Long>
}