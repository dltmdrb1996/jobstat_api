package com.wildrew.app.utils.dummy

import com.wildrew.app.statistics_read.core.core_mongo_base.model.BaseDocument
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.BaseMongoRepository
import com.wildrew.app.statistics_read.core.core_mongo_base.repository.BaseMongoRepositoryImpl
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface RecordRepository : BaseMongoRepository<Record, String>

@Repository
class RecordRepositoryImpl(
    private val entityInformation: MongoEntityInformation<Record, String>,
    private val mongoOperations: MongoOperations,
) : BaseMongoRepositoryImpl<Record, String>(
        entityInformation,
        mongoOperations,
    ),
    RecordRepository

@Document(collection = "records")
class Record(
    name: String,
    age: Int,
    data: RecordDto?,
) : BaseDocument() {
    @Field("name")
    var name: String = name
        protected set

    @Field("age")
    var age: Int = age
        protected set

    @Field("data")
    var data: RecordDto? = data
        protected set

    init {
        validate()
    }

    override fun validate() {
    }

    override fun toString(): String = "Record(id =$id createAt = $createdAt name='$name', age=$age, data=$data)"

    fun update(
        name: String,
        age: Int,
        data: RecordDto?,
    ): Record {
        this.name = name
        this.age = age
        this.data = data
        refreshUpdatedAt()
        validate()

        return this
    }
}

data class RecordDto(
    @Field("address")
    val name: String,
    @Field("age")
    val age: Int,
    @Field("data")
    val address: Address,
)

data class Address(
    @Field("street")
    val street: String?,
    @Field("city")
    val city: String,
)
