package com.example.jobstat.statistics.stats.fake

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.repository.StatsMongoRepository
import com.example.jobstat.statistics.stats.registry.StatsRepositoryRegistry
import com.example.jobstat.statistics.stats.registry.StatsType

class FakeStatsRepositoryRegistry(
    private val repositories: Map<StatsType, StatsMongoRepository<*, *>>,
) : StatsRepositoryRegistry(emptyList()) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : BaseStatsDocument> getRepository(statsType: StatsType): StatsMongoRepository<T, String> =
        repositories[statsType] as? StatsMongoRepository<T, String>
            ?: throw IllegalArgumentException("Repository not found for type: $statsType")

    override fun hasRepository(statsType: StatsType): Boolean = statsType in repositories
}
