package com.wildrew.jobstat.statistics_read.rankings.repository

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.BaseRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.BaseRankingRepository
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.stereotype.Component
import kotlin.reflect.full.findAnnotation

@Component
class RankingRepositoryRegistry(
    repositories: List<BaseRankingRepository<*, *, *>>,
) {
    private val repositoryMap: Map<RankingType, BaseRankingRepository<*, *, *>> = initializeRepositoryMap(repositories)

    private fun initializeRepositoryMap(repositories: List<BaseRankingRepository<*, *, *>>): Map<RankingType, BaseRankingRepository<*, *, *>> =
        repositories.associateBy { repo ->
            val actualClass =
                if (repo.javaClass.name.contains("SpringCGLIB")) {
                    repo.javaClass.superclass.kotlin
                } else {
                    repo.javaClass.kotlin
                }
            actualClass.findAnnotation<RankingRepositoryType>()?.type
                ?: throw IllegalArgumentException("RankingRepositoryType annotation not found on ${repo.javaClass.simpleName}")
        }

    @Suppress("UNCHECKED_CAST")
    fun <T : BaseRankingDocument<*>> getRepository(rankingType: RankingType): BaseRankingRepository<T, *, *> =
        repositoryMap[rankingType] as? BaseRankingRepository<T, *, *>
            ?: throw IllegalArgumentException("저장소를 찾을 수 없습니다: $rankingType")

    fun hasRepository(rankingType: RankingType): Boolean = rankingType in repositoryMap
}
