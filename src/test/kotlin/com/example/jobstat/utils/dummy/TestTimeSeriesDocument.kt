package com.example.jobstat.utils.dummy

import com.example.jobstat.statistics_read.core.core_mongo_base.model.BaseTimeSeriesDocument
import com.example.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.example.jobstat.statistics_read.core.core_mongo_base.repository.BaseTimeSeriesRepositoryImpl
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.stereotype.Repository

@Document(collection = "test_time_series")
class TestTimeSeriesDocument(
    id: String? = null,
    baseDate: String,
    period: SnapshotPeriod,
    @Field("value")
    val value: Double,
    @Field("category")
    val category: String,
) : BaseTimeSeriesDocument(id, baseDate, period) {
    override fun validate() {
        require(value >= 0) { "Value must be non-negative" }
    }
}

@Repository
class TestTimeSeriesRepository(
    private val entityInformation: MongoEntityInformation<TestTimeSeriesDocument, String>,
    private val mongoOperations: MongoOperations,
) : BaseTimeSeriesRepositoryImpl<TestTimeSeriesDocument, String>(
        entityInformation,
        mongoOperations,
    )
