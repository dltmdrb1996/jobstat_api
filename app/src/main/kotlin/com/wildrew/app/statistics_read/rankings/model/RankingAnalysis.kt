package com.wildrew.app.statistics_read.rankings.model

import com.wildrew.app.statistics_read.core.core_mongo_base.model.ranking.BaseRankingDocument

data class RankingAnalysis<T : BaseRankingDocument<*>>(
    val documentType: Class<*>,
    val results: List<T>,
    val analysisType: String,
    val metadata: Map<String, Any> = emptyMap(),
)
