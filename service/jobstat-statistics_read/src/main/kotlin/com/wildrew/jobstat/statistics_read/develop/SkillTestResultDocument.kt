package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_model.DocumentStatus
import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseReferenceDocument
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "skill_test_results")
class SkillTestResultDocument(
    id: String? = null,
    referenceId: Long,
    @Field("skill_id")
    val skillId: Long,
    @Field("test_type")
    val testType: String,
    @Field("avg_score")
    val avgScore: Double,
    @Field("participant_count")
    val participantCount: Int,
    @Field("difficulty_distribution")
    val difficultyDistribution: Map<String, Double>,
    @Field("score_percentiles")
    val scorePercentiles: Map<Int, Double>,
) : BaseReferenceDocument(
        id,
        referenceId,
        EntityType.SKILL,
        DocumentStatus.ACTIVE,
    ) {
    override fun validate() {
        require(testType.isNotBlank()) { "Test type must not be blank" }
        require(avgScore >= 0) { "Average score must be greater than or equal to 0" }
        require(participantCount >= 0) { "Participant count must be greater than or equal to 0" }
        require(difficultyDistribution.isNotEmpty()) { "Difficulty distribution must not be empty" }
        require(scorePercentiles.isNotEmpty()) { "Score percentiles must not be empty" }
    }
}
