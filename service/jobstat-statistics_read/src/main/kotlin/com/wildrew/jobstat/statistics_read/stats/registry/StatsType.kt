package com.wildrew.jobstat.statistics_read.stats.registry

import com.wildrew.jobstat.statistics_read.stats.document.*

enum class StatsType {
    BENEFIT,
    CERTIFICATION,
    COMPANY,
    COMPANY_SIZE,
    CONTRACT_TYPE,
    EDUCATION,
    EXPERIENCE,
    INDUSTRY,
    JOB_CATEGORY,
    LOCATION,
    REMOTE_WORK,
    REMOTE_WORK_TYPE,
    SKILL,
    ;

    val collectionPrefix: String get() = name.lowercase()
}
