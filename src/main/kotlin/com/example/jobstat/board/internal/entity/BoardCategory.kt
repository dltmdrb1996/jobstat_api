package com.example.jobstat.board.internal.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.*

interface ReadBoardCategory {
    val id: Long
    val name: String
    val displayName: String
    val description: String
    val boards: Set<ReadBoard>
}

@Entity
@Table(
    name = "board_categories",
    indexes = [Index(name = "idx_category_name", columnList = "name", unique = true)],
)
internal class BoardCategory protected constructor(
    name: String,
    displayName: String,
    description: String,
) : BaseEntity(),
    ReadBoardCategory {
    @Column(nullable = false, unique = true, length = 50)
    override var name: String = name
        protected set

    @Column(nullable = false, length = 50)
    override var displayName: String = displayName
        protected set

    @Column(nullable = false, length = 255)
    override var description: String = description
        protected set

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    override var boards: MutableSet<Board> = mutableSetOf()
        protected set

    fun updateName(name: String) {
        require(name.isNotBlank()) { "이름이 비어있습니다" }
        this.name = name
    }

    fun updateCategory(
        name: String,
        displayName: String,
        description: String,
    ) {
        require(name.isNotBlank()) { "이름이 비어있습니다" }
        require(displayName.isNotBlank()) { "표시 이름이 비어있습니다" }
        this.name = name
        this.displayName = displayName
        this.description = description
    }

    companion object {
        fun create(
            name: String,
            displayName: String,
            description: String,
        ): BoardCategory {
            require(name.isNotBlank()) { "이름이 비어있습니다" }
            require(displayName.isNotBlank()) { "표시 이름이 비어있습니다" }
            return BoardCategory(name, displayName, description)
        }
    }
}
