package com.wildrew.jobstat.statistics_read.core.core_model

enum class EntityType(
    val code: String,
    val tableName: String?,
) {
    JOB_CATEGORY("JOB", "job_categories"),
    INDUSTRY("IND", "industries"),
    SKILL("SKILL", "skills"),

    COMPANY("COMP", "companies"),
    COMPANY_SIZE("CSZ", null),

    LOCATION("LOC", "locations"),

    EDUCATION("EDU", "education_levels"),
    EXPERIENCE("EXP", "experience_levels"),
    CERTIFICATION("CERT", "certifications"),

    BENEFIT("BNF", "benefits"),
    CONTRACT_TYPE("CT", null),
    REMOTE_WORK_TYPE("RWT", null),

    SALARY("SAL", null),
    POSTING_COUNT("PST", null),
    ;

    companion object {
        fun fromCode(code: String) =
            values().find { it.code == code }
                ?: throw IllegalArgumentException("Invalid entity type code: $code")
    }
}
