package com.wildrew.jobstat.statistics_read.core.core_model

enum class EntityType(
    val code: String,
    val tableName: String?,
) {
    // --- 직무/산업/기술 관련 ---
    JOB_CATEGORY("JOB", "job_categories"),
    INDUSTRY("IND", "industries"),
    SKILL("SKILL", "skills"),

    // --- 회사 관련 ---
    COMPANY("COMP", "companies"),
    COMPANY_SIZE("CSZ", null),

    // --- 지역 관련 ---
    LOCATION("LOC", "locations"),

    // --- 지원자/요구사항 관련 ---
    EDUCATION("EDU", "education_levels"),
    EXPERIENCE("EXP", "experience_levels"),
    CERTIFICATION("CERT", "certifications"),

    // --- 근무 조건 관련 ---
    BENEFIT("BNF", "benefits"),
    CONTRACT_TYPE("CT", null),
    REMOTE_WORK_TYPE("RWT", null),

    // --- 기타 지표 ---
    SALARY("SAL", null),
    POSTING_COUNT("PST", null),
    ;

    companion object {
        fun fromCode(code: String) =
            values().find { it.code == code }
                ?: throw IllegalArgumentException("Invalid entity type code: $code")
    }
}

