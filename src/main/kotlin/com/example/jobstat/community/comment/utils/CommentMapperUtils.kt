package com.example.jobstat.community.comment.utils

import com.example.jobstat.community.comment.entity.Comment
import com.example.jobstat.core.global.extension.toEpochMilli
import java.time.LocalDateTime

/**
 * 댓글 엔티티 매핑 유틸리티 클래스
 * 여러 UseCase에서 공통으로 사용하는 댓글 엔티티 변환 메서드를 모아둔 클래스입니다.
 */
object CommentMapperUtils {
    /**
     * 댓글 엔티티를 기본 포맷의 데이터 클래스로 변환
     *
     * @param comment 변환할 댓글 엔티티
     * @param T 변환될 데이터 클래스 타입
     * @param creator 데이터 클래스 생성 함수
     * @return 변환된 데이터 클래스 인스턴스
     */
    internal inline fun <T> mapToCommentDto(
        comment: Comment,
        creator: (
            id: String,
            boardId: String,
            userId: Long?,
            author: String,
            content: String,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
            eventTs: Long,
        ) -> T,
    ): T =
        with(comment) {
            creator(
                id.toString(),
                board.id.toString(),
                userId,
                author,
                content,
                createdAt,
                updatedAt,
                updatedAt.toEpochMilli(),
            )
        }

    /**
     * 댓글 엔티티를 문자열 시간 포맷의 데이터 클래스로 변환
     *
     * @param comment 변환할 댓글 엔티티
     * @param T 변환될 데이터 클래스 타입
     * @param creator 데이터 클래스 생성 함수
     * @return 변환된 데이터 클래스 인스턴스
     */
    internal inline fun <T> mapToCommentDtoWithStringDates(
        comment: Comment,
        creator: (
            id: String,
            boardId: String,
            userId: Long?,
            author: String,
            content: String,
            createdAt: String,
            updatedAt: String,
            eventTs: Long,
        ) -> T,
    ): T =
        with(comment) {
            creator(
                id.toString(),
                board.id.toString(),
                userId,
                author,
                content,
                createdAt.toString(),
                updatedAt.toString(),
                updatedAt.toEpochMilli(),
            )
        }
}
