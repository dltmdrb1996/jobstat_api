package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseReferenceDocument
import com.wildrew.jobstat.statistics_read.core.core_model.DocumentStatus
import com.wildrew.jobstat.statistics_read.core.core_model.EntityType
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant

@Document(collection = "company_reviews")
class CompanyReviewsDocument(
    id: String? = null,
    referenceId: Long,
    status: DocumentStatus = DocumentStatus.ACTIVE,
    @Field("reviews")
    val reviews: List<Review>,
    @Field("average_rating")
    val averageRating: Double,
    @Field("review_count")
    val reviewCount: Int,
) : BaseReferenceDocument(
        id,
        referenceId,
        EntityType.COMPANY,
        status,
    ) {
    override fun validate() {
        require(reviews.isNotEmpty()) { "Reviews must not be empty" }
    }

    data class Review(
        @Field("rating")
        val rating: Double,
        @Field("comment")
        val comment: String,
        @Field("date")
        val date: Instant,
    )
}
