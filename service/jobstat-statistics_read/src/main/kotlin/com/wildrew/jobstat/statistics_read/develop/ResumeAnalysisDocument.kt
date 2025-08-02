package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_model.DocumentStatus
import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseReferenceDocument
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

@Document(collection = "resume_analysis_results")
class ResumeAnalysisDocument(
    id: String? = null,
    referenceId: Long, // candidate_id
    @Field("analysis_date")
    val analysisDate: Instant,
    @Field("matched_skills")
    val matchedSkills: List<MatchedSkill>,
    @Field("improvement_suggestions")
    val improvementSuggestions: List<String>,
    @Field("matched_job_categories")
    val matchedJobCategories: List<MatchedJobCategory>,
) : BaseReferenceDocument(
        id,
        referenceId,
        EntityType.SKILL,
        DocumentStatus.ACTIVE,
    ) {
    override fun validate() {
        require(matchedSkills.isNotEmpty()) { "Matched skills must not be empty" }
    }

    data class MatchedSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("score")
        val score: Double,
    )

    data class MatchedJobCategory(
        @Field("job_category_id")
        val jobCategoryId: Long,
        @Field("fit_score")
        val fitScore: Double,
    )
}
