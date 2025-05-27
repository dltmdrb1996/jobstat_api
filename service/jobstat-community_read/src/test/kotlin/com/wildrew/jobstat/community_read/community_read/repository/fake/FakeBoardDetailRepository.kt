package com.wildrew.jobstat.community_read.community_read.repository.fake

import com.wildrew.jobstat.community_read.model.BoardReadModel
import com.wildrew.jobstat.community_read.repository.BoardDetailRepository
import com.wildrew.jobstat.community_read.repository.impl.RedisBoardDetailRepository
import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_serializer.DataSerializer
import org.springframework.data.redis.connection.StringRedisConnection
import java.util.concurrent.ConcurrentHashMap

class FakeBoardDetailRepository(
    private val dataSerializer: DataSerializer,
) : BoardDetailRepository {
    val store = ConcurrentHashMap<String, String>()

    private fun detailKey(boardId: Long) = RedisBoardDetailRepository.detailKey(boardId)

    override fun findBoardDetail(boardId: Long): BoardReadModel? =
        store[detailKey(boardId)]?.let {
            try {
                dataSerializer.deserialize(it, BoardReadModel::class)
            } catch (e: Exception) {
                null
            }
        }

    override fun findBoardDetails(boardIds: List<Long>): Map<Long, BoardReadModel> {
        if (boardIds.isEmpty()) return emptyMap()
        return boardIds.mapNotNull { id -> findBoardDetail(id)?.let { id to it } }.toMap()
    }

    override fun saveBoardDetail(
        board: BoardReadModel,
        eventTs: Long,
    ) {
        val json =
            try {
                dataSerializer.serialize(board) ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "게시글 ${board.id}에 대한 직렬화 결과가 null입니다")
            } catch (e: Exception) {
                throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "게시글 ${board.id} 직렬화 실패")
            }
        store[detailKey(board.id)] = json
    }

    override fun saveBoardDetails(
        boards: List<BoardReadModel>,
        eventTs: Long,
    ) {
        if (boards.isEmpty()) return
        val jsonMap =
            boards.associate { board ->
                val json =
                    try {
                        dataSerializer.serialize(board) ?: throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "게시글 ${board.id}에 대한 직렬화 결과가 null입니다")
                    } catch (e: Exception) {
                        throw AppException.fromErrorCode(ErrorCode.SERIALIZATION_FAILURE, "배치에서 게시글 ${board.id} 직렬화 실패")
                    }
                detailKey(board.id) to json
            }
        store.putAll(jsonMap)
    }

    override fun saveBoardDetailInPipeline(
        conn: StringRedisConnection,
        board: BoardReadModel,
    ) {
        saveBoardDetail(board, 0L)
    }

    fun getJson(boardId: Long): String? = store[detailKey(boardId)]

    fun clear() {
        store.clear()
    }
}
