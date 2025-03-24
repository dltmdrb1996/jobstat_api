# 프로젝트 구성도
![system-architecture-new](https://github.com/user-attachments/assets/6f9a8ac2-ffc7-4b00-91af-1d76b87e629c)

# 프로젝트 구조

```
├── main
│   ├── kotlin
│   │   └── com
│   │       └── example
│   │           └── jobstat
│   │               ├── auth
│   │               │   ├── email
│   │               │   │   ├── entity
│   │               │   │   ├── repository
│   │               │   │   ├── service
│   │               │   │   └── usecase
│   │               │   ├── token
│   │               │   │   ├── service
│   │               │   │   └── usecase
│   │               │   └── user
│   │               │       ├── entity
│   │               │       ├── repository
│   │               │       ├── service
│   │               │       └── usecase
│   │               ├── community
│   │               │   ├── board
│   │               │   │   ├── entity
│   │               │   │   ├── model
│   │               │   │   ├── repository
│   │               │   │   ├── service
│   │               │   │   └── usecase
│   │               │   └── comment
│   │               │       ├── entity
│   │               │       ├── repository
│   │               │       ├── service
│   │               │       └── usecase
│   │               ├── core
│   │               │   ├── base
│   │               │   │   ├── mongo
│   │               │   │   │   ├── ranking
│   │               │   │   │   └── stats
│   │               │   │   └── repository
│   │               │   ├── config
│   │               │   ├── constants
│   │               │   ├── converter
│   │               │   ├── error
│   │               │   ├── extension
│   │               │   ├── security
│   │               │   │   └── annotation
│   │               │   ├── state
│   │               │   ├── usecase
│   │               │   │   └── impl
│   │               │   ├── utils
│   │               │   └── wrapper
│   │               └── statistics
│   │                   ├── rankings
│   │                   │   ├── document
│   │                   │   ├── model
│   │                   │   │   └── rankingtype
│   │                   │   ├── repository
│   │                   │   ├── service
│   │                   │   └── usecase
│   │                   │       └── analyze
│   │                   └── stats
│   │                       ├── document
│   │                       ├── registry
│   │                       ├── repository
│   │                       ├── service
│   │                       └── usecase
│   └── resources
│       └── static
└── test
    └── kotlin
        └── com
            └── example
                └── jobstat
                    ├── auth
                    │   ├── token
                    │   │   └── service
                    │   └── user
                    │       ├── fake
                    │       ├── repository
                    │       ├── service
                    │       └── usecase
                    ├── community
                    │   ├── fake
                    │   │   └── repository
                    │   ├── repository
                    │   ├── service
                    │   └── usecase
                    ├── core
                    │   └── base
                    │       └── repository
                    ├── rankings
                    │   ├── fake
                    │   └── service
                    ├── statistics
                    │   ├── fake
                    │   └── stats
                    │       ├── fake
                    │       └── service
                    └── utils
                        ├── base
                        ├── config
                        └── dummy
```

# 테스트 커버리지
<img width="1553" alt="스크린샷 2025-03-24 오후 8 02 15" src="https://github.com/user-attachments/assets/1cbc9595-77c6-47f6-9185-6534a1ebe4cf" />

# 필수 환경변수

애플리케이션이 정상적으로 구동되기 위해 아래 환경변수들을 설정해야 합니다. 각 변수의 값은 본인의 실제 환경에 맞게 입력해 주세요.

- **DB_PASSWORD**: `<YOUR_DB_PASSWORD>`
- **MONGO_USERNAME**: `<YOUR_MONGO_USERNAME>`
- **MONGO_PASSWORD**: `<YOUR_MONGO_PASSWORD>`
- **SENTRY_DSN**: `<YOUR_SENTRY_DSN>`
- **SENTRY_AUTH_TOKEN**: `<YOUR_SENTRY_AUTH_TOKEN>`
- **JWT_SECRET**: `<YOUR_JWT_SECRET>`
- **DB_HOST**: `<YOUR_DB_HOST>`
- **MONGO_HOST**: `<YOUR_MONGO_HOST>`
- **REDIS_HOST**: `<YOUR_REDIS_HOST>`
- **CORS_ALLOWED_ORIGINS**: `<YOUR_CORS_ALLOWED_ORIGINS>`
- **DB_USERNAME**: `<YOUR_DB_USERNAME>`
- **DDNS_DOMAIN**: `<YOUR_DDNS_DOMAIN>`
- **GMAIL_PASSWORD**: `<YOUR_GMAIL_PASSWORD>`
- **GMAIL_ID**: `<YOUR_GMAIL_ID>`
- **ADMIN_USERNAME**: `<YOUR_ADMIN_USERNAME>`
- **ADMIN_PASSWORD**: `<YOUR_ADMIN_PASSWORD>`
- **REDIS_PASSWORD**: `<YOUR_REDIS_PASSWORD>`
- **REDIS_USERNAME**: `<YOUR_REDIS_USERNAME>`

# Mysql 테이블
![mysql](https://github.com/user-attachments/assets/86c8795f-f0bf-4742-914d-9de97a57457b)

# Mongo 도큐먼트 구조

### Stats 도큐먼트
```mermaid
classDiagram
    %% [1] 기본/공통 클래스 및 인터페이스
    class BaseDocument {
      +id: String?
    }
    class BaseTimeSeriesDocument {
      +baseDate: String
      +period: SnapshotPeriod
    }
    BaseDocument <|-- BaseTimeSeriesDocument<img width="1563" alt="스크린샷 2025-03-24 오후 8 01 33" src="https://github.com/user-attachments/assets/bcfaaee7-8739-4367-9b46-172ec7bba301" />


    class SnapshotPeriod {
      +startDate: Instant
      +endDate: Instant
      +durationInDays: Long
    }

    class RankingInfo {
      <<interface>>
      +currentRank: int
      +previousRank: Integer?
      +rankChange: Integer?
      +percentile: Double?
      +rankingScore: RankingScore
    }
    class RankingScore {
      <<interface>>
      +value: double
    }
    class PostingCountScore {
      +value: double
      +totalPostings: int
      +activePostings: int
    }
    RankingScore <|-- PostingCountScore

    class BaseStats {
      <<interface>>
      +postingCount: int
      +activePostingCount: int
      +avgSalary: long
      +growthRate: double
      +yearOverYearGrowth: Double?
      +monthOverMonthChange: Double?
      +demandTrend: String
    }
    class CommonStats {
      +postingCount: int
      +activePostingCount: int
      +avgSalary: long
      +growthRate: double
      +yearOverYearGrowth: Double?
      +monthOverMonthChange: Double?
      +demandTrend: String
    }
    BaseStats <|-- CommonStats

    class Distribution {
      <<interface>>
      +count: int
      +ratio: double
      +avgSalary: Long?
    }
    class CommonDistribution {
      +count: int
      +ratio: double
      +avgSalary: Long?
    }
    Distribution <|-- CommonDistribution

    %% [3] 통계(Stats) 도큐먼트 베이스 및 기존 구현
    class BaseStatsDocument {
      +entityId: Long
      +stats: BaseStats
      +rankings: Map~RankingType,RankingInfo~
    }
    BaseTimeSeriesDocument <|-- BaseStatsDocument

    class BenefitStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +name: String
      +stats: BenefitStats
      +industryDistribution: List~BenefitIndustry~
      +jobCategoryDistribution: List~BenefitJobCategory~
      +companySizeDistribution: List~BenefitCompanySize~
      +locationDistribution: List~BenefitLocation~
      +experienceDistribution: List~BenefitExperience~
      +compensationImpact: BenefitCompensationImpact
      +employeeSatisfaction: BenefitSatisfactionMetrics
      +costMetrics: BenefitCostMetrics
      +rankings: Map~RankingType,BenefitRankingInfo~
    }
    BaseStatsDocument <|-- BenefitStatsDocument

    class CertificationStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +name: String
      +stats: CertificationStats
      +jobCategoryDistribution: List~CertificationJobCategory~
      +industryDistribution: List~CertificationIndustry~
      +skillCorrelations: List~CertificationSkill~
      +experienceDistribution: List~CertificationExperience~
      +companyDistribution: List~CertificationCompany~
      +locationDistribution: List~CertificationLocation~
      +examMetrics: CertificationExamMetrics
      +careerImpact: CertificationCareerImpact
      +investmentMetrics: CertificationInvestmentMetrics
      +rankings: Map~RankingType,CertificationRankingInfo~
      +type: String
    }
    BaseStatsDocument <|-- CertificationStatsDocument

    class CompanyStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +name: String
      +stats: CompanyStats
      +size: CompanySize
      +industryId: Long
      +jobCategories: List~CompanyJobCategory~
      +skills: List~CompanySkill~
      +benefits: List~CompanyBenefit~
      +experienceDistribution: List~CompanyExperienceDistribution~
      +educationDistribution: List~CompanyEducationDistribution~
      +locationDistribution: List~CompanyLocationDistribution~
      +hiringTrends: CompanyHiringTrends
      +remoteWorkRatio: double
      +contractTypeDistribution: List~ContractTypeDistribution~
      +employeeSatisfaction: CompanyEmployeeSatisfaction
      +rankings: Map~RankingType,CompanyRankingInfo~
    }
    BaseStatsDocument <|-- CompanyStatsDocument

    class ContractTypeStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +type: String
      +stats: ContractTypeStats
      +industryDistribution: List~ContractTypeIndustry~
      +jobCategoryDistribution: List~ContractTypeJobCategory~
      +companySizeDistribution: List~ContractTypeCompanySize~
      +locationDistribution: List~ContractTypeLocation~
      +experienceDistribution: List~ContractTypeExperience~
      +skillDistribution: List~ContractTypeSkill~
      +compensationMetrics: ContractTypeCompensation
      +employmentMetrics: ContractTypeEmployment
      +conversionMetrics: ContractTypeConversion
      +rankings: Map~RankingType,ContractTypeRankingInfo~
    }
    BaseStatsDocument <|-- ContractTypeStatsDocument

    class EducationStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +level: String
      +stats: EducationStats
      +industryDistribution: List~EducationIndustry~
      +jobCategoryDistribution: List~EducationJobCategory~
      +companySizeDistribution: List~EducationCompanySize~
      +locationDistribution: List~EducationLocation~
      +skillRequirements: List~EducationSkill~
      +careerMetrics: EducationCareerMetrics
      +roiMetrics: EducationRoiMetrics
      +marketDemand: EducationMarketDemand
      +rankings: Map~RankingType,EducationRankingInfo~
    }
    BaseStatsDocument <|-- EducationStatsDocument

    %% [4] 추가 통계(Stats) 구현 도큐먼트
    class ExperienceStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +range: String
      +stats: ExperienceStats
      +industryDistribution: List~ExperienceIndustry~
      +jobCategoryDistribution: List~ExperienceJobCategory~
      +companySizeDistribution: List~ExperienceCompanySize~
      +locationDistribution: List~ExperienceLocation~
      +skillRequirements: List~ExperienceSkill~
      +careerProgression: ExperienceCareerProgression
      +salaryMetrics: ExperienceSalaryMetrics
      +marketValue: ExperienceMarketValue
      +employmentTypeDistribution: List~ExperienceEmploymentType~
      +rankings: Map~RankingType,ExperienceRankingInfo~
    }
    BaseStatsDocument <|-- ExperienceStatsDocument

    class LocationStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +name: String
      +stats: LocationStats
      +industryDistribution: List~LocationIndustry~
      +jobCategoryDistribution: List~LocationJobCategory~
      +companyDistribution: List~LocationCompany~
      +skillDistribution: List~LocationSkill~
      +experienceDistribution: List~LocationExperience~
      +educationDistribution: List~LocationEducation~
      +employmentMetrics: LocationEmploymentMetrics
      +livingMetrics: LocationLivingMetrics
      +remoteWorkStats: LocationRemoteWorkStats
      +rankings: Map~RankingType,LocationRankingInfo~
    }
    BaseStatsDocument <|-- LocationStatsDocument

    class RemoteWorkStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +type: String
      +stats: RemoteWorkStats
      +industryDistribution: List~RemoteWorkIndustry~
      +jobCategoryDistribution: List~RemoteWorkJobCategory~
      +companySizeDistribution: List~RemoteWorkCompanySize~
      +locationDistribution: List~RemoteWorkLocation~
      +skillDistribution: List~RemoteWorkSkill~
      +experienceDistribution: List~RemoteWorkExperience~
      +productivityMetrics: RemoteWorkProductivity
      +collaborationMetrics: RemoteWorkCollaboration
      +infrastructureMetrics: RemoteWorkInfrastructure
      +satisfactionMetrics: RemoteWorkSatisfaction
      +rankings: Map~RankingType,RemoteWorkRankingInfo~
    }
    BaseStatsDocument <|-- RemoteWorkStatsDocument

    class IndustryStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +name: String
      +stats: IndustryStats
      +jobCategories: List~IndustryJobCategory~
      +skills: List~IndustrySkill~
      +companies: List~IndustryCompany~
      +experienceDistribution: List~ExperienceDistribution~
      +educationDistribution: List~EducationDistribution~
      +locationDistribution: List~LocationDistribution~
      +salaryRangeDistribution: List~SalaryRangeDistribution~
      +companySizeDistribution: List~CompanySizeDistribution~
      +remoteWorkRatio: Double
      +contractTypeDistribution: List~ContractTypeDistribution~
      +rankings: Map~RankingType,IndustryRankingInfo~
    }
    BaseStatsDocument <|-- IndustryStatsDocument

    class JobCategoryStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +name: String
      +stats: JobCategoryStats
      +skills: List~JobCategorySkill~
      +certifications: List~JobCategoryCertification~
      +experienceDistribution: List~ExperienceDistribution~
      +educationDistribution: List~EducationDistribution~
      +salaryRangeDistribution: List~SalaryRangeDistribution~
      +companySizeDistribution: List~CompanySizeDistribution~
      +industryDistribution: List~IndustryDistribution~
      +locationDistribution: List~LocationDistribution~
      +benefitsDistribution: List~BenefitDistribution~
      +remoteWorkRatio: Double
      +contractTypeDistribution: List~ContractTypeDistribution~
      +transitionPaths: List~CareerTransitionPath~
      +rankings: Map~RankingType,JobCategoryRankingInfo~
    }
    BaseStatsDocument <|-- JobCategoryStatsDocument

    class SkillStatsDocument {
      +entityId: Long
      +baseDate: String
      +period: SnapshotPeriod
      +name: String
      +stats: SkillStats
      +experienceLevels: List~SkillExperienceLevel~
      +companySizeDistribution: List~CompanySizeDistribution~
      +industryDistribution: List~IndustryDistribution~
      +isSoftSkill: Boolean
      +isEmergingSkill: Boolean
      +relatedJobCategories: List~RelatedJobCategory~
      +rankings: Map~RankingType,SkillRankingInfo~
    }
    BaseStatsDocument <|-- SkillStatsDocument
```

### Ranking 도큐먼트
```mermaid
%%{init: {'theme':'default', 'flowchart': {'curve': 'basis', 'nodeSpacing': 40, 'rankSpacing': 30}}}%%
classDiagram
    direction TB

    %% 공통 기본/공통 클래스
    class BaseDocument {
      +id: String?
    }
    class BaseTimeSeriesDocument {
      +baseDate: String
      +period: SnapshotPeriod
    }
    BaseDocument <|-- BaseTimeSeriesDocument

    class SnapshotPeriod {
      +startDate: Instant
      +endDate: Instant
      +durationInDays: Long
    }

    %% Ranking 관련 인터페이스/클래스
    class RankingEntry {
      <<interface>>
      +entityId: Long
      +name: String
      +rank: int
      +previousRank: Integer?
      +rankChange: Integer?
    }
    class RankingMetrics {
      <<interface>>
      +totalCount: int
      +rankedCount: int
      +newEntries: int
      +droppedEntries: int
      +volatilityMetrics: VolatilityMetrics
    }
    class VolatilityMetrics {
      +avgRankChange: double
      +rankChangeStdDev: double
      +volatilityTrend: String
    }
    class RankingInfo {
      <<interface>>
      +currentRank: int
      +previousRank: Integer?
      +rankChange: Integer?
      +percentile: Double?
      +rankingScore: RankingScore
    }
    class RankingScore {
      <<interface>>
      +value: double
    }
    class PostingCountScore {
      +value: double
      +totalPostings: int
      +activePostings: int
    }
    RankingScore <|-- PostingCountScore

    %% Ranking 도큐먼트 베이스
    class BaseRankingDocument {
      +metrics: RankingMetrics
      +rankings: List~RankingEntry~
      +page: int
    }
    BaseTimeSeriesDocument <|-- BaseRankingDocument

    class SimpleRankingDocument {
      <<«abstract»>>
      +metrics: RankingMetrics
      +rankings: List~SimpleRankingEntry~
    }
    BaseRankingDocument <|-- SimpleRankingDocument

    class RelationshipRankingDocument {
      <<«abstract»>>
      +metrics: RankingMetrics
      +primaryEntityType: EntityType
      +relatedEntityType: EntityType
      +rankings: List~RelationshipRankingEntry~
    }
    BaseRankingDocument <|-- RelationshipRankingDocument

    class DistributionRankingDocument {
      <<«abstract»>>
      +metrics: RankingMetrics
      +groupEntityType: EntityType
      +targetEntityType: EntityType
      +rankings: List~DistributionRankingEntry~
    }
    BaseRankingDocument <|-- DistributionRankingDocument

    class SimpleRankingEntry {
      <<interface>>
      +score: double
    }
    RankingEntry <|.. SimpleRankingEntry

    class RelationshipRankingEntry {
      <<interface>>
      +primaryEntityId: Long
      +primaryEntityName: String
      +relatedRankings: List~RelatedEntityRank~
    }
    RankingEntry <|.. RelationshipRankingEntry

    class RelatedEntityRank {
      <<interface>>
      +entityId: Long
      +name: String
      +rank: int
      +score: double
    }

    class DistributionRankingEntry {
      <<interface>>
      +distribution: Map~String,Double~
      +dominantCategory: String
      +distributionMetrics: DistributionMetrics
    }
    RankingEntry <|.. DistributionRankingEntry

    class DistributionMetrics {
      +entropy: double
      +concentration: double
      +uniformity: double
    }

    %% Ranking 도큐먼트 구현 – 기존 및 추가 구현
    class SkillPostingCountRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: SkillPostingMetrics
      +rankings: List~SkillPostingRankingEntry~
    }
    SimpleRankingDocument <|-- SkillPostingCountRankingsDocument

    class SkillGrowthRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: SkillGrowthMetrics
      +rankings: List~SkillGrowthRankingEntry~
    }
    SimpleRankingDocument <|-- SkillGrowthRankingsDocument

    class LocationPostingCountRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: LocationPostingMetrics
      +rankings: List~LocationPostingRankingEntry~
    }
    SimpleRankingDocument <|-- LocationPostingCountRankingsDocument

    class IndustrySalaryRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: IndustrySalaryMetrics
      +rankings: List~IndustrySalaryRankingEntry~
    }
    SimpleRankingDocument <|-- IndustrySalaryRankingsDocument

    class JobCategorySkillRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: JobCategorySkillMetrics
      +primaryEntityType: EntityType
      +relatedEntityType: EntityType
      +rankings: List~JobCategorySkillRankingEntry~
    }
    RelationshipRankingDocument <|-- JobCategorySkillRankingsDocument

    class SkillSalaryRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: SkillSalaryMetrics
      +rankings: List~SkillSalaryRankingEntry~
    }
    SimpleRankingDocument <|-- SkillSalaryRankingsDocument

    class LocationSalaryRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: LocationSalaryMetrics
      +rankings: List~LocationSalaryRankingEntry~
    }
    SimpleRankingDocument <|-- LocationSalaryRankingsDocument

    class JobCategorySalaryRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: JobCategorySalaryMetrics
      +rankings: List~JobCategorySalaryRankingEntry~
    }
    SimpleRankingDocument <|-- JobCategorySalaryRankingsDocument

    class JobCategoryPostingCountRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: JobCategoryPostingMetrics
      +rankings: List~JobCategoryPostingRankingEntry~
    }
    SimpleRankingDocument <|-- JobCategoryPostingCountRankingsDocument

    class JobCategoryGrowthRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: JobCategoryGrowthMetrics
      +rankings: List~JobCategoryGrowthRankingEntry~
    }
    SimpleRankingDocument <|-- JobCategoryGrowthRankingsDocument

    class IndustryGrowthRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: IndustryGrowthMetrics
      +rankings: List~IndustryGrowthRankingEntry~
    }
    SimpleRankingDocument <|-- IndustryGrowthRankingsDocument

    class EducationSalaryRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: EducationSalaryMetrics
      +rankings: List~EducationSalaryRankingEntry~
    }
    SimpleRankingDocument <|-- EducationSalaryRankingsDocument

    class CompanySizeSkillRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanySizeSkillMetrics
      +primaryEntityType: EntityType
      +relatedEntityType: EntityType
      +rankings: List~CompanySizeSkillRankingEntry~
    }
    RelationshipRankingDocument <|-- CompanySizeSkillRankingsDocument

    class CompanySizeSalaryRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanySizeSalaryMetrics
      +groupEntityType: EntityType
      +targetEntityType: EntityType
      +rankings: List~CompanySizeSalaryRankingEntry~
    }
    DistributionRankingDocument <|-- CompanySizeSalaryRankingsDocument

    class BenefitPostingCountRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: BenefitPostingMetrics
      +rankings: List~BenefitPostingRankingEntry~
    }
    SimpleRankingDocument <|-- BenefitPostingCountRankingsDocument

    class CompanyGrowthRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanyGrowthMetrics
      +rankings: List~CompanyGrowthRankingEntry~
    }
    SimpleRankingDocument <|-- CompanyGrowthRankingsDocument

    class CompanyHiringVolumeRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanyHiringMetrics
      +rankings: List~CompanyHiringRankingEntry~
    }
    SimpleRankingDocument <|-- CompanyHiringVolumeRankingsDocument

    class CompanyRetentionRateRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanyRetentionMetrics
      +rankings: List~CompanyRetentionRankingEntry~
    }
    SimpleRankingDocument <|-- CompanyRetentionRateRankingsDocument

    class CompanySalaryRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanySalaryMetrics
      +rankings: List~CompanySalaryRankingEntry~
    }
    SimpleRankingDocument <|-- CompanySalaryRankingsDocument

    class CompanySizeBenefitRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanySizeBenefitMetrics
      +groupEntityType: EntityType
      +targetEntityType: EntityType
      +rankings: List~CompanySizeBenefitRankingEntry~
    }
    DistributionRankingDocument <|-- CompanySizeBenefitRankingsDocument

    class CompanySizeEducationRankingsDocument {
      +id: String?
      +page: int
      +baseDate: String
      +period: SnapshotPeriod
      +metrics: CompanySizeEducationMetrics
      +groupEntityType: EntityType
      +targetEntityType: EntityType
      +rankings: List~CompanySizeEducationRankingEntry~
    }
    DistributionRankingDocument <|-- CompanySizeEducationRankingsDocument
```
### Mongo Repository 상속구조
```mermaid
classDiagram
    %% 인터페이스 정의
    class MongoRepository {
        <<interface>>
    }
    class BaseMongoRepository {
        <<interface>>
        +findByCreatedAtBetween(start, end)
        +findAllByQuery(query)
        +bulkFindByIds(ids)
        +bulkInsert(entities)
        +bulkUpdate(entities)
        +bulkDelete(ids)
        +bulkUpsert(entities)
    }
    class BaseTimeSeriesRepository {
        <<interface>>
        +findByBaseDate(baseDate)
        +findByBaseDateBetween(startDate, endDate)
        +findLatest()
        +findLatestN(n)
    }
    class BaseRankingRepository {
        <<interface>>
        +findByPage(baseDate, page)
        +findAllPages(baseDate)
        +findTopN(baseDate, limit)
        +findByRankRange(baseDate, startRank, endRank)
        +findByEntityId(baseDate, entityId)
        +findTopMovers(startDate, endDate, limit)
        +findTopLosers(startDate, endDate, limit)
        +findStableEntities(months, maxRankChange)
        +findVolatileEntities(months, minRankChange)
        +findEntitiesWithConsistentRanking(months, maxRank)
    }
    class SimpleRankingRepository {
        <<interface>>
        +findByValueRange(baseDate, minValue, maxValue)
        +findRisingStars(months, minRankImprovement)
        +findByEntityIdAndBaseDate(entityId, baseDate)
    }
    class RelationshipRankingRepository {
        <<interface>>
        +findByPrimaryEntityId(primaryEntityId, baseDate)
        +findTopNRelatedEntities(primaryEntityId, baseDate, limit)
        +findByRelatedEntityId(relatedEntityId, baseDate)
        +findStrongRelationships(baseDate, minScore)
        +findStrongestPairs(baseDate, limit)
        +findCommonRelationships(primaryEntityId1, primaryEntityId2, baseDate)
    }
    class DistributionRankingRepository {
        <<interface>>
        +findByDistributionPattern(baseDate, pattern, threshold)
        +findByDominantCategory(baseDate, category)
        +findDistributionTrends(entityId, months)
        +findSignificantDistributionChanges(startDate, endDate)
        +findSimilarDistributions(entityId, baseDate, similarity)
        +findUniformDistributions(baseDate, maxVariance)
        +findSkewedDistributions(baseDate, minSkewness)
        +findDistributionChanges(entityId, months)
        +findCategoryDominance(baseDate, category, minPercentage)
    }
    class ReferenceMongoRepository {
        <<interface>>
        +findByReferenceId(referenceId)
        +findByReferenceIds(referenceIds)
        +findByEntityType(entityType)
        +findByEntityTypeAndStatus(entityType, status)
        +findByReferenceIdAndEntityType(referenceId, entityType)
        +updateStatus(referenceId, status)
    }
    class StatsMongoRepository {
        <<interface>>
        +getCollectionName()
        +findByEntityId(entityId)
        +findByEntityIdAndBaseDate(entityId, baseDate)
        +findByBaseDateAndEntityIds(baseDate, entityIds)
        +findStatsByEntityIdsBatch(baseDate, entityIds, batchSize)
        +findByBaseDateBetweenAndEntityId(startDate, endDate, entityId)
        +findLatestStatsByEntityId(entityId)
        +findTopGrowthSkills(startDate, endDate, limit)
        +findTopSkillsByIndustry(industryId, baseDate, limit)
        +findTopSkillsByCompanySize(companySize, baseDate, limit)
        +findTopSkillsByJobCategory(jobCategoryId, baseDate, limit)
        +findEmergingSkillsByIndustry(industryId, baseDate, minGrowthRate)
        +findSkillsWithMultiIndustryGrowth(baseDate, minIndustryCount, minGrowthRate)
    }

    %% 구현 클래스 정의
    class SimpleMongoRepository {
        <<abstract>>
    }
    class BaseMongoRepositoryImpl {
        <<abstract>>
        -OPTIMAL_BATCH_SIZE: int
        -UNORDERED_BULK_OPTIONS: BulkWriteOptions
        +findByCreatedAtBetween(start, end)
        +findAllByQuery(query)
        +bulkFindByIds(ids)
        +bulkInsert(entities)
        +bulkUpdate(entities)
        +bulkDelete(ids)
        +bulkUpsert(entities)
        -queryToFilter(query)
        -queryToSort(query)
    }
    class BaseTimeSeriesRepositoryImpl {
        <<abstract>>
        +findByBaseDate(baseDate)
        +findByBaseDateBetween(startDate, endDate)
        +findLatest()
        +findLatestN(n)
    }
    class BaseRankingRepositoryImpl {
        <<abstract>>
        +findByPage(baseDate, page)
        +findAllPages(baseDate)
        +findTopN(baseDate, limit)
        +findByRankRange(baseDate, startRank, endRank)
        +findByEntityId(baseDate, entityId)
        +findTopMovers(startDate, endDate, limit)
        +findTopLosers(startDate, endDate, limit)
        +findStableEntities(months, maxRankChange)
        +findVolatileEntities(months, minRankChange)
        +findEntitiesWithConsistentRanking(months, maxRank)
    }
    class SimpleRankingRepositoryImpl {
        <<abstract>>
        +findByValueRange(baseDate, minValue, maxValue)
        +findRisingStars(months, minRankImprovement)
        +findByEntityIdAndBaseDate(entityId, baseDate)
    }
    class RelationshipRankingRepositoryImpl {
        <<abstract>>
        +findByPrimaryEntityId(primaryEntityId, baseDate)
        +findTopNRelatedEntities(primaryEntityId, baseDate, limit)
        +findByRelatedEntityId(relatedEntityId, baseDate)
        +findStrongRelationships(baseDate, minScore)
        +findStrongestPairs(baseDate, limit)
        +findCommonRelationships(primaryEntityId1, primaryEntityId2, baseDate)
    }
    class DistributionRankingRepositoryImpl {
        <<abstract>>
        +findByDistributionPattern(baseDate, pattern, threshold)
        +findByDominantCategory(baseDate, category)
        +findDistributionTrends(entityId, months)
        +findSignificantDistributionChanges(startDate, endDate)
        +findSimilarDistributions(entityId, baseDate, similarity)
        +findUniformDistributions(baseDate, maxVariance)
        +findSkewedDistributions(baseDate, minSkewness)
        +findDistributionChanges(entityId, months)
        +findCategoryDominance(baseDate, category, minPercentage)
        -calculateSimilarity(dist1, dist2)
        -calculateDistributionChange(dist1, dist2)
        -calculateCosineSimilarity(dist1, dist2)
    }
    class ReferenceMongoRepositoryImpl {
        <<abstract>>
        +findByReferenceId(referenceId)
        +findByReferenceIds(referenceIds)
        +findByEntityType(entityType)
        +findByEntityTypeAndStatus(entityType, status)
        +findByReferenceIdAndEntityType(referenceId, entityType)
        +updateStatus(referenceId, status)
    }
    class StatsMongoRepositoryImpl {
        <<abstract>>
        -collection
        +getCollectionName()
        +findByEntityId(entityId)
        +findByEntityIdAndBaseDate(entityId, baseDate)
        +findByBaseDateAndEntityIds(baseDate, entityIds)
        +findStatsByEntityIdsBatch(baseDate, entityIds, batchSize)
        +findByBaseDateBetweenAndEntityId(startDate, endDate, entityId)
        +findLatestStatsByEntityId(entityId)
        +findTopGrowthSkills(startDate, endDate, limit)
        +findTopSkillsByIndustry(industryId, baseDate, limit)
        +findTopSkillsByCompanySize(companySize, baseDate, limit)
        +findTopSkillsByJobCategory(jobCategoryId, baseDate, limit)
        +findEmergingSkillsByIndustry(industryId, baseDate, minGrowthRate)
        +findSkillsWithMultiIndustryGrowth(baseDate, minIndustryCount, minGrowthRate)
    }

    %% 인터페이스 상속 관계
    MongoRepository <|-- BaseMongoRepository
    BaseMongoRepository <|-- BaseTimeSeriesRepository
    BaseTimeSeriesRepository <|-- BaseRankingRepository
    BaseTimeSeriesRepository <|-- StatsMongoRepository
    BaseMongoRepository <|-- ReferenceMongoRepository
    BaseRankingRepository <|-- SimpleRankingRepository
    BaseRankingRepository <|-- RelationshipRankingRepository
    BaseRankingRepository <|-- DistributionRankingRepository

    %% 구현 클래스 관계
    SimpleMongoRepository <|-- BaseMongoRepositoryImpl
    BaseMongoRepositoryImpl <|-- BaseTimeSeriesRepositoryImpl
    BaseTimeSeriesRepositoryImpl <|-- BaseRankingRepositoryImpl
    BaseTimeSeriesRepositoryImpl <|-- StatsMongoRepositoryImpl
    BaseMongoRepositoryImpl <|-- ReferenceMongoRepositoryImpl
    BaseRankingRepositoryImpl <|-- SimpleRankingRepositoryImpl
    BaseRankingRepositoryImpl <|-- RelationshipRankingRepositoryImpl
    BaseRankingRepositoryImpl <|-- DistributionRankingRepositoryImpl

    %% 인터페이스-구현 관계
    BaseMongoRepository <.. BaseMongoRepositoryImpl
    BaseTimeSeriesRepository <.. BaseTimeSeriesRepositoryImpl
    BaseRankingRepository <.. BaseRankingRepositoryImpl
    SimpleRankingRepository <.. SimpleRankingRepositoryImpl
    RelationshipRankingRepository <.. RelationshipRankingRepositoryImpl
    DistributionRankingRepository <.. DistributionRankingRepositoryImpl
    ReferenceMongoRepository <.. ReferenceMongoRepositoryImpl
    StatsMongoRepository <.. StatsMongoRepositoryImpl
```
# Batch 과정
```mermaid
flowchart TD
    classDef crawlingClass fill:#d4f1f9,stroke:#05a,stroke-width:1px
    classDef processingClass fill:#ffebcc,stroke:#c63,stroke-width:1px
    classDef enrichmentClass fill:#e6f5d0,stroke:#383,stroke-width:1px
    classDef statisticsClass fill:#f9d5e5,stroke:#934,stroke-width:1px
    classDef utilityClass fill:#e8e8e8,stroke:#666,stroke-width:1px

    subgraph "1. Data Collection"
        A1[MultipleSiteCrawlerService] -->|Scheduled Crawling| A2[UrlCrawler]
        A2 -->|Web Scraping| A3[UrlDataSource]
        A3 -->|Parse HTML| A4[WebPageParser]
        A4 -->|Extract Listings| A5[JobListing Objects]
        A5 -->|Convert to DB Entities| A6[CrawlingTarget]
        A6 -->|Store| A7[CrawlingTargetService]
        A2 -->|Update State| A8[RedisCrawlingRepository]
        A2 -->|Log Errors| A9[CrawlingErrorRepository]
    end

    subgraph "2. Data Processing"
        B1[JobQueueBatchConfig] -->|Configuration| B2[SingleJobQueueReader]
        B2 -->|Read Queue Items| B3[JobQueue with status=PENDING]
        B3 -->|Process Item| B4[SingleJobQueueProcessor]
        B4 -->|Extract Metadata| B5[PreProcessedJobQueue]
        B5 -->|Write Results| B6[SingleJobQueueWriter]
        B6 -->|Create Company| B7[CompanyService]
        B6 -->|Create Job Posting| B8[JobPostingService]
        B6 -->|Create Mappings| B9[Various Mapping Services]
    end

    subgraph "3. Data Enrichment"
        C1[SingleJobQueueProcessingUtils] -->|Skills Processing| C2[Process Skills with Keywords]
        C1 -->|Job Categories Processing| C3[Process Categories from Title/Skills]
        C1 -->|Location Processing| C4[Process Location with Synonyms]
        C1 -->|Benefits Processing| C5[Process Benefits]
        C1 -->|Certifications Processing| C6[Process Certifications]
        C1 -->|Company Data Preparation| C7[Prepare Company Data]
        C1 -->|Job Posting Data Preparation| C8[Prepare Job Posting Data]
        C1 -->|Date/Salary Processing| C9[Extract Experience/Salary/Dates]
        C1 -->|Employment Type Processing| C10[Map Contract/Remote Types]
    end

    subgraph "4. Statistics Generation"
        D1[StatisticsBatchConfig] -->|Config Job| D2[StatisticsReader]
        D2 -->|Read Processing Target| D3[StatisticsProcessor]
        D3 -->|Process Entity Stats| D4[StatisticsCalculatorFacade]
        D4 -->|Accumulate Data| D5[Various Calculators]
        D5 -->|Store in Memory| D6[AbstractAccumulatedData Objects]
        D3 -->|Process Rankings| D7[RankingService]
        D7 -->|Update Rankings| D8[RankingManager]
        D8 -->|Manage Score Types| D9[ScoreType COUNT/AVERAGE/LATEST]
        D2 -->|Generate Final Stats| D10[StatisticsTasklet]
        D10 -->|Generate Documents| D11[RankingDocumentGeneratorManager]
        D11 -->|Create Score Objects| D12[RankingScoreFactory]
        D10 -->|Save to MongoDB| D13[StatisticsService]
        D4 -->|Calculate Growth| D14[StatisticsCalculationUtil]
    end

    subgraph "5. Statistics Access"
        E1[RankingStatisticsFacade] -->|Access Company Stats| E2[Company Rankings Repositories]
        E1 -->|Access Industry Stats| E3[Industry Rankings Repositories]
        E1 -->|Access Job Category Stats| E4[Job Category Rankings Repositories]
        E1 -->|Access Location Stats| E5[Location Rankings Repositories]
        E1 -->|Access Skills Stats| E6[Skills Rankings Repositories]
        E1 -->|Access Benefits Stats| E7[Benefits & Education Repositories]
        E1 -->|Retrieve Time Series| E8[Latest Rankings Data]
        E8 -->|Provide to API| E9[Final Statistics Data Access]
    end

    %% Cross-module connections
    A7 --> B3
    B6 --> D2
    C2 --> B4
    C3 --> B4
    C4 --> B4
    C5 --> B4
    C6 --> B4
    C7 --> B4
    C8 --> B4
    C9 --> B4
    C10 --> B4
    D13 --> E1

    %% Apply styles
    class A1,A2,A3,A4,A5,A6,A7,A8,A9 crawlingClass
    class B1,B2,B3,B4,B5,B6,B7,B8,B9 processingClass
    class C1,C2,C3,C4,C5,C6,C7,C8,C9,C10 enrichmentClass
    class D1,D2,D3,D4,D5,D6,D7,D8,D9,D10,D11,D12,D13,D14 statisticsClass
    class E1,E2,E3,E4,E5,E6,E7,E8,E9 utilityClass
```
