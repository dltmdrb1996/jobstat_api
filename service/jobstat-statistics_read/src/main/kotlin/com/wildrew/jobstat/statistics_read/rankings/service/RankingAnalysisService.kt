package com.wildrew.jobstat.statistics_read.rankings.service

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.BaseRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.rankings.model.RakingWithStatsPage
import com.wildrew.jobstat.statistics_read.rankings.model.RankingData
import com.wildrew.jobstat.statistics_read.rankings.model.RankingPage
import com.wildrew.jobstat.statistics_read.rankings.model.RankingWithStats
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.toStatsType
import com.wildrew.jobstat.statistics_read.rankings.repository.RankingRepositoryRegistry
import com.wildrew.jobstat.statistics_read.stats.service.StatsAnalysisService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface RankingAnalysisService {
    fun findRankingPage(
        rankingType: RankingType,
        baseDate: BaseDate,
        page: Int? = null,
    ): RankingPage

    fun findTopNRankings(
        rankingType: RankingType,
        baseDate: BaseDate,
        limit: Int,
    ): List<RankingEntry>

    fun findRankingMovements(
        rankingType: RankingType,
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<RankingEntry>

    fun findConsistentRankings(
        rankingType: RankingType,
        months: Int,
        maxRank: Int,
    ): List<RankingEntry>

    fun findRankRange(
        rankingType: RankingType,
        baseDate: BaseDate,
        startRank: Int,
        endRank: Int,
    ): List<RankingEntry>

    fun findTopLosers(
        rankingType: RankingType,
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<RankingEntry>

    fun findVolatileRankings(
        rankingType: RankingType,
        months: Int,
        minRankChange: Int,
    ): List<RankingEntry>

    fun <T : BaseStatsDocument> findStatsWithRanking(
        rankingType: RankingType,
        baseDate: BaseDate,
        page: Int?,
    ): RakingWithStatsPage<T>
}

@Service
class RankingAnalysisServiceImpl(
    private val repositoryRegistry: RankingRepositoryRegistry,
    private val statsService: StatsAnalysisService,
) : RankingAnalysisService {
    companion object {
        private const val CURSOR_KEY_PREFIX = "ranking:cursor"
        private const val PAGE_SIZE = 100
    }

    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun <T : BaseStatsDocument> findStatsWithRanking(
        rankingType: RankingType,
        baseDate: BaseDate,
        page: Int?,
    ): RakingWithStatsPage<T> {
        val statsType = rankingType.toStatsType()
        val rankingPage = findRankingPage(rankingType, baseDate, page)
        val rankings = rankingPage.items.data
        val ids = rankings.map { it.entityId }

        // stats를 Map으로 변환하여 빠른 검색이 가능하도록 함
        val statsMap =
            statsService
                .findStatsByEntityIdsAndBaseDate<T>(statsType, baseDate, ids)

        // rankings의 순서를 유지하면서 매칭되는 stats를 찾아 결합
        val rankingWithStats =
            rankings.map { ranking ->
                RankingWithStats(
                    ranking = ranking,
                    stat = statsMap[ranking.entityId] ?: throw AppException.fromErrorCode(ErrorCode.RESOURCE_NOT_FOUND),
                )
            }

        return RakingWithStatsPage(
            items = rankingWithStats,
            rankingPage.rankedCount,
            rankingPage.hasNextPage,
        )
    }

    override fun findRankingPage(
        rankingType: RankingType,
        baseDate: BaseDate,
        page: Int?,
    ): RankingPage {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val doc = repository.findByPage(baseDate.toString(), page ?: 1)
        val rankings = doc.rankings
        val hasNextPage = rankings.size >= PAGE_SIZE
        return RankingPage(
            items =
                RankingData(
                    type = rankingType,
                    data = rankings,
                ),
            doc.metrics.rankedCount,
            hasNextPage = hasNextPage,
        )
    }

    override fun findTopNRankings(
        rankingType: RankingType,
        baseDate: BaseDate,
        limit: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findTopN(baseDate.toString(), limit)
        return rankings
    }

    override fun findRankingMovements(
        rankingType: RankingType,
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findTopMovers(startDate.toString(), endDate.toString(), limit)
        return rankings
    }

    override fun findConsistentRankings(
        rankingType: RankingType,
        months: Int,
        maxRank: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findEntitiesWithConsistentRanking(months, maxRank)
        return rankings
    }

    override fun findRankRange(
        rankingType: RankingType,
        baseDate: BaseDate,
        startRank: Int,
        endRank: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findByRankRange(baseDate.toString(), startRank, endRank)
        return rankings
    }

    override fun findTopLosers(
        rankingType: RankingType,
        startDate: BaseDate,
        endDate: BaseDate,
        limit: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findTopLosers(startDate.toString(), endDate.toString(), limit)
        return rankings
    }

    override fun findVolatileRankings(
        rankingType: RankingType,
        months: Int,
        minRankChange: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findVolatileEntities(months, minRankChange)
        return rankings
    }
}
