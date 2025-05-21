package com.example.jobstat.statistics_read.core.core_mongo_base.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.io.Serializable
import java.time.Instant

// 모든 MongoDB 문서의 기본이 되는 추상 클래스
// 생성 시간과 수정 시간을 자동으로 관리합니다
abstract class BaseDocument(
    @Id
    @Field(name = "_id", targetType = FieldType.OBJECT_ID)
    open var id: String? = null,
    @Indexed(background = true)
    @Field("createdAt")
    open var createdAt: Instant = Instant.now(),
    @Indexed(background = true)
    @Field("updatedAt")
    open var updatedAt: Instant = Instant.now(),
) : Serializable {
    protected abstract fun validate() // 검증 메서드

    protected fun refreshUpdatedAt() { // 수정일시 갱신
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseDocument

        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
