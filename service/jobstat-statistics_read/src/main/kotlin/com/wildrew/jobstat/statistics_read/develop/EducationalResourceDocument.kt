package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseReferenceDocument
import com.wildrew.jobstat.statistics_read.core.core_model.DocumentStatus
import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "education_resources_db")
class EducationalResourceDocument(
    id: String? = null,
    referenceId: Long,
    @Field("title")
    val title: String,
    @Field("description")
    val description: String,
    @Field("resource_type")
    val resourceType: String,
    @Field("skill_categories")
    val skillCategories: List<String>,
    @Field("difficulty_level")
    val difficultyLevel: String,
    @Field("url")
    val url: String?,
) : BaseReferenceDocument(
        id,
        referenceId,
        EntityType.EDUCATION,
        DocumentStatus.ACTIVE,
    ) {
    override fun validate() {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(description.isNotBlank()) { "Description must not be blank" }
        require(resourceType.isNotBlank()) { "Resource type must not be blank" }
        require(skillCategories.isNotEmpty()) { "Skill categories must not be empty" }
        require(difficultyLevel.isNotBlank()) { "Difficulty level must not be blank" }
    }
}
