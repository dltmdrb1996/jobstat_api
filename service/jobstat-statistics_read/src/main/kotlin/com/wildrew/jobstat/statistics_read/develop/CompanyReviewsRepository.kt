package com.wildrew.jobstat.statistics_read.develop

import com.mongodb.client.model.Accumulators.avg
import com.mongodb.client.model.Aggregates.*
import com.mongodb.client.model.Filters
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepositoryImpl
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.ReferenceMongoRepository
import com.wildrew.jobstat.statistics_read.develop.CompanyReviewsDocument
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository
import java.time.Instant

@NoRepositoryBean
interface CompanyReviewsRepository : ReferenceMongoRepository<CompanyReviewsDocument, String> {
    fun findByCompanyIdAndRatingGreaterThan(
        companyId: Long,
        rating: Double,
    ): List<CompanyReviewsDocument>

    fun findByCompanyIdsAndDateBetween(
        companyIds: List<Long>,
        startDate: Instant,
        endDate: Instant,
    ): List<CompanyReviewsDocument>

    fun calculateAverageRatingByCompanyId(companyId: Long): Double
}

@Repository
class CompanyReviewsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanyReviewsDocument, String>,
    private val mongoOperations: MongoOperations,
) : ReferenceMongoRepositoryImpl<CompanyReviewsDocument, String>(entityInformation, mongoOperations),
    CompanyReviewsRepository {
    override fun findByCompanyIdAndRatingGreaterThan(
        companyId: Long,
        rating: Double,
    ): List<CompanyReviewsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("reference_id", companyId),
                    Filters.gt("reviews.rating", rating),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByCompanyIdsAndDateBetween(
        companyIds: List<Long>,
        startDate: Instant,
        endDate: Instant,
    ): List<CompanyReviewsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.`in`("reference_id", companyIds),
                    Filters.gte("reviews.date", startDate),
                    Filters.lte("reviews.date", endDate),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun calculateAverageRatingByCompanyId(companyId: Long): Double {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val result =
            collection
                .aggregate(
                    listOf(
                        match(Filters.eq("reference_id", companyId)),
                        unwind("\$reviews"),
                        group(null, avg("averageRating", "\$reviews.rating")),
                    ),
                ).first()

        return result?.getDouble("averageRating") ?: 0.0
    }
}
