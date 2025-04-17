package com.example.jobstat.statistics_read.rankings.fake

import com.example.jobstat.core.base.mongo.ranking.BaseRankingDocument
import com.example.jobstat.core.base.repository.BaseRankingRepository
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.example.jobstat.statistics_read.rankings.repository.RankingRepositoryRegistry

class FakeRankingRepositoryRegistry(
    private val repositories: Map<RankingType, BaseRankingRepository<*, *, *>>,
) : RankingRepositoryRegistry(emptyList()) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : BaseRankingDocument<*>> getRepository(rankingType: RankingType): BaseRankingRepository<T, *, *> =
        repositories[rankingType] as? BaseRankingRepository<T, *, *>
            ?: throw IllegalArgumentException("Repository not found for type: $rankingType")

    override fun hasRepository(rankingType: RankingType): Boolean = rankingType in repositories
}
