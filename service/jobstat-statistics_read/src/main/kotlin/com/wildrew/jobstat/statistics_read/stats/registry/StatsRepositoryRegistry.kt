package com.wildrew.jobstat.statistics_read.stats.registry

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.StatsMongoRepository
import org.springframework.stereotype.Component
import kotlin.reflect.full.findAnnotation

@Component
class StatsRepositoryRegistry(
    repositories: List<StatsMongoRepository<*, *>>,
) {
    private val repositoryMap: Map<StatsType, StatsMongoRepository<*, *>> = initializeRepositoryMap(repositories)

    private fun initializeRepositoryMap(repositories: List<StatsMongoRepository<*, *>>): Map<StatsType, StatsMongoRepository<*, *>> =
        repositories.associateBy { repo ->
            val actualClass =
                if (repo.javaClass.name.contains("SpringCGLIB")) {
                    repo.javaClass.superclass.kotlin
                } else {
                    repo.javaClass.kotlin
                }
            actualClass.findAnnotation<StatsRepositoryType>()?.type
                ?: throw IllegalArgumentException("StatsRepositoryType annotation not found on ${repo.javaClass.simpleName}")
        }

    @Suppress("UNCHECKED_CAST")
    fun <T : BaseStatsDocument> getRepository(statsType: StatsType): StatsMongoRepository<T, String> =
        repositoryMap[statsType] as? StatsMongoRepository<T, String>
            ?: throw IllegalArgumentException("해당 타입의 리포지토리를 찾을 수 없습니다: $statsType")

    fun hasRepository(statsType: StatsType): Boolean = statsType in repositoryMap
}
