package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseMongoRepositoryImpl
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository


@NoRepositoryBean
interface ProcessingMetadataRepository : BaseMongoRepository<ProcessingMetadata, String>

@Repository
class ProcessingMetadataRepositoryImpl(
    private val entityInformation: MongoEntityInformation<ProcessingMetadata, String>,
    private val mongoOperations: MongoOperations,
) : BaseMongoRepositoryImpl<ProcessingMetadata, String>(
    entityInformation,
    mongoOperations,
), ProcessingMetadataRepository
