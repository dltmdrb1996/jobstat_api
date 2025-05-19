package com.example.jobstat.core.core_model

enum class EntityType(
    val code: String,
    val tableName: String?,
) {
    SKILL("SKILL", "skills"),
    INDUSTRY("IND", "industries"),
    JOB_CATEGORY("JOB", "job_categories"),
    LOCATION("LOC", "locations"),
    COMPANY("COMP", "companies"),
    CERTIFICATION("CERT", "certifications"),
    EDUCATION("EDU", "education_levels"),
    EXPERIENCE("EXP", "experience_levels"),
    BENEFIT("BNF", "benefits"),
    COMPANY_SIZE("CSZ", null),
    SALARY("SAL", null),
    POSTING_COUNT("PST", null),
    ;

    companion object {
        fun fromCode(code: String) =
            values().find { it.code == code }
                ?: throw IllegalArgumentException("유효하지 않은 엔티티 타입 코드입니다: $code")
    }
}
