package com.example.jobstat.board.internal.entity

import com.example.jobstat.core.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "board_categories")
class BoardCategory(
    @Column(nullable = false)
    var name: String,
) : BaseEntity() {
    companion object {
        fun create(name: String): BoardCategory = BoardCategory(name)
    }
}
