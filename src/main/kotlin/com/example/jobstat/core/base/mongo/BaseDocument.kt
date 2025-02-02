package com.example.jobstat.core.base.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType
import java.io.Serializable
import java.time.Instant

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
    protected abstract fun validate()

    protected fun refreshUpdatedAt() {
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
