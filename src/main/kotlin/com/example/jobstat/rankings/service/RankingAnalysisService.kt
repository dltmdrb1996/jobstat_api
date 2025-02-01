package com.example.jobstat.rankings.service

import com.example.jobstat.core.base.mongo.ranking.*
import com.example.jobstat.core.state.BaseDate
import com.example.jobstat.rankings.model.RankingData
import com.example.jobstat.rankings.model.RankingPage
import com.example.jobstat.rankings.repository.RankingRepositoryRegistry
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

interface RankingAnalysisService {
    fun findRankingPage(
        rankingType: RankingType,
        baseDate: BaseDate,
        lastRank: Int? = null,
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
    private val redisTemplate: StringRedisTemplate,
) : RankingAnalysisService {
    companion object {
        private const val CURSOR_KEY_PREFIX = "ranking:cursor"
        private const val PAGE_SIZE = 100
    }

    private val logger = LoggerFactory.getLogger(RankingAnalysisServiceImpl::class.java)

    @Suppress("UNCHECKED_CAST")
    override fun findRankingPage(
        rankingType: RankingType,
        baseDate: BaseDate,
        page: Int?,
    ): RankingPage {
        val page = page ?: 1
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val doc = repository.findByPage(baseDate.toString(), page)
        val rankings = doc.rankings

        val hasNextPage = rankings.size > PAGE_SIZE
        return RankingPage(
            items =
                RankingData(
                    type = rankingType,
                    data = rankings,
                ),
            hasNextPage = hasNextPage,
            nextCursor = page + 1,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun findTopNRankings(
        rankingType: RankingType,
        baseDate: BaseDate,
        limit: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findTopN(baseDate.toString(), limit)
        return rankings
    }

    @Suppress("UNCHECKED_CAST")
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

    @Suppress("UNCHECKED_CAST")
    override fun findConsistentRankings(
        rankingType: RankingType,
        months: Int,
        maxRank: Int,
    ): List<RankingEntry> {
        val repository = repositoryRegistry.getRepository<BaseRankingDocument<*>>(rankingType)
        val rankings = repository.findEntitiesWithConsistentRanking(months, maxRank)
        return rankings
    }

    @Suppress("UNCHECKED_CAST")
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

    @Suppress("UNCHECKED_CAST")
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

    @Suppress("UNCHECKED_CAST")
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
