package com.example.jobstat.statistics.stats.fake

import com.example.jobstat.statistics.rankings.model.RankingType
import com.example.jobstat.statistics.stats.document.SkillStatsDocument
import com.example.jobstat.statistics.stats.repository.SkillStatsRepository
import com.example.jobstat.utils.base.AbstractFakeStatsRepository
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.*
import java.util.function.Function

class FakeSkillStatsRepository :
    AbstractFakeStatsRepository<SkillStatsDocument>(),
    SkillStatsRepository {
    override fun findByGrowthRateGreaterThan(growthRate: Double): List<SkillStatsDocument> =
        documents.values
            .filter { doc -> doc.stats.growthRate > growthRate }

    override fun findEmergingSkills(limit: Int): List<SkillStatsDocument> =
        documents.values
            .filter { it.isEmergingSkill }
            .sortedByDescending { it.stats.growthRate }
            .take(limit)

    override fun findByDemandScoreGreaterThan(demandScore: Double): List<SkillStatsDocument> =
        documents.values
            .filter { (it.stats as SkillStatsDocument.SkillStats).demandScore > demandScore }

    override fun findByJobCategoryIdAndBaseDate(
        jobCategoryId: Long,
        baseDate: String,
    ): List<SkillStatsDocument> =
        documents.values
            .filter { doc ->
                doc.baseDate == baseDate &&
                    doc.relatedJobCategories.any { it.jobCategoryId == jobCategoryId }
            }

    override fun calculateRank(
        type: RankingType,
        value: Double,
    ): Int {
        val values =
            documents.values
                .mapNotNull { doc -> doc.rankings[type]?.rankingScore?.value }
                .filter { it > value }
                .size
        return values + 1
    }

    override fun calculatePercentile(
        type: RankingType,
        value: Double,
    ): Double {
        val allValues =
            documents.values
                .mapNotNull { doc -> doc.rankings[type]?.rankingScore?.value }

        if (allValues.isEmpty()) return 0.0

        val belowCount = allValues.count { it <= value }
        return (belowCount.toDouble() / allValues.size.toDouble()) * 100
    }

    override fun calculateMedianSalary(): Long {
        val salaries =
            documents.values
                .map { it.stats.avgSalary }
                .sorted()

        return if (salaries.isEmpty()) {
            0L
        } else {
            salaries[salaries.size / 2]
        }
    }

    override fun findByEntityIdAndBaseDateBetween(
        entityId: Long,
        startDate: String,
        endDate: String,
    ): List<SkillStatsDocument> =
        documents.values
            .filter { doc ->
                doc.entityId == entityId &&
                    doc.baseDate >= startDate &&
                    doc.baseDate <= endDate
            }.sortedByDescending { it.baseDate }

    override fun <S : SkillStatsDocument?> findAll(example: Example<S>): MutableList<S> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?> findAll(
        example: Example<S>,
        sort: Sort,
    ): MutableList<S> {
        TODO("Not yet implemented")
    }

    override fun findAll(sort: Sort): MutableList<SkillStatsDocument> {
        TODO("Not yet implemented")
    }

    override fun findAll(pageable: Pageable): Page<SkillStatsDocument> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?> findAll(
        example: Example<S>,
        pageable: Pageable,
    ): Page<S> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?> count(example: Example<S>): Long {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?> findOne(example: Example<S>): Optional<S> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?> exists(example: Example<S>): Boolean {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?, R : Any?> findBy(
        example: Example<S>,
        queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>,
    ): R & Any {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?> insert(entity: S & Any): S & Any {
        TODO("Not yet implemented")
    }

    override fun <S : SkillStatsDocument?> insert(entities: MutableIterable<S>): MutableList<S> {
        TODO("Not yet implemented")
    }
}
