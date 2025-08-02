package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseReferenceDocument
import com.wildrew.jobstat.statistics_read.core.core_model.DocumentStatus
import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "interview_question_db")
class InterviewQuestionDocument(
    id: String? = null,
    referenceId: Long,
    entityType: EntityType = EntityType.JOB_CATEGORY,
    status: DocumentStatus = DocumentStatus.ACTIVE,
    @Field("job_category_id")
    val jobCategoryId: Long,
    @Field("questions")
    val questions: List<InterviewQuestion>,
    @Field("category")
    val category: String,
) : BaseReferenceDocument(
        id,
        referenceId,
        entityType,
        status,
    ) {
    override fun validate() {
        require(questions.isNotEmpty()) { "Questions must not be empty" }
    }

    fun copy(
        id: String? = this.id,
        referenceId: Long = this.referenceId,
        entityType: EntityType = this.entityType,
        status: DocumentStatus = this.status,
        jobCategoryId: Long = this.jobCategoryId,
        questions: List<InterviewQuestion> = this.questions,
        category: String = this.category,
    ): InterviewQuestionDocument =
        InterviewQuestionDocument(
            id = id,
            referenceId = referenceId,
            entityType = entityType,
            status = status,
            jobCategoryId = jobCategoryId,
            questions = questions,
            category = category,
        )

    data class InterviewQuestion(
        @Field("question")
        val question: String,
        @Field("tips")
        val tips: String,
        @Field("difficulty")
        val difficulty: String,
        @Field("skills_tested")
        val skillsTested: List<String>,
    )
}
