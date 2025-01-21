package com.example.jobstat.rankings.repository

import com.example.jobstat.core.base.repository.RelationshipRankingRepository
import com.example.jobstat.core.base.repository.RelationshipRankingRepositoryImpl
import com.example.jobstat.rankings.model.IndustrySkillRankingsDocument
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@NoRepositoryBean
interface IndustrySkillRankingsRepository
    :
    RelationshipRankingRepository<IndustrySkillRankingsDocument, IndustrySkillRankingsDocument.IndustrySkillRankingEntry, String> {
    // 산업 간 공통 스킬 분석
    fun findCrossIndustrySkills(
        baseDate: String,
        minCommonSkills: Int,
    ): List<IndustrySkillRankingsDocument>

    // 산업별 특화 스킬 분석
    fun findIndustrySpecificSkills(
        baseDate: String,
        minSpecificityScore: Double,
    ): List<IndustrySkillRankingsDocument>

    // 스킬 전환 가능성 분석
    fun findSkillTransitionOpportunities(
        baseDate: String,
        sourceIndustryId: Long,
    ): List<IndustrySkillRankingsDocument>
}

@Repository
class IndustrySkillRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<IndustrySkillRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : RelationshipRankingRepositoryImpl<IndustrySkillRankingsDocument, IndustrySkillRankingsDocument.IndustrySkillRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    IndustrySkillRankingsRepository {
    override fun findCrossIndustrySkills(
        baseDate: String,
        minCommonSkills: Int,
    ): List<IndustrySkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$gte",
                            listOf(
                                Document("\$size", "\$metrics.industry_skill_correlation.cross_industry_skills"),
                                minCommonSkills,
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("rankings.related_rankings.score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findIndustrySpecificSkills(
        baseDate: String,
        minSpecificityScore: Double,
    ): List<IndustrySkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.unwind("\$rankings.related_rankings"),
                Aggregates.match(
                    Filters.gte(
                        "rankings.related_rankings.industry_specificity",
                        minSpecificityScore,
                    ),
                ),
                Aggregates.sort(Sorts.descending("rankings.related_rankings.industry_specificity")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSkillTransitionOpportunities(
        baseDate: String,
        sourceIndustryId: Long,
    ): List<IndustrySkillRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Document(
                        "\$expr",
                        Document(
                            "\$eq",
                            listOf(
                                "\$metrics.industry_skill_correlation.skill_transition_patterns.from_industry_id",
                                sourceIndustryId,
                            ),
                        ),
                    ),
                ),
                Aggregates.unwind("\$metrics.industry_skill_correlation.skill_transition_patterns"),
                Aggregates.sort(
                    Sorts.descending(
                        "metrics.industry_skill_correlation.skill_transition_patterns.transition_score",
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
