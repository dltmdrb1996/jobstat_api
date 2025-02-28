# 프로젝트 구조
```
📁 main
├── 📁 kotlin
│   └── 📁 com
│       └── 📁 example
│           └── 📁 jobstat
│               ├── 📄 AdminController.kt
│               ├── 📄 IndexController.kt
│               ├── 📄 JobstatApplication.kt
│               ├── 📁 auth
│               │   ├── 📄 AuthController.kt
│               │   ├── 📁 email
│               │   │   ├── 📄 EmailVerificationController.kt
│               │   │   ├── 📁 entity
│               │   │   │   └── 📄 EmailVerification.kt
│               │   │   ├── 📁 repository
│               │   │   │   ├── 📄 EmailVerificationRepository.kt
│               │   │   │   └── 📄 EmailVerificationRepositoryImpl.kt
│               │   │   ├── 📁 service
│               │   │   │   ├── 📄 EmailService.kt
│               │   │   │   ├── 📄 EmailServiceImpl.kt
│               │   │   │   ├── 📄 EmailVerificationService.kt
│               │   │   │   └── 📄 EmailVerificationServiceImpl.kt
│               │   │   └── 📁 usecase
│               │   │       ├── 📄 RequestEmailVerification.kt
│               │   │       └── 📄 VerifyEmail.kt
│               │   ├── 📁 token
│               │   │   ├── 📁 service
│               │   │   │   ├── 📄 TokenService.kt
│               │   │   │   └── 📄 TokenServiceImpl.kt
│               │   │   └── 📁 usecase
│               │   │       └── 📄 RefreshToken.kt
│               │   └── 📁 user
│               │       ├── 📄 DummyUserController.kt
│               │       ├── 📄 UserConstants.kt
│               │       ├── 📄 UserController.kt
│               │       ├── 📁 entity
│               │       │   ├── 📄 Role.kt
│               │       │   ├── 📄 User.kt
│               │       │   └── 📄 UserRole.kt
│               │       ├── 📁 repository
│               │       │   ├── 📄 RoleRepository.kt
│               │       │   ├── 📄 RoleRepositoryImpl.kt
│               │       │   ├── 📄 UserRepository.kt
│               │       │   └── 📄 UserRepositoryImpl.kt
│               │       ├── 📁 service
│               │       │   ├── 📄 LoginAttemptService.kt
│               │       │   ├── 📄 LoginAttemptServiceImpl.kt
│               │       │   ├── 📄 UserService.kt
│               │       │   └── 📄 UserServiceImpl.kt
│               │       └── 📁 usecase
│               │           ├── 📄 Login.kt
│               │           ├── 📄 Register.kt
│               │           ├── 📄 UpdateUserPassword.kt
│               │           └── 📄 UpdateUserStatus.kt
│               ├── 📁 community
│               │   ├── 📁 board
│               │   │   ├── 📄 BoardConstants.kt
│               │   │   ├── 📄 BoardController.kt
│               │   │   ├── 📁 entity
│               │   │   │   ├── 📄 Board.kt
│               │   │   │   └── 📄 BoardCategory.kt
│               │   │   ├── 📁 model
│               │   │   │   └── 📄 BoardType.kt
│               │   │   ├── 📁 repository
│               │   │   │   ├── 📄 BoardRepository.kt
│               │   │   │   ├── 📄 BoardRepositoryImpl.kt
│               │   │   │   ├── 📄 CategoryRepository.kt
│               │   │   │   └── 📄 CategoryRepositoryImpl.kt
│               │   │   ├── 📁 service
│               │   │   │   ├── 📄 BoardService.kt
│               │   │   │   ├── 📄 BoardServiceImpl.kt
│               │   │   │   ├── 📄 CategoryService.kt
│               │   │   │   └── 📄 CategoryServiceImpl.kt
│               │   │   └── 📁 usecase
│               │   │       ├── 📄 CreateBoard.kt
│               │   │       ├── 📄 CreateGuestBoard.kt
│               │   │       ├── 📄 CreateMemberBoard.kt
│               │   │       ├── 📄 GetAuthorActivities.kt
│               │   │       ├── 📄 GetBoardDetail.kt
│               │   │       ├── 📄 GetBoardList.kt
│               │   │       ├── 📄 GetBoardStats.kt
│               │   │       ├── 📄 GetBoardsByCategory.kt
│               │   │       ├── 📄 GetTopBoards.kt
│               │   │       ├── 📄 LikeBoard.kt
│               │   │       └── 📄 UpdateBoard.kt
│               │   └── 📁 comment
│               │       ├── 📄 CommentConstants.kt
│               │       ├── 📄 CommentController.kt
│               │       ├── 📁 entity
│               │       │   └── 📄 Comment.kt
│               │       ├── 📁 repository
│               │       │   ├── 📄 CommentRepository.kt
│               │       │   └── 📄 CommentRepositoryImpl.kt
│               │       ├── 📁 service
│               │       │   ├── 📄 CommentService.kt
│               │       │   └── 📄 CommentServiceImpl.kt
│               │       └── 📁 usecase
│               │           ├── 📄 AddComment.kt
│               │           ├── 📄 DeleteBoard.kt
│               │           ├── 📄 DeleteComment.kt
│               │           ├── 📄 GetRecentComments.kt
│               │           └── 📄 UpdateComment.kt
│               ├── 📁 core
│               │   ├── 📁 base
│               │   │   ├── 📄 Address.kt
│               │   │   ├── 📄 BaseEntity.kt
│               │   │   ├── 📄 JobPreferences.kt
│               │   │   ├── 📄 SoftDeleteBaseEntity.kt
│               │   │   ├── 📄 VersionedBaseEntity.kt
│               │   │   ├── 📄 VersionedSoftDeleteBaseEntity.kt
│               │   │   ├── 📁 mongo
│               │   │   │   ├── 📄 BaseDocument.kt
│               │   │   │   ├── 📄 BaseReferenceDocument.kt
│               │   │   │   ├── 📄 BaseTimeSeriesDocument.kt
│               │   │   │   ├── 📄 CommonDistribution.kt
│               │   │   │   ├── 📄 Distribution.kt
│               │   │   │   ├── 📄 SnapshotPeriod.kt
│               │   │   │   ├── 📁 ranking
│               │   │   │   │   ├── 📄 BaseRankingDocument.kt
│               │   │   │   │   ├── 📄 DistributionRankingDocument.kt
│               │   │   │   │   ├── 📄 RankingEntry.kt
│               │   │   │   │   ├── 📄 RankingMetrics.kt
│               │   │   │   │   ├── 📄 RelationshipRankingDocument.kt
│               │   │   │   │   ├── 📄 SimpleRankingDocument.kt
│               │   │   │   │   └── 📄 VolatilityMetrics.kt
│               │   │   │   └── 📁 stats
│               │   │   │       ├── 📄 BaseStats.kt
│               │   │   │       ├── 📄 BaseStatsDocument.kt
│               │   │   │       ├── 📄 CommonStats.kt
│               │   │   │       ├── 📄 RankingInfo.kt
│               │   │   │       └── 📄 RankingScore.kt
│               │   │   └── 📁 repository
│               │   │       ├── 📄 BaseMongoRepository.kt
│               │   │       ├── 📄 BaseRankingRepository.kt
│               │   │       ├── 📄 BaseTimeSeriesRepository.kt
│               │   │       ├── 📄 DistributionRankingRepository.kt
│               │   │       ├── 📄 ReferenceMongoRepository.kt
│               │   │       ├── 📄 RelationshipRankingRepository.kt
│               │   │       ├── 📄 SimpleRankingRepository.kt
│               │   │       └── 📄 StatsMongoRepository.kt
│               │   ├── 📁 config
│               │   │   ├── 📄 AdminPageConfig.kt
│               │   │   ├── 📄 AppConfig.kt
│               │   │   ├── 📄 CacheConfig.kt
│               │   │   ├── 📄 MailConfig.kt
│               │   │   ├── 📄 MongoConfig.kt
│               │   │   ├── 📄 MonitoringConfig.kt
│               │   │   ├── 📄 ObjectMapperConfig.kt
│               │   │   ├── 📄 OpenApiConfig.kt
│               │   │   ├── 📄 RedisConfig.kt
│               │   │   └── 📄 SpringAsyncConfig.kt
│               │   ├── 📁 constants
│               │   │   ├── 📄 CacheConstants.kt
│               │   │   ├── 📄 CommonConstants.kt
│               │   │   ├── 📄 ExceptionConstants.kt
│               │   │   └── 📄 RestConstants.kt
│               │   ├── 📁 converter
│               │   │   ├── 📄 CustomModelConverter.kt
│               │   │   ├── 📄 RankingScoreReadConverter.kt
│               │   │   ├── 📄 RankingScoreWriteConverter.kt
│               │   │   └── 📄 StringListConverter.kt
│               │   ├── 📁 error
│               │   │   ├── 📄 AppException.kt
│               │   │   ├── 📄 ExceptionHandlers.kt
│               │   │   └── 📄 GlobalExceptionHandler.kt
│               │   ├── 📁 extension
│               │   │   ├── 📄 EntityExtension.kt
│               │   │   ├── 📄 Extension.kt
│               │   │   └── 📄 ListExtension.kt
│               │   ├── 📁 security
│               │   │   ├── 📄 BCryptPasswordUtil.kt
│               │   │   ├── 📄 JwtTokenFilter.kt
│               │   │   ├── 📄 JwtTokenGenerator.kt
│               │   │   ├── 📄 JwtTokenParser.kt
│               │   │   ├── 📄 RateLimitAspect.kt
│               │   │   ├── 📄 SecurityConfig.kt
│               │   │   ├── 📄 TokenPayload.kt
│               │   │   └── 📁 annotation
│               │   │       ├── 📄 AdminAuth.kt
│               │   │       ├── 📄 ApiVersion.kt
│               │   │       ├── 📄 Public.kt
│               │   │       ├── 📄 PublicWithTokenCheck.kt
│               │   │       └── 📄 RateLimit.kt
│               │   ├── 📁 state
│               │   │   ├── 📄 Address.kt
│               │   │   ├── 📄 BaseDate.kt
│               │   │   ├── 📄 CompanySize.kt
│               │   │   ├── 📄 ContractType.kt
│               │   │   ├── 📄 DataType.kt
│               │   │   ├── 📄 DocumentStatus.kt
│               │   │   ├── 📄 EducationLevel.kt
│               │   │   ├── 📄 EducationRequirement.kt
│               │   │   ├── 📄 EntityType.kt
│               │   │   ├── 📄 ExperienceLevel.kt
│               │   │   ├── 📄 JobListing.kt
│               │   │   ├── 📄 RemoteWorkType.kt
│               │   │   ├── 📄 Salary.kt
│               │   │   └── 📄 Site.kt
│               │   ├── 📁 usecase
│               │   │   ├── 📄 UseCase.kt
│               │   │   ├── 📄 UseCaseLoggingAspect.kt
│               │   │   └── 📁 impl
│               │   │       ├── 📄 AsyncUseCase.kt
│               │   │       ├── 📄 BaseUseCase.kt
│               │   │       ├── 📄 CacheableValidUseCase.kt
│               │   │       ├── 📄 LoggingUseCase.kt
│               │   │       ├── 📄 ParallelTransactionalUseCase.kt
│               │   │       ├── 📄 SagaUseCase.kt
│               │   │       └── 📄 ValidUseCase.kt
│               │   ├── 📁 utils
│               │   │   ├── 📄 JwtAuthenticationEntryPoint.kt
│               │   │   ├── 📄 RegexPatterns.kt
│               │   │   ├── 📄 SecurityUtils.kt
│               │   │   ├── 📄 SpringEnvironmentProvider.kt
│               │   │   ├── 📄 StatisticsCalculationUtil.kt
│               │   │   └── 📄 TimeUtility.kt
│               │   └── 📁 wrapper
│               │       ├── 📄 ApiResponse.kt
│               │       ├── 📄 ApiResponseWithType.kt
│               │       └── 📄 ApiResult.kt
│               └── 📁 statistics
│                   ├── 📄 RankingController.kt
│                   ├── 📄 StatsController.kt
│                   ├── 📁 rankings
│                   │   ├── 📁 document
│                   │   │   ├── 📄 BenefitPostingCountRankingsDocument.kt
│                   │   │   ├── 📄 CompanyGrowthRankingsDocument.kt
│                   │   │   ├── 📄 CompanyHiringVolumeRankingsDocument.kt
│                   │   │   ├── 📄 CompanyRetentionRateRankingsDocument.kt
│                   │   │   ├── 📄 CompanySalaryRankingsDocument.kt
│                   │   │   ├── 📄 CompanySizeBenefitRankingsDocument.kt
│                   │   │   ├── 📄 CompanySizeEducationRankingsDocument.kt
│                   │   │   ├── 📄 CompanySizeSalaryRankingsDocument.kt
│                   │   │   ├── 📄 CompanySizeSkillRankingsDocument.kt
│                   │   │   ├── 📄 EducationSalaryRankingsDocument.kt
│                   │   │   ├── 📄 IndustryGrowthRankingsDocument.kt
│                   │   │   ├── 📄 IndustrySalaryRankingsDocument.kt
│                   │   │   ├── 📄 IndustrySkillRankingsDocument.kt
│                   │   │   ├── 📄 JobCategoryGrowthRankingsDocument.kt
│                   │   │   ├── 📄 JobCategoryPostingCountRankingsDocument.kt
│                   │   │   ├── 📄 JobCategorySalaryRankingsDocument.kt
│                   │   │   ├── 📄 JobCategorySkillRankingsDocument.kt
│                   │   │   ├── 📄 LocationPostingCountRankingsDocument.kt
│                   │   │   ├── 📄 LocationSalaryRankingsDocument.kt
│                   │   │   ├── 📄 SkillGrowthRankingsDocument.kt
│                   │   │   ├── 📄 SkillPostingCountRankingsDocument.kt
│                   │   │   └── 📄 SkillSalaryRankingsDocument.kt
│                   │   ├── 📁 model
│                   │   │   ├── 📄 RankingAnalysis.kt
│                   │   │   ├── 📄 RankingPage.kt
│                   │   │   ├── 📄 RankingResult.kt
│                   │   │   ├── 📄 RankingSummary.kt
│                   │   │   ├── 📄 RankingTrends.kt
│                   │   │   ├── 📄 RankingWithStats.kt
│                   │   │   └── 📁 rankingtype
│                   │   │       ├── 📄 DomainType.kt
│                   │   │       └── 📄 RankingType.kt
│                   │   ├── 📁 repository
│                   │   │   ├── 📄 BenefitPostingCountRankingsRepository.kt
│                   │   │   ├── 📄 CompanyGrowthRankingsRepository.kt
│                   │   │   ├── 📄 CompanyHiringVolumeRankingsRepository.kt
│                   │   │   ├── 📄 CompanyRetentionRateRankingsRepository.kt
│                   │   │   ├── 📄 CompanySalaryRankingsRepository.kt
│                   │   │   ├── 📄 CompanySizeBenefitRankingsRepository.kt
│                   │   │   ├── 📄 CompanySizeEducationRankingsRepository.kt
│                   │   │   ├── 📄 CompanySizeSalaryRankingsRepository.kt
│                   │   │   ├── 📄 CompanySizeSkillRankingsRepository.kt
│                   │   │   ├── 📄 EducationSalaryRankingsRepository.kt
│                   │   │   ├── 📄 IndustryGrowthRankingsRepository.kt
│                   │   │   ├── 📄 IndustrySalaryRankingsRepository.kt
│                   │   │   ├── 📄 IndustrySkillRankingsRepository.kt
│                   │   │   ├── 📄 JobCategoryGrowthRankingsRepository.kt
│                   │   │   ├── 📄 JobCategoryPostingCountRankingsRepository.kt
│                   │   │   ├── 📄 JobCategorySalaryRankingsRepository.kt
│                   │   │   ├── 📄 JobCategorySkillRankingsRepository.kt
│                   │   │   ├── 📄 LocationPostingCountRankingsRepository.kt
│                   │   │   ├── 📄 LocationSalaryRankingsRepository.kt
│                   │   │   ├── 📄 RankingRepositoryRegistry.kt
│                   │   │   ├── 📄 RankingRepositoryType.kt
│                   │   │   ├── 📄 SkillGrowthRankingsRepository.kt
│                   │   │   ├── 📄 SkillPostingCountRankingsRepository.kt
│                   │   │   └── 📄 SkillSalaryRankingsRepository.kt
│                   │   ├── 📁 service
│                   │   │   └── 📄 RankingAnalysisService.kt
│                   │   └── 📁 usecase
│                   │       ├── 📄 GetBenefitRankingWithStats.kt
│                   │       ├── 📄 GetCertificationRankingWithStats.kt
│                   │       ├── 📄 GetCompanyRankingWithStats.kt
│                   │       ├── 📄 GetEducationRankingWithStats.kt
│                   │       ├── 📄 GetIndustryRankingWithStats.kt
│                   │       ├── 📄 GetJobCategoryRankingWithStats.kt
│                   │       ├── 📄 GetLocationRankingWithStats.kt
│                   │       ├── 📄 GetRankingPage.kt
│                   │       ├── 📄 GetRankingWithStats.kt
│                   │       ├── 📄 GetSkillRankingWithStats.kt
│                   │       └── 📁 analyze
│                   │           ├── 📄 AnalyzeConsistentRankings.kt
│                   │           ├── 📄 AnalyzeRankRange.kt
│                   │           ├── 📄 AnalyzeRankingMovements.kt
│                   │           ├── 📄 AnalyzeTopLosers.kt
│                   │           ├── 📄 AnalyzeTopPerformers.kt
│                   │           └── 📄 AnalyzeVolatileRankings.kt
│                   └── 📁 stats
│                       ├── 📁 document
│                       │   ├── 📄 BenefitStatsDocument.kt
│                       │   ├── 📄 CertificationStatsDocument.kt
│                       │   ├── 📄 CompanyStatsDocument.kt
│                       │   ├── 📄 ContractTypeStatsDocument.kt
│                       │   ├── 📄 EducationStatsDocument.kt
│                       │   ├── 📄 ExperienceStatsDocument.kt
│                       │   ├── 📄 IndustryStatsDocument.kt
│                       │   ├── 📄 JobCategoryStatsDocument.kt
│                       │   ├── 📄 LocationStatsDocument.kt
│                       │   ├── 📄 RemoteWorkStatsDocument.kt
│                       │   └── 📄 SkillStatsDocument.kt
│                       ├── 📁 registry
│                       │   ├── 📄 StatsRepositoryRegistry.kt
│                       │   ├── 📄 StatsRepositoryType.kt
│                       │   └── 📄 StatsType.kt
│                       ├── 📁 repository
│                       │   ├── 📄 BenefitStatsRepository.kt
│                       │   ├── 📄 CertificationStatsRepository.kt
│                       │   ├── 📄 CompanyStatsRepository.kt
│                       │   ├── 📄 ContractTypeStatsRepository.kt
│                       │   ├── 📄 EducationStatsRepository.kt
│                       │   ├── 📄 ExperienceStatsRepository.kt
│                       │   ├── 📄 IndustryStatsRepository.kt
│                       │   ├── 📄 JobCategoryStatsRepository.kt
│                       │   ├── 📄 LocationStatsRepository.kt
│                       │   ├── 📄 RemoteWorkStatsRepository.kt
│                       │   └── 📄 SkillStatsRepository.kt
│                       ├── 📁 service
│                       │   └── 📄 StatsAnalysisService.kt
│                       └── 📁 usecase
│                           ├── 📄 GetLatestStats.kt
│                           ├── 📄 GetStatsByEntityId.kt
│                           ├── 📄 GetStatsByEntityIdAndBaseDate.kt
│                           └── 📄 GetStatsByEntityIds.kt
📁 test
└── 📁 kotlin
    └── 📁 com
        └── 📁 example
            └── 📁 jobstat
                ├── 📁 auth
                │   ├── 📁 token
                │   │   └── 📁 service
                │   │       └── 📄 TokenServiceTest.kt
                │   └── 📁 user
                │       ├── 📁 fake
                │       │   ├── 📄 FakeRoleRepository.kt
                │       │   ├── 📄 FakeUserRepository.kt
                │       │   ├── 📄 RoleFixture.kt
                │       │   └── 📄 UserFixture.kt
                │       ├── 📁 repository
                │       │   ├── 📄 RoleRepositoryIntegrationTest.kt
                │       │   └── 📄 UserRepositoryIntegrationTest.kt
                │       ├── 📁 service
                │       │   ├── 📄 LoginAttemptServiceTest.kt
                │       │   └── 📄 UserServiceTest.kt
                │       └── 📁 usecase
                │           ├── 📄 LoginTest.kt
                │           └── 📄 RegisterTest.kt
                ├── 📁 community
                │   ├── 📁 fake
                │   │   ├── 📄 BoardFixture.kt
                │   │   ├── 📄 CategoryFixture.kt
                │   │   ├── 📄 CommentFixture.kt
                │   │   └── 📁 repository
                │   │       ├── 📄 FakeBoardRepository.kt
                │   │       ├── 📄 FakeCategoryRepository.kt
                │   │       └── 📄 FakeCommentRepository.kt
                │   ├── 📁 repository
                │   │   ├── 📄 BoardRepositoryIntegrationTest.kt
                │   │   ├── 📄 CategoryRepositoryIntegrationTest.kt
                │   │   └── 📄 CommentRepositoryIntegrationTest.kt
                │   ├── 📁 service
                │   │   ├── 📄 BoardServiceTest.kt
                │   │   ├── 📄 CategoryServiceTest.kt
                │   │   └── 📄 CommentServiceTest.kt
                │   └── 📁 usecase
                │       ├── 📄 AddCommentTest.kt
                │       ├── 📄 CreateBoardTest.kt
                │       ├── 📄 CreateGuestBoardTest.kt
                │       ├── 📄 CreateMemberBoardTest.kt
                │       ├── 📄 DeleteBoardTest.kt
                │       ├── 📄 DeleteCommentTest.kt
                │       ├── 📄 GetAuthorActivitiesTest.kt
                │       ├── 📄 GetBoardDetailTest.kt
                │       ├── 📄 GetBoardListTest.kt
                │       ├── 📄 GetBoardStatsTest.kt
                │       ├── 📄 GetBoardsByCategoryTest.kt
                │       ├── 📄 GetRecentCommentsTest.kt
                │       ├── 📄 GetTopBoardsTest.kt
                │       ├── 📄 LikeBoardTest.kt
                │       └── 📄 UpdateBoardTest.kt
                ├── 📁 core
                │   └── 📁 base
                │       └── 📁 repository
                │           ├── 📄 BaseRankingRepositoryIntegrationTest.kt
                │           ├── 📄 BaseTimeSeriesRepositoryIntegrationTest.kt
                │           ├── 📄 DistributionRankingRepositoryIntegrationTest.kt
                │           ├── 📄 RecordRepositoryMongoIntegrationTest.kt
                │           ├── 📄 RelationshipRankingRepositoryIntegrationTest.kt
                │           ├── 📄 SimpleRankingRepositoryIntegrationTest.kt
                │           └── 📄 StatsMongoRepositoryIntegrationTest.kt
                ├── 📁 rankings
                │   ├── 📁 fake
                │   │   ├── 📄 FakeRankingRepositoryRegistry.kt
                │   │   └── 📄 FakeSkillGrowthRankingsRepository.kt
                │   └── 📁 service
                │       └── 📄 RankingAnalysisServiceTest.kt
                ├── 📁 statistics
                │   ├── 📁 fake
                │   │   ├── 📄 AbstractFakeSimpleRankingRepository.kt
                │   │   └── 📄 AbstractFakeTimeSeriesRepository.kt
                │   └── 📁 stats
                │       ├── 📁 fake
                │       │   ├── 📄 FakeSkillStatsRepository.kt
                │       │   └── 📄 FakeStatsRepositoryRegistry.kt
                │       └── 📁 service
                │           └── 📄 StatsAnalysisServiceTest.kt
                └── 📁 utils
                    ├── 📄 FakePasswordUtil.kt
                    ├── 📄 FakeStringRedisTemplate.kt
                    ├── 📄 IndexManager.kt
                    ├── 📄 TestCacheManager.kt
                    ├── 📄 TestClock.kt
                    ├── 📄 TestFixture.kt
                    ├── 📄 TestUtils.kt
                    ├── 📁 base
                    │   ├── 📄 AbstractFakeDistributionRankingRepository.kt
                    │   ├── 📄 AbstractFakeRankingRepository.kt
                    │   ├── 📄 AbstractFakeRelationshipRankingRepository.kt
                    │   ├── 📄 AbstractFakeSimpleRankingRepository.kt
                    │   ├── 📄 AbstractFakeStatsRepository.kt
                    │   ├── 📄 BaseFakeRepository.kt
                    │   ├── 📄 BaseIntegrationTest.kt
                    │   └── 📄 JpaIntegrationTestSupport.kt
                    ├── 📁 config
                    │   ├── 📄 DockerTestConfig.kt
                    │   ├── 📄 TestMongoConfig.kt
                    │   └── 📄 TestMysqlConfig.kt
                    └── 📁 dummy
                        ├── 📄 Record.kt
                        └── 📄 TestTimeSeriesDocument.kt
```

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
