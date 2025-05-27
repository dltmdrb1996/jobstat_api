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

fun StatsType.toDocumentClass(): Class<out BaseStatsDocument> =
    when (this) {
        StatsType.BENEFIT -> BenefitStatsDocument::class.java
        StatsType.CERTIFICATION -> CertificationStatsDocument::class.java
        StatsType.COMPANY -> CompanyStatsDocument::class.java
        StatsType.CONTRACT_TYPE -> ContractTypeStatsDocument::class.java
        StatsType.EDUCATION -> EducationStatsDocument::class.java
        StatsType.EXPERIENCE -> ExperienceStatsDocument::class.java
        StatsType.INDUSTRY -> IndustryStatsDocument::class.java
        StatsType.JOB_CATEGORY -> JobCategoryStatsDocument::class.java
        StatsType.LOCATION -> LocationStatsDocument::class.java
        StatsType.REMOTE_WORK -> RemoteWorkStatsDocument::class.java
        StatsType.SKILL -> SkillStatsDocument::class.java
    }
