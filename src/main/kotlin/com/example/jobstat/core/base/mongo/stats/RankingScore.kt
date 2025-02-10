package com.example.jobstat.core.base.mongo.stats

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "score_type", // 타입 정보를 저장할 필드
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PostingCountScore::class, name = "posting_count_score"),
    JsonSubTypes.Type(value = SalaryScore::class, name = "salary_score"),
    JsonSubTypes.Type(value = GrowthScore::class, name = "growth_score"),
    JsonSubTypes.Type(value = DemandScore::class, name = "demand_score"),
    JsonSubTypes.Type(value = MonthlyChangeScore::class, name = "monthly_change_score"),
    JsonSubTypes.Type(value = CompanyGrowthScore::class, name = "company_growth_score"),
    JsonSubTypes.Type(value = CompanyHiringScore::class, name = "company_hiring_score"),
    JsonSubTypes.Type(value = CompanyWorkLifeBalanceScore::class, name = "company_work_life_balance_score"),
    JsonSubTypes.Type(value = EntryLevelFriendlinessScore::class, name = "entry_level_friendliness_score"),
)
interface RankingScore : Serializable {
    val value: Double
}

data class PostingCountScore(
    @Field("value")
    override val value: Double,
    @Field("total_postings")
    val totalPostings: Int,
    @Field("active_postings")
    val activePostings: Int,
) : RankingScore

data class SalaryScore(
    @Field("value")
    override val value: Double,
    @Field("avg_salary")
    val avgSalary: Long,
    @Field("median_salary")
    val medianSalary: Long?,
) : RankingScore

data class GrowthScore(
    @Field("value")
    override val value: Double,
    @Field("growth_rate")
    val growthRate: Double,
    @Field("consistency_score")
    val consistencyScore: Double?, // 성장의 일관성 점수
) : RankingScore

data class DemandScore(
    @Field("value")
    override val value: Double,
    @Field("application_rate")
    val applicationRate: Double?,
    @Field("market_demand")
    val marketDemand: Double?,
) : RankingScore

data class MonthlyChangeScore(
    @Field("value")
    override val value: Double,
    @Field("change_rate")
    val changeRate: Double,
    @Field("absolute_change")
    val absoluteChange: Double,
) : RankingScore

data class CompanyGrowthScore(
    @Field("value")
    override val value: Double,
    @Field("revenue_growth")
    val revenueGrowth: Double,
    @Field("employee_growth")
    val employeeGrowth: Double,
    @Field("market_share_growth")
    val marketShareGrowth: Double,
) : RankingScore

data class CompanyHiringScore(
    @Field("value")
    override val value: Double,
    @Field("hiring_volume")
    val hiringVolume: Int,
    @Field("hiring_growth_rate")
    val hiringGrowthRate: Double,
    @Field("retention_rate")
    val retentionRate: Double,
) : RankingScore

data class CompanyWorkLifeBalanceScore(
    @Field("value")
    override val value: Double,
    @Field("satisfaction_rate")
    val satisfactionRate: Double,
    @Field("work_hours_flexibility")
    val workHoursFlexibility: Double,
    @Field("benefits_satisfaction")
    val benefitsSatisfaction: Double,
    @Field("remote_work_score")
    val remoteWorkScore: Double,
) : RankingScore

data class EntryLevelFriendlinessScore(
    @Field("value")
    override val value: Double,
    @Field("entry_level_hiring_rate")
    val entryLevelHiringRate: Double,
    @Field("training_program_quality")
    val trainingProgramQuality: Double,
    @Field("mentorship_availability")
    val mentorshipAvailability: Double,
) : RankingScore
