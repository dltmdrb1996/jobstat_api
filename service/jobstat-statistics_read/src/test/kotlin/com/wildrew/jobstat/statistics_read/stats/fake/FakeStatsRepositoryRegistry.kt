package com.wildrew.jobstat.statistics_read.stats.fake

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import com.wildrew.jobstat.statistics_read.stats.registry.StatsRepositoryRegistry
import com.wildrew.jobstat.statistics_read.stats.registry.StatsType

class FakeStatsRepositoryRegistry(
    private val repositories: Map<StatsType, StatsMongoRepository<*, *>>,
) : StatsRepositoryRegistry(emptyList()) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : BaseStatsDocument> getRepository(statsType: StatsType): StatsMongoRepository<T, String> =
        repositories[statsType] as? StatsMongoRepository<T, String>
            ?: throw IllegalArgumentException("Repository not found for type: $statsType")

    override fun hasRepository(statsType: StatsType): Boolean = statsType in repositories
}
