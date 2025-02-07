package com.example.jobstat.statistics.stats.registry

import com.example.jobstat.core.base.mongo.stats.BaseStatsDocument
import com.example.jobstat.core.base.repository.StatsMongoRepository
import org.springframework.stereotype.Component
import kotlin.reflect.full.findAnnotation

@Component
class StatsRepositoryRegistry(
    repositories: List<StatsMongoRepository<*, *>>,
) {
    private val repositoryMap: Map<StatsType, StatsMongoRepository<*, *>> = initializeRepositoryMap(repositories)

    private fun initializeRepositoryMap(repositories: List<StatsMongoRepository<*, *>>): Map<StatsType, StatsMongoRepository<*, *>> =
        repositories.associateBy { repo ->
            repo.javaClass.kotlin
                .findAnnotation<StatsRepositoryType>()
                ?.type
                ?: getStatsTypeFromCollectionName(repo.getCollectionName())
                ?: throw IllegalArgumentException("리포지토리의 StatsType을 결정할 수 없습니다: ${repo.javaClass}")
        }

    private fun getStatsTypeFromCollectionName(collectionName: String): StatsType? =
        when {
            collectionName.contains("benefit") -> StatsType.BENEFIT
            collectionName.contains("certification") -> StatsType.CERTIFICATION
            collectionName.contains("company") -> StatsType.COMPANY
            collectionName.contains("contract_type") -> StatsType.CONTRACT_TYPE
            collectionName.contains("education") -> StatsType.EDUCATION
            collectionName.contains("experience") -> StatsType.EXPERIENCE
            collectionName.contains("industry") -> StatsType.INDUSTRY
            collectionName.contains("job_category") -> StatsType.JOB_CATEGORY
            collectionName.contains("location") -> StatsType.LOCATION
            collectionName.contains("remote_work") -> StatsType.REMOTE_WORK
            collectionName.contains("skill") -> StatsType.SKILL
            else -> null
        }

    @Suppress("UNCHECKED_CAST")
    fun <T : BaseStatsDocument> getRepository(statsType: StatsType): StatsMongoRepository<T, String> =
        repositoryMap[statsType] as? StatsMongoRepository<T, String>
            ?: throw IllegalArgumentException("해당 타입의 리포지토리를 찾을 수 없습니다: $statsType")

    fun hasRepository(statsType: StatsType): Boolean = statsType in repositoryMap
}
