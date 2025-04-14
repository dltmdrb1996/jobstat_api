// file: src/main/kotlin/com/example/jobstat/community_read/repository/RedisCommentDetailRepository.kt
package com.example.jobstat.community_read.repository.impl

import com.example.jobstat.community_read.model.CommentReadModel
import com.example.jobstat.community_read.repository.CommentDetailRepository
import com.example.jobstat.core.error.AppException
import com.example.jobstat.core.error.ErrorCode
import com.example.jobstat.core.global.utils.serializer.DataSerializer
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.StringRedisConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisCommentDetailRepository(
    private val redisTemplate: StringRedisTemplate,
    private val dataSerializer: DataSerializer,
) : CommentDetailRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        // 키 포맷 정의
        fun detailKey(commentId: Long) = "comment:detail:json::$commentId"

        fun detailStateKey(commentId: Long) = "comment:detailstate::$commentId" // 상태 정보 키
    }

    // --------------------------
    // 조회 관련 메소드
    // --------------------------

    /**
     * 댓글 상세 정보 조회
     */
    override fun findCommentDetail(commentId: Long): CommentReadModel? =
        redisTemplate
            .opsForValue()
            .get(detailKey(commentId))
            ?.let { dataSerializer.deserialize(it, CommentReadModel::class) }

    /**
     * 여러 댓글 상세 정보 조회
     */
    override fun findCommentDetails(commentIds: List<Long>): Map<Long, CommentReadModel> {
        if (commentIds.isEmpty()) return emptyMap()

        val keys = commentIds.map { detailKey(it) }
        val values = redisTemplate.opsForValue().multiGet(keys)
        val resultMap = mutableMapOf<Long, CommentReadModel>()
        commentIds.forEachIndexed { index, commentId ->
            val data = values?.get(index)
            if (data != null) {
                dataSerializer.deserialize(data, CommentReadModel::class)?.let {
                    resultMap[commentId] = it
                }
            }
        }
        return resultMap
    }

    // --------------------------
    // 저장 관련 메소드
    // --------------------------

    /**
     * 댓글 상세 정보 저장
     */
    override fun saveCommentDetail(
        comment: CommentReadModel,
        eventTs: Long,
    ) {
        // 단일 저장 시 사용 (이벤트 타임스탬프는 핸들러에서 처리)
        val json =
            dataSerializer.serialize(comment)
                ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        redisTemplate.opsForValue().set(detailKey(comment.id), json)
        log.debug("댓글 상세 정보 저장/업데이트 (SET): commentId=${comment.id}")
    }

    /**
     * 여러 댓글 상세 정보 저장
     */
    override fun saveCommentDetails(
        comments: List<CommentReadModel>,
        eventTs: Long,
    ) {
        if (comments.isEmpty()) return
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            comments.forEach { c ->
                saveCommentDetailInPipeline(stringConn, c)
            }
            null
        }
    }

    // --------------------------
    // 파이프라인 관련 메소드
    // --------------------------

    /**
     * 파이프라인에서 댓글 상세 정보 저장
     */
    override fun saveCommentDetailInPipeline(
        conn: StringRedisConnection,
        comment: CommentReadModel,
    ) {
        val json =
            dataSerializer.serialize(comment)
                ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        // 전체 JSON 문자열 저장 (덮어쓰기)
        conn.set(detailKey(comment.id), json)
        // 파이프라인 내에서는 로그를 최소화
    }
}
