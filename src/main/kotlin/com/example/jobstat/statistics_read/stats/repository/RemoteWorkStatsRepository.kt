package com.example.jobstat.statistics_read.stats.repository

import com.example.jobstat.core.base.repository.StatsMongoRepository
import com.example.jobstat.core.base.repository.StatsMongoRepositoryImpl
import com.example.jobstat.statistics_read.stats.document.RemoteWorkStatsDocument
import com.example.jobstat.statistics_read.stats.registry.StatsRepositoryType
import com.example.jobstat.statistics_read.stats.registry.StatsType
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@StatsRepositoryType(StatsType.REMOTE_WORK)
@NoRepositoryBean
interface RemoteWorkStatsRepository : StatsMongoRepository<RemoteWorkStatsDocument, String> {
    fun findByType(type: String): List<RemoteWorkStatsDocument>

    fun findByTypeAndBaseDateBetween(
        type: String,
        startDate: String,
        endDate: String,
    ): List<RemoteWorkStatsDocument>

    fun findByProductivityIndexGreaterThan(index: Double): List<RemoteWorkStatsDocument>

    fun findByAdoptionRateGreaterThan(rate: Double): List<RemoteWorkStatsDocument>

    fun findTopBySatisfactionScore(limit: Int): List<RemoteWorkStatsDocument>

    fun findByIndustryIdAndType(
        industryId: Long,
        type: String,
    ): List<RemoteWorkStatsDocument>

    fun findByJobCategoryIdAndAdoptionRateGreaterThan(
        jobCategoryId: Long,
        adoptionRate: Double,
    ): List<RemoteWorkStatsDocument>

    fun calculateAverageProductivityByType(type: String): Double

    fun findTopPerformingIndustries(limit: Int): List<Document>
}

@Repository
class RemoteWorkStatsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<RemoteWorkStatsDocument, String>,
    private val mongoOperations: MongoOperations,
) : StatsMongoRepositoryImpl<RemoteWorkStatsDocument, String>(entityInformation, mongoOperations),
    RemoteWorkStatsRepository {
    override fun findByType(type: String): List<RemoteWorkStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(Filters.eq("type", type))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByTypeAndBaseDateBetween(
        type: String,
        startDate: String,
        endDate: String,
    ): List<RemoteWorkStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("type", type),
                    Filters.gte("base_date", startDate),
                    Filters.lte("base_date", endDate),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByProductivityIndexGreaterThan(index: Double): List<RemoteWorkStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.productivity_index", index),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByAdoptionRateGreaterThan(rate: Double): List<RemoteWorkStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.gt("stats.adoption_rate", rate),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findTopBySatisfactionScore(limit: Int): List<RemoteWorkStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find()
            .sort(Sorts.descending("satisfaction_metrics.overall_satisfaction"))
            .limit(limit)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByIndustryIdAndType(
        industryId: Long,
        type: String,
    ): List<RemoteWorkStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("industry_distribution.industry_id", industryId),
                    Filters.eq("type", type),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByJobCategoryIdAndAdoptionRateGreaterThan(
        jobCategoryId: Long,
        adoptionRate: Double,
    ): List<RemoteWorkStatsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("job_category_distribution.job_category_id", jobCategoryId),
                    Filters.gt("stats.adoption_rate", adoptionRate),
                ),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun calculateAverageProductivityByType(type: String): Double {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("type", type)),
                Aggregates.group(
                    null,
                    Accumulators.avg("avgProductivity", "\$productivity_metrics.overall_productivity"),
                ),
            )

        return collection
            .aggregate(pipeline)
            .firstOrNull()
            ?.getDouble("avgProductivity")
            ?: 0.0
    }

    override fun findTopPerformingIndustries(limit: Int): List<Document> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.unwind("\$industry_distribution"),
                Aggregates.group(
                    "\$industry_distribution.industry_id",
                    Accumulators.first("industryName", "\$industry_distribution.name"),
                    Accumulators.avg("avgProductivity", "\$industry_distribution.productivity_score"),
                    Accumulators.avg("avgAdoptionRate", "\$industry_distribution.adoption_rate"),
                ),
                Aggregates.sort(Sorts.descending("avgProductivity")),
                Aggregates.limit(limit),
            )

        return collection
            .aggregate(pipeline)
            .toList()
    }
}
