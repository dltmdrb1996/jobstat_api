package com.wildrew.jobstat.statistics_read.rankings.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.RelationshipRankingRepository
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.repository.RelationshipRankingRepositoryImpl
import com.wildrew.jobstat.statistics_read.rankings.document.JobCategorySkillRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface JobCategorySkillRankingsRepository : RelationshipRankingRepository<JobCategorySkillRankingsDocument, JobCategorySkillRankingsDocument.JobCategorySkillRankingEntry, String> {
    // 스킬 연관성 분석
    fun findCorrelatedSkills(
        baseDate: String,
        minCorrelation: Double,
    ): List<JobCategorySkillRankingsDocument>

    // 필수 스킬 분석
    fun findCriticalSkills(
        baseDate: String,
        minRequiredRate: Double,
    ): List<JobCategorySkillRankingsDocument>

    // 성장하는 스킬 분석
    fun findEmergingSkills(
        baseDate: String,
        minGrowthRate: Double,
        minImportanceScore: Double,
    ): List<JobCategorySkillRankingsDocument>
}

@Repository
@RankingRepositoryType(RankingType.JOB_CATEGORY_SKILL)
class JobCategorySkillRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<JobCategorySkillRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : RelationshipRankingRepositoryImpl<JobCategorySkillRankingsDocument, JobCategorySkillRankingsDocument.JobCategorySkillRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    JobCategorySkillRankingsRepository {
    override fun findCorrelatedSkills(
        baseDate: String,
        minCorrelation: Double,
    ): List<JobCategorySkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.unwind("\$metrics.skill_correlation.complementary_skills"),
                Aggregates.match(
                    Filters.gte(
                        "metrics.skill_correlation.complementary_skills.correlation_score",
                        minCorrelation,
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.skill_correlation.complementary_skills.correlation_score",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findCriticalSkills(
        baseDate: String,
        minRequiredRate: Double,
    ): List<JobCategorySkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.match(
                    Filters.gte("rankings.related_rankings.required_rate", minRequiredRate),
                ),
                Aggregates.sort(Sorts.descending("rankings.related_rankings.required_rate")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findEmergingSkills(
        baseDate: String,
        minGrowthRate: Double,
        minImportanceScore: Double,
    ): List<JobCategorySkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.match(
                    Filters.and(
                        Filters.gte("rankings.related_rankings.growth_rate", minGrowthRate),
                        Filters.gte("rankings.related_rankings.importance_score", minImportanceScore),
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("rankings.related_rankings.growth_rate"),
                        Sorts.descending("rankings.related_rankings.importance_score"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
