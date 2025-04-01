package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.BoardReadModel
import com.example.jobstat.core.constants.RedisKeyConstants
import com.example.jobstat.core.global.utils.RedisOperationUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisBoardDetailRepository(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : BoardDetailRepository {

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun create(boardModel: BoardReadModel, ttl: Duration) {
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "게시글 상세 정보 저장",
            detailInfo = "boardId: ${boardModel.id}",
            errorCode = null
        ) {
            val key = RedisKeyConstants.Board.detailKey(boardModel.id)
            val existingJson = redisTemplate.opsForValue().get(key)
            if (existingJson != null) {
                val existing = objectMapper.readValue(existingJson, BoardReadModel::class.java)
                if (existing.updatedAt >= boardModel.updatedAt) {
                    log.info("Board detail for id {} is already up-to-date. Skipping creation.", boardModel.id)
                    return@executeWithRetry
                }
            }
            val json = objectMapper.writeValueAsString(boardModel)
            redisTemplate.opsForValue().set(key, json, ttl)
        }
    }

    override fun update(boardModel: BoardReadModel) {
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "게시글 상세 정보 업데이트",
            detailInfo = "boardId: ${boardModel.id}",
            errorCode = null
        ) {
            val key = RedisKeyConstants.Board.detailKey(boardModel.id)
            val existingJson = redisTemplate.opsForValue().get(key)
            if (existingJson != null) {
                val existing = objectMapper.readValue(existingJson, BoardReadModel::class.java)
                if (existing.updatedAt >= boardModel.updatedAt) {
                    log.info("Board detail for id {} is already up-to-date. Skipping update.", boardModel.id)
                    return@executeWithRetry
                }
            }
            val json = objectMapper.writeValueAsString(boardModel)
            redisTemplate.opsForValue().set(key, json)
        }
    }

    override fun delete(boardId: Long) {
        RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "게시글 상세 정보 삭제",
            detailInfo = "boardId: $boardId",
            errorCode = null
        ) {
            redisTemplate.delete(RedisKeyConstants.Board.detailKey(boardId))
        }
    }

    override fun read(boardId: Long): BoardReadModel? {
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "게시글 상세 정보 조회",
            detailInfo = "boardId: $boardId",
            errorCode = null
        ) {
            val key = RedisKeyConstants.Board.detailKey(boardId)
            val json = redisTemplate.opsForValue().get(key) ?: return@executeWithRetry null
            objectMapper.readValue(json, BoardReadModel::class.java)
        }
    }

    override fun readAll(boardIds: List<Long>): Map<Long, BoardReadModel> {
        return RedisOperationUtils.executeWithRetry(
            logger = log,
            operationName = "게시글 상세 정보 일괄 조회",
            detailInfo = "boardIds: $boardIds",
            errorCode = null
        ) {
            if (boardIds.isEmpty()) return@executeWithRetry emptyMap()
            val keys = boardIds.map { RedisKeyConstants.Board.detailKey(it) }
            val values = redisTemplate.opsForValue().multiGet(keys) ?: return@executeWithRetry emptyMap()
            boardIds.zip(values)
                .filter { (_, json) -> json != null }
                .associate { (id, json) ->
                    id to objectMapper.readValue(json!!, BoardReadModel::class.java)
                }
        }
    }
}
