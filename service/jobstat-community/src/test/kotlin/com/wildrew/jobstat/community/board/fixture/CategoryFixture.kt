package com.wildrew.jobstat.community.board.fixture

import com.wildrew.jobstat.community.board.entity.BoardCategory
import com.wildrew.jobstat.community.utils.TestFixture
import java.time.LocalDateTime

class CategoryFixture private constructor(
    private var name: String = "기본 카테고리",
    private var displayName: String = "기본 표시 이름",
    private var description: String = "기본 설명",
    private var createdAt: LocalDateTime = LocalDateTime.now(),
    private var updatedAt: LocalDateTime = LocalDateTime.now(),
) : TestFixture<BoardCategory> {
    override fun create(): BoardCategory =
        BoardCategory
            .create(name, displayName, description)

    fun withName(name: String) = apply { this.name = name }

    fun withDisplayName(displayName: String) = apply { this.displayName = displayName }

    fun withDescription(description: String) = apply { this.description = description }

    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

    companion object {
        fun aCategory() = CategoryFixture()
    }
}
