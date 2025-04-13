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
    private val dataSerializer: DataSerializer
) : CommentDetailRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        // 키 정의는 기존 방식 유지 (JSON 문자열 저장 가정)
        fun detailKey(commentId: Long) = "comment:detail:json::$commentId"
        fun detailStateKey(commentId: Long) = "comment:detailstate::$commentId" // 이 키의 용도 확인 필요
    }

    override fun findCommentDetail(commentId: Long): CommentReadModel? {
        return redisTemplate.opsForValue().get(detailKey(commentId))
            ?.let { dataSerializer.deserialize(it, CommentReadModel::class) }
    }

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

    override fun saveCommentDetail(comment: CommentReadModel, eventTs: Long) {
        // 파이프라인 외부에서 단일 저장 시 사용 (eventTs 처리는 핸들러에서)
        val json = dataSerializer.serialize(comment)
            ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        redisTemplate.opsForValue().set(detailKey(comment.id), json)
        log.debug("댓글 상세 정보 저장/업데이트 (SET): commentId=${comment.id}")
    }

    override fun saveCommentDetails(comments: List<CommentReadModel>, eventTs: Long) {
        if (comments.isEmpty()) return
        redisTemplate.executePipelined { conn ->
            val stringConn = conn as StringRedisConnection
            comments.forEach { c ->
                saveCommentDetailInPipeline(stringConn, c)
            }
            null
        }
    }

    // 파이프라인 내에서 전체 JSON 문자열을 SET 명령어로 저장
    override fun saveCommentDetailInPipeline(conn: StringRedisConnection, comment: CommentReadModel) {
        val json = dataSerializer.serialize(comment)
            ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE)
        // SET 명령어를 사용하여 전체 JSON 문자열 저장 (덮어쓰기)
        conn.set(detailKey(comment.id), json)
        // 파이프라인 내에서는 로그를 최소화하거나 핸들러 레벨에서 로깅
    }

}