package com.wildrew.jobstat.statistics_read.rankings.service

import com.wildrew.jobstat.core.core_error.model.AppException
import com.wildrew.jobstat.core.core_error.model.ErrorCode
import com.wildrew.jobstat.core.core_global.model.BaseDate
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.BaseRankingDocument
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.ranking.RankingEntry
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.rankings.model.*
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.RankingType
import com.wildrew.jobstat.statistics_read.rankings.model.rankingtype.toStatsType
import com.wildrew.jobstat.statistics_read.rankings.repository.RankingRepositoryRegistry
import com.wildrew.jobstat.statistics_read.stats.service.StatsAnalysisService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

interface RankingAnalysisService {
    fun findRankingOnly(
        rankingType: RankingType,
        baseDate: BaseDate,
        cursor: Int?,
        limit: Int,
    ): PureRankingPage

    fun <T : BaseStatsDocument> findStatsWithRanking(
        rankingType: RankingType,
        baseDate: BaseDate,
        cursor: Int?,
        limit: Int,
    ): RakingWithStatsPage<T>

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
}

@Service
class RankingAnalysisServiceImpl(
    private val repositoryRegistry: RankingRepositoryRegistry,
    private val statsService: StatsAnalysisService,
) : RankingAnalysisService {
    companion object {
        private const val PAGE_SIZE = 100
    }

    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun findRankingOnly(
        rankingType: RankingType,
        baseDate: BaseDate,
        cursor: Int?,
        limit: Int,
    ): PureRankingPage = findPureRankings(rankingType, baseDate, cursor, limit)

    override fun <T : BaseStatsDocument> findStatsWithRanking(
        rankingType: RankingType,
        baseDate: BaseDate,
        cursor: Int?,
        limit: Int,
    ): RakingWithStatsPage<T> {
        val statsType = rankingType.toStatsType()
        val pureRankingPage = findPureRankings(rankingType, baseDate, cursor, limit)

        if (pureRankingPage.items.isEmpty()) {
            return RakingWithStatsPage(emptyList(), 0, false, null)
        }

        val ids = pureRankingPage.items.map { it.entityId }
        val statsMap = statsService.findStatsByEntityIdsAndBaseDate<T>(statsType, baseDate, ids)

        val rankingWithStats =
            pureRankingPage.items.map { ranking ->
                RankingWithStats(
                    ranking = ranking,
                    stat = statsMap[ranking.entityId] ?: throw AppException.fromErrorCode(ErrorCode.RESOURCE_NOT_FOUND),
                )
            }

        return RakingWithStatsPage(
            items = rankingWithStats,
            totalCount = pureRankingPage.totalCount,
            hasNextPage = pureRankingPage.hasNextPage,
            nextCursor = pureRankingPage.nextCursor,
        )
    }

//    private fun findPureRankings(
//        rankingType: RankingType,
//        baseDate: BaseDate,
//        cursor: Int?,
//        limit: Int,
//    ): PureRankingPage {
//        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
//
//        val startRank = cursor?.let { it + 1 } ?: 1
//        val endRank = startRank + limit - 1
//        val startPage = (startRank - 1) / PAGE_SIZE + 1
//        val endPage = (endRank - 1) / PAGE_SIZE + 1
//
//        val rankings =
//            repository
//                .findByPageRange(baseDate.toString(), startPage, endPage)
//                .flatMap { it.rankings }
//                .filter { it.rank in startRank..endRank }
//                .take(limit)
//
//        if (rankings.isEmpty()) {
//            return PureRankingPage(emptyList(), 0, false, null)
//        }
//
//        val lastItem = rankings.last()
//        val totalCount = repository.findByPage(baseDate.toString(), 1).metrics.rankedCount
//        val hasNextPage = lastItem.rank < totalCount
//        val nextCursor = if (hasNextPage) lastItem.rank else null
//
//        return PureRankingPage(
//            items = rankings,
//            totalCount = totalCount,
//            hasNextPage = hasNextPage,
//            nextCursor = nextCursor,
//        )
//    }

    private fun findPureRankings(
        rankingType: RankingType,
        baseDate: BaseDate,
        cursor: Int?,
        limit: Int,
    ): PureRankingPage {
        // 제네릭 타입을 맞추기 위해 캐스팅이 필요합니다.
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)

        // 1. DB에서 직접 슬라이싱된 데이터를 가져옵니다.
        val rankingSlice = repository.findRankingsSlice(baseDate.toString(), cursor, limit)

        val items = rankingSlice.items
        val totalCount = rankingSlice.totalCount

        if (items.isEmpty()) {
            return PureRankingPage(emptyList(), totalCount, false, null)
        }

        val lastItem = items.last()

        val hasNextPage = lastItem.rank < totalCount

        val nextCursor = if (hasNextPage) lastItem.rank else null

        return PureRankingPage(
            items = items,
            totalCount = totalCount,
            hasNextPage = hasNextPage,
            nextCursor = nextCursor,
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
