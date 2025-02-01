package com.example.jobstat.rankings.model

import com.example.jobstat.core.base.mongo.ranking.BaseRankingDocument

data class RankingAnalysis<T : BaseRankingDocument<*>>(
    val documentType: Class<*>,
    val results: List<T>,
    val analysisType: String,
    val metadata: Map<String, Any> = emptyMap(),
)
