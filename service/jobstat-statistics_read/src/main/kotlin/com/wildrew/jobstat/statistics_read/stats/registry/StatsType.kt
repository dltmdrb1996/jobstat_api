package com.wildrew.jobstat.statistics_read.stats.registry

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.stats.BaseStatsDocument
import com.wildrew.jobstat.statistics_read.stats.document.*

enum class StatsType {
    BENEFIT,
    CERTIFICATION,
    COMPANY,
    CONTRACT_TYPE,
    EDUCATION,
    EXPERIENCE,
    INDUSTRY,
    JOB_CATEGORY,
    LOCATION,
    REMOTE_WORK,
    SKILL,
    ;

    val collectionPrefix: String get() = name.lowercase()
}
