package com.wildrew.jobstat.statistics_read.develop

import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseMongoRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseMongoRepositoryImpl
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.regex.Pattern

@NoRepositoryBean
interface UserDashboardConfigRepository : BaseMongoRepository<UserDashboardConfigDocument, String> {
    fun findByUserId(userId: Long): UserDashboardConfigDocument?

    fun findByPreferredMetrics(metrics: List<String>): List<UserDashboardConfigDocument>

    fun findByPreferredEntities(entityType: String): List<UserDashboardConfigDocument>

    fun updateDashboardLayout(
        userId: Long,
        layout: UserDashboardConfigDocument.DashboardLayout,
    ): UserDashboardConfigDocument?

    fun updateNotificationSettings(
        userId: Long,
        settings: UserDashboardConfigDocument.NotificationSettings,
    ): UserDashboardConfigDocument?
}

@Repository
class UserDashboardConfigRepositoryImpl(
    private val entityInformation: MongoEntityInformation<UserDashboardConfigDocument, String>,
    private val mongoOperations: MongoOperations,
) : BaseMongoRepositoryImpl<UserDashboardConfigDocument, String>(entityInformation, mongoOperations),
    UserDashboardConfigRepository {
    init {
//        mongoOperations.indexOps(entityInformation.javaType).ensureIndex(
//            Index()
//                .on("user_id", Sort.Direction.ASC)
//                .unique()
//                .background()
//        )
    }

    override fun findByUserId(userId: Long): UserDashboardConfigDocument? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("user_id", userId),
            ).firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findByPreferredMetrics(metrics: List<String>): List<UserDashboardConfigDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.`in`("preferred_metrics", metrics),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByPreferredEntities(entityType: String): List<UserDashboardConfigDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.regex("preferred_entities", Pattern.compile("^$entityType:")),
            ).map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun updateDashboardLayout(
        userId: Long,
        layout: UserDashboardConfigDocument.DashboardLayout,
    ): UserDashboardConfigDocument? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val updateResult =
            collection.findOneAndUpdate(
                Filters.eq("user_id", userId),
                Updates.combine(
                    Updates.set("dashboard_layout", mongoOperations.converter.convertToMongoType(layout)),
                    Updates.set("updatedAt", Instant.now()),
                ),
                FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER),
            )

        return updateResult?.let { doc ->
            mongoOperations.converter.read(entityInformation.javaType, doc)
        }
    }

    override fun updateNotificationSettings(
        userId: Long,
        settings: UserDashboardConfigDocument.NotificationSettings,
    ): UserDashboardConfigDocument? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val updateResult =
            collection.findOneAndUpdate(
                Filters.eq("user_id", userId),
                Updates.combine(
                    Updates.set(
                        "notification_settings",
                        mongoOperations.converter.convertToMongoType(settings),
                    ),
                    Updates.set("updatedAt", Instant.now()),
                ),
                FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER),
            )

        return updateResult?.let { doc ->
            mongoOperations.converter.read(entityInformation.javaType, doc)
        }
    }
}
