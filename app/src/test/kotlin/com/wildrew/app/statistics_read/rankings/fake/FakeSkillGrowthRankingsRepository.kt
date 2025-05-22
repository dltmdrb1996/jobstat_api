package com.wildrew.app.statistics_read.rankings.fake

import com.wildrew.jobstat.statistics_read.fake.AbstractFakeSimpleRankingRepository
import com.wildrew.jobstat.statistics_read.rankings.document.SkillGrowthRankingsDocument
import com.wildrew.jobstat.statistics_read.rankings.repository.SkillGrowthRankingsRepository
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.*
import java.util.function.Function
import kotlin.math.abs

class FakeSkillGrowthRankingsRepository :
    AbstractFakeSimpleRankingRepository<SkillGrowthRankingsDocument, SkillGrowthRankingsDocument.SkillGrowthRankingEntry>(),
    SkillGrowthRankingsRepository {
    override fun findByPage(
        baseDate: String,
        page: Int,
    ): SkillGrowthRankingsDocument =
        documents.values
            .find { it.baseDate == baseDate }
            ?: throw NoSuchElementException("Document not found for baseDate: $baseDate, page: $page")

    override fun findTopMovers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> {
        if (startDate > endDate) return emptyList()
        return documents.values
            .filter { it.baseDate == endDate }
            .flatMap { it.rankings }
            .filter { it.rankChange != null && (it.rankChange ?: 0) > 0 }
            .sortedByDescending { it.rankChange }
            .take(limit)
    }

    override fun findByRankRange(
        baseDate: String,
        startRank: Int,
        endRank: Int,
    ): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> {
        val (minRank, maxRank) =
            if (startRank <= endRank) {
                startRank to endRank
            } else {
                endRank to startRank
            }

        val results =
            documents.values
                .filter { it.baseDate == baseDate }
                .flatMap { it.rankings }
                .filter { it.rank in minRank..maxRank }

        return if (startRank <= endRank) {
            results.sortedBy { it.rank }
        } else {
            results.sortedByDescending { it.rank }
        }
    }

    override fun findEntitiesWithConsistentRanking(
        months: Int,
        maxRank: Int,
    ): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> {
        if (maxRank <= 0) return emptyList()

        val dates =
            documents.values
                .map { it.baseDate }
                .distinct()
                .sortedDescending()
                .take(months)

        val entityRanks = mutableMapOf<Long, List<Int>>()

        // 각 엔티티별로 순위 변화를 추적
        dates.forEach { date ->
            documents.values
                .filter { it.baseDate == date }
                .forEach { doc ->
                    doc.rankings.forEach { entry ->
                        val ranks = entityRanks.getOrDefault(entry.entityId, emptyList())
                        entityRanks[entry.entityId] = ranks + entry.rank
                    }
                }
        }

        // 일관된 순위를 가진 엔티티 필터링
        val consistentEntityIds =
            entityRanks
                .filter { (_, ranks) ->
                    ranks.size == months &&
                        // 모든 기간에 데이터가 있고
                        ranks.all { it <= maxRank } &&
                        // 모든 순위가 maxRank 이하이며
                        ranks.toSet().size == 1 // 순위가 변하지 않았음
                }.keys

        // 최신 순위 데이터 반환
        return documents.values
            .filter { it.baseDate == dates.first() }
            .flatMap { it.rankings }
            .filter { it.entityId in consistentEntityIds }
            .sortedBy { it.rank }
    }

    override fun findVolatileEntities(
        months: Int,
        minRankChange: Int,
    ): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> {
        val dates =
            documents.values
                .map { it.baseDate }
                .distinct()
                .sortedDescending()
                .take(months)

        return documents.values
            .filter { it.baseDate == dates.first() } // 최신 데이터만 사용
            .flatMap { it.rankings }
            .filter { entry ->
                entry.rankChange?.let { change ->
                    abs(change) >= minRankChange
                } ?: false
            }.sortedByDescending { abs(it.rankChange ?: 0) }
    }

    override fun findTopLosers(
        startDate: String,
        endDate: String,
        limit: Int,
    ): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> {
        if (startDate > endDate) return emptyList()

        return documents.values
            .filter { it.baseDate == endDate }
            .flatMap { it.rankings }
            .filter { it.rankChange != null && (it.rankChange ?: 0) < 0 }
            .sortedBy { it.rankChange } // 음수값이므로 오름차순 정렬하면 가장 큰 하락폭 순
            .take(limit)
    }

    override fun findTopN(
        baseDate: String,
        limit: Int,
    ): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> =
        documents.values
            .filter { it.baseDate == baseDate }
            .flatMap { it.rankings }
            .sortedBy { it.rank }
            .take(limit)

    override fun findByGrowthConsistency(
        baseDate: String,
        minConsistency: Double,
        minGrowthRate: Double,
    ): List<SkillGrowthRankingsDocument> =
        documents.values
            .filter { it.baseDate == baseDate }
            .filter { doc ->
                doc.rankings.any { entry ->
                    entry.growthConsistency >= minConsistency && entry.growthRate >= minGrowthRate
                }
            }

    override fun findByMarketPenetration(
        baseDate: String,
        minPenetration: Double,
    ): List<SkillGrowthRankingsDocument> =
        documents.values
            .filter { it.baseDate == baseDate }
            .filter { doc ->
                doc.rankings.any { entry ->
                    entry.growthFactors.marketPenetration >= minPenetration
                }
            }

    override fun findByMultiFactorGrowth(
        baseDate: String,
        minDemandGrowth: Double,
        minSalaryGrowth: Double,
    ): List<SkillGrowthRankingsDocument> =
        documents.values
            .filter { it.baseDate == baseDate }
            .filter { doc ->
                doc.rankings.any { entry ->
                    entry.growthFactors.demandGrowth >= minDemandGrowth &&
                        entry.growthFactors.salaryGrowth >= minSalaryGrowth
                }
            }

    override fun findStableEntities(
        months: Int,
        maxRankChange: Int,
    ): List<SkillGrowthRankingsDocument.SkillGrowthRankingEntry> {
        val dates =
            documents.values
                .map { it.baseDate }
                .distinct()
                .sortedDescending()
                .take(months)

        return documents.values
            .filter { it.baseDate in dates }
            .flatMap { it.rankings }
            .filter { entry ->
                entry.rankChange?.let { change ->
                    abs(change) <= maxRankChange
                } ?: false
            }.sortedBy { abs(it.rankChange ?: 0) }
    }

    override fun <S : SkillGrowthRankingsDocument?> findAll(example: Example<S>): MutableList<S> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?> findAll(
        example: Example<S>,
        sort: Sort,
    ): MutableList<S> {
        TODO("Not yet implemented")
    }

    override fun findAll(sort: Sort): MutableList<SkillGrowthRankingsDocument> {
        TODO("Not yet implemented")
    }

    override fun findAll(pageable: Pageable): Page<SkillGrowthRankingsDocument> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?> findAll(
        example: Example<S>,
        pageable: Pageable,
    ): Page<S> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?> count(example: Example<S>): Long {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?> findOne(example: Example<S>): Optional<S> {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?> exists(example: Example<S>): Boolean {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?, R : Any?> findBy(
        example: Example<S>,
        queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>,
    ): R & Any {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?> insert(entity: S & Any): S & Any {
        TODO("Not yet implemented")
    }

    override fun <S : SkillGrowthRankingsDocument?> insert(entities: MutableIterable<S>): MutableList<S> {
        TODO("Not yet implemented")
    }

    companion object {
        private const val PAGE_SIZE = 100
    }
}

/**
 * 테스트 도우미 함수들
 */
private fun <T> List<T>.hasSameElements(other: List<T>): Boolean {
    if (this.size != other.size) return false
    return this.toSet() == other.toSet()
}

private fun SkillGrowthRankingsDocument.SkillGrowthRankingEntry.isConsistent(): Boolean = this.rankChange != null && abs(this.rankChange ?: 0) <= 2
