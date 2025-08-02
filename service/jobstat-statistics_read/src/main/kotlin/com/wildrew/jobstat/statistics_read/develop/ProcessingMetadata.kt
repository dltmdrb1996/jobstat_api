package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseDocument
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "statistics_processing_metadata")
data class ProcessingMetadata(
    @Field("period")
    val period: SnapshotPeriod,
    @Field("processed")
    val processed: Int
) : BaseDocument() {
    override fun validate() {
    }
}