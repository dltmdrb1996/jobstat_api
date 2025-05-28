package com.wildrew.jobstat.community.board.entity

import com.wildrew.jobstat.core.core_jpa_base.base.BaseAutoIncEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "board_categories",
    indexes = [Index(name = "idx_category_name", columnList = "name", unique = true)],
)
class BoardCategory(
    name: String,
    displayName: String,
    description: String,
) : BaseAutoIncEntity() {
    @Column(nullable = false, unique = true, length = 50)
    var name: String = name
        protected set

    @Column(nullable = false, length = 50)
    var displayName: String = displayName
        protected set

    @Column(nullable = false, length = 255)
    var description: String = description
        protected set

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    var boards: MutableSet<Board> = mutableSetOf()
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
