package com.example.jobstat.statistics_read.rankings.model

import com.example.jobstat.core.base.mongo.ranking.BaseRankingDocument

data class RankingResult(
    val documentType: Class<*>,
    val data: List<BaseRankingDocument<*>>,
) {
    fun toAnalysis(analysisType: String) =
        RankingAnalysis(
            documentType = documentType,
            results = data,
            analysisType = analysisType,
        )
}
