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
    JsonSubTypes.Type(value = PostingCountScore::class, name = "게시물_수_점수"),
    JsonSubTypes.Type(value = SalaryScore::class, name = "급여_점수"),
    JsonSubTypes.Type(value = GrowthScore::class, name = "성장_점수"),
    JsonSubTypes.Type(value = DemandScore::class, name = "수요_점수"),
    JsonSubTypes.Type(value = MonthlyChangeScore::class, name = "월간_변화_점수"),
    JsonSubTypes.Type(value = CompanyGrowthScore::class, name = "기업_성장_점수"),
    JsonSubTypes.Type(value = CompanyHiringScore::class, name = "기업_채용_점수"),
    JsonSubTypes.Type(value = CompanyWorkLifeBalanceScore::class, name = "기업_워라밸_점수"),
    JsonSubTypes.Type(value = EntryLevelFriendlinessScore::class, name = "신입_친화도_점수"),
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
    val consistencyScore: Double?,
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
