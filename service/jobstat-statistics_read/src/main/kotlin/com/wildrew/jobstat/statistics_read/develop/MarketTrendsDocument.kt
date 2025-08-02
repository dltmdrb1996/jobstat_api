package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.SnapshotPeriod
import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseDocument
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "market_trends_monthly")
class MarketTrendsDocument(
    id: String? = null,
    @Field("base_date")
    val baseDate: String,
    @Field("period")
    val period: SnapshotPeriod,
    @Field("emerging_technologies")
    val emergingTechnologies: List<EmergingTechnology>,
    @Field("year_over_year_growth")
    val yearOverYearGrowth: Double,
) : BaseDocument(id) {
    override fun validate() {
        require(emergingTechnologies.isNotEmpty()) { "Emerging technologies must not be empty" }
    }

    data class EmergingTechnology(
        @Field("category")
        val category: String,
        @Field("growth_rate")
        val growthRate: Double,
        @Field("avg_salary_growth")
        val avgSalaryGrowth: Double,
        @Field("top_skills")
        val topSkills: List<EmergingSkill>,
    )

    data class EmergingSkill(
        @Field("skill_id")
        val skillId: Long,
        @Field("name")
        val name: String,
        @Field("growth_rate")
        val growthRate: Double,
    )
}
