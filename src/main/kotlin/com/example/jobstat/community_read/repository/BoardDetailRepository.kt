package com.example.jobstat.community_read.repository

import com.example.jobstat.community_read.model.BoardReadModel
import java.time.Duration

interface BoardDetailRepository {
    fun create(boardModel: BoardReadModel, ttl: Duration = Duration.ofDays(1))
    fun update(boardModel: BoardReadModel)
    fun delete(boardId: Long)
    fun read(boardId: Long): BoardReadModel?
    fun readAll(boardIds: List<Long>): Map<Long, BoardReadModel>
}