package com.example.jobstat.core.base

import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
abstract class SoftDeleteBaseAutoIncEntity : BaseAutoIncEntity() {
    @Column(nullable = false)
    protected var _deleted: Boolean = false

    val isDeleted: Boolean
        get() = _deleted

    @Column
    protected var _deletedAt: LocalDateTime? = null

    val deletedAt: LocalDateTime?
        get() = _deletedAt

    /**
     * 소프트 삭제 플래그와 삭제시각 설정
     */
    fun delete() {
        this._deleted = true
        this._deletedAt = LocalDateTime.now()
    }

    /**
     * 소프트 삭제 복구
     */
    fun restore() {
        this._deleted = false
        this._deletedAt = null
    }
}
