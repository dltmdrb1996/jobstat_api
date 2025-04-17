package com.example.jobstat.statistics_read.rankings.repository

import com.example.jobstat.core.base.repository.SimpleRankingRepository
import com.example.jobstat.core.base.repository.SimpleRankingRepositoryImpl
import com.example.jobstat.statistics_read.rankings.document.SkillGrowthRankingsDocument
import com.example.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository

@RankingRepositoryType(RankingType.SKILL_GROWTH)
@NoRepositoryBean
interface SkillGrowthRankingsRepository : SimpleRankingRepository<SkillGrowthRankingsDocument, SkillGrowthRankingsDocument.SkillGrowthRankingEntry, String> {
    // 성장 일관성 분석
    fun findByGrowthConsistency(
        baseDate: String,
        minConsistency: Double,
        minGrowthRate: Double,
    ): List<SkillGrowthRankingsDocument>

    // 시장 침투율 기반 분석
    fun findByMarketPenetration(
        baseDate: String,
        minPenetration: Double,
    ): List<SkillGrowthRankingsDocument>

    // 복합 성장 요소 분석
    fun findByMultiFactorGrowth(
        baseDate: String,
        minDemandGrowth: Double,
        minSalaryGrowth: Double,
    ): List<SkillGrowthRankingsDocument>
}

@Repository
class SkillGrowthRankingsRepositoryImpl(
    private val entityInformation: MongoEntityInformation<SkillGrowthRankingsDocument, String>,
    private val mongoOperations: MongoOperations,
) : SimpleRankingRepositoryImpl<SkillGrowthRankingsDocument, SkillGrowthRankingsDocument.SkillGrowthRankingEntry, String>(
        entityInformation,
        mongoOperations,
    ),
    SkillGrowthRankingsRepository {
    override fun findByGrowthConsistency(
        baseDate: String,
        minConsistency: Double,
        minGrowthRate: Double,
    ): List<SkillGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.and(
                        Filters.gte("growth_consistency", minConsistency),
                        Filters.gte("growth_rate", minGrowthRate),
                    ),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("growth_consistency"),
                        Sorts.descending("growth_rate"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByMarketPenetration(
        baseDate: String,
        minPenetration: Double,
    ): List<SkillGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.gte("growth_factors.market_penetration", minPenetration),
                ),
                Aggregates.sort(
                    Sorts.orderBy(
                        Sorts.descending("growth_factors.market_penetration"),
                        Sorts.descending("growth_factors.adoption_rate"),
                    ),
                ),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findByMultiFactorGrowth(
        baseDate: String,
        minDemandGrowth: Double,
        minSalaryGrowth: Double,
    ): List<SkillGrowthRankingsDocument> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        val pipeline =
            listOf(
                Aggregates.match(Filters.eq("base_date", baseDate)),
                Aggregates.match(
                    Filters.and(
                        Filters.gte("growth_factors.demand_growth", minDemandGrowth),
                        Filters.gte("growth_factors.salary_growth", minSalaryGrowth),
                    ),
                ),
                Aggregates.addFields(
                    Field(
                        "growth_score",
                        Document(
                            "\$add",
                            listOf(
                                "\$growth_factors.demand_growth",
                                "\$growth_factors.salary_growth",
                            ),
                        ),
                    ),
                ),
                Aggregates.sort(Sorts.descending("growth_score")),
            )

        return collection
            .aggregate(pipeline)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }
}
