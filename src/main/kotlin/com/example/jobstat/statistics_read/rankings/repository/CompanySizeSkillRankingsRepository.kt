package com.example.jobstat.statistics_read.rankings.repository

import com.example.jobstat.core.base.repository.RelationshipRankingRepository
import com.example.jobstat.core.base.repository.RelationshipRankingRepositoryImpl
import com.example.jobstat.core.state.CompanySize
import com.example.jobstat.statistics_read.rankings.document.CompanySizeSkillRankingsDocument
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.COMPANY_SIZE_SKILL_DEMAND)
@NoRepositoryBean
interface CompanySizeSkillRankingsRepository : RelationshipRankingRepository<CompanySizeSkillRankingsDocument, CompanySizeSkillRankingsDocument.CompanySizeSkillRankingEntry, String> {
    // 회사 규모별 핵심 스킬 분석
    fun findCoreSkillsByCompanySize(
        baseDate: String,
        companySize: CompanySize,
        minRequirementLevel: Double,
    ): List<CompanySizeSkillRankingsDocument>

    // 스킬 복잡도 분석
    fun findSkillComplexityPatterns(
        baseDate: String,
        minComplexityScore: Double,
    ): List<CompanySizeSkillRankingsDocument>

    // 성장하는 스킬 트렌드 분석
    fun findEmergingSkillsBySize(
        baseDate: String,
        companySize: CompanySize,
        minGrowthRate: Double,
    ): List<CompanySizeSkillRankingsDocument>

    // 스킬 채택률 기반 분석
    fun findHighAdoptionSkills(
        baseDate: String,
        minAdoptionRate: Double,
    ): List<CompanySizeSkillRankingsDocument>
}

@Repository
class CompanySizeSkillRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<CompanySizeSkillRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : RelationshipRankingRepositoryImpl<CompanySizeSkillRankingsDocument, CompanySizeSkillRankingsDocument.CompanySizeSkillRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    CompanySizeSkillRankingsRepository {
    override fun findCoreSkillsByCompanySize(
        baseDate: String,
        companySize: CompanySize,
        minRequirementLevel: Double,
    ): List<CompanySizeSkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(Filters.eq("rankings.primary_entity_name", companySize.name)),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.match(
                    Filters.gte("rankings.related_rankings.requirement_level", minRequirementLevel),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("rankings.related_rankings.requirement_level"),
                        Sorts.descending("rankings.related_rankings.score"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSkillComplexityPatterns(
        baseDate: String,
        minComplexityScore: Double,
    ): List<CompanySizeSkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.exists("metrics.size_skill_correlation.skill_complexity", true),
                ),
                Aggregates.unwind("\$metrics.size_skill_correlation.skill_complexity"),
                Aggregates.match(
                    Filters.gte(
                        "metrics.size_skill_correlation.skill_complexity.complexity_score",
                        minComplexityScore,
                    ),
                ),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.size_skill_correlation.skill_complexity.complexity_score",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findEmergingSkillsBySize(
        baseDate: String,
        companySize: CompanySize,
        minGrowthRate: Double,
    ): List<CompanySizeSkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(Filters.eq("rankings.primary_entity_name", companySize.name)),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.match(Filters.gte("rankings.related_rankings.growth_rate", minGrowthRate)),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("rankings.related_rankings.growth_rate"),
                        Sorts.descending("rankings.related_rankings.score"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findHighAdoptionSkills(
        baseDate: String,
        minAdoptionRate: Double,
    ): List<CompanySizeSkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(Filters.gte("rankings.skill_adoption_rate", minAdoptionRate)),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("rankings.skill_adoption_rate"),
                        Sorts.descending("rankings.total_postings"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
