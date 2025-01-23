package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.DistributionRankingDocument
import com.example.jobstat.core.state.BaseDate
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface DistributionRankingRepository<
    T : DistributionRankingDocument<E>,
    E : DistributionRankingDocument.DistributionRankingEntry,
    ID : Any,
> : BaseRankingRepository<T, E, ID> {
    fun findByDistributionPattern(
        baseDate: BaseDate,
        pattern: Map<String, Double>,
        threshold: Double,
    ): List<T>

    fun findByDominantCategory(
        baseDate: BaseDate,
        category: String,
    ): List<T>

    fun findDistributionTrends(
        entityId: Long,
        months: Int,
    ): List<T>

    fun findSignificantDistributionChanges(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<T>

    fun findSimilarDistributions(
        entityId: Long,
        baseDate: BaseDate,
        similarity: Double,
    ): List<T>

    fun findUniformDistributions(
        baseDate: BaseDate,
        maxVariance: Double,
    ): List<T>

    fun findSkewedDistributions(
        baseDate: BaseDate,
        minSkewness: Double,
    ): List<T>

    fun findDistributionChanges(
        entityId: Long,
        months: Int,
    ): List<T>

    fun findCategoryDominance(
        baseDate: BaseDate,
        category: String,
        minPercentage: Double,
    ): List<T>

    fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T?
}

abstract class DistributionRankingRepositoryImpl<
    T : DistributionRankingDocument<E>,
    E : DistributionRankingDocument.DistributionRankingEntry,
    ID : Any,
>(
    private val entityInformation: MongoEntityInformation<T, ID>,
    private val mongoOperations: MongoOperations,
) : BaseRankingRepositoryImpl<T, E, ID>(entityInformation, mongoOperations),
    DistributionRankingRepository<T, E, ID> {
    private val logger = LoggerFactory.getLogger(DistributionRankingRepositoryImpl::class.java)

    /**
     * 주어진 분포 패턴과 유사한 엔티티들을 찾습니다.
     * 유사도는 각 카테고리별 차이의 평균으로 계산되며, 이 값이 (1 - threshold) 이하인 엔티티들을 반환합니다.
     *
     * @param baseDate 기준 날짜 (yyyy-MM 형식)
     * @param pattern 찾고자 하는 분포 패턴 (카테고리별 비율 맵)
     * @param threshold 유사도 임계값 (0.0 ~ 1.0)
     * @return 유사한 분포를 가진 엔티티들의 리스트
     */
    override fun findByDistributionPattern(
        baseDate: BaseDate,
        pattern: Map<String, Double>,
        threshold: Double,
    ): List<T> {
        val operations = mutableListOf<AggregationOperation>()

        operations.add(match(Criteria.where("base_date").`is`(baseDate.toString())))

        // 각 카테고리별 차이를 계산하고 평균을 구함
        val differencesExpr =
            pattern.entries.joinToString(" + ") {
                "abs(\$rankings.distribution.${it.key} - ${it.value})"
            }

        operations.add(
            project()
                .and("rankings")
                .`as`("rankings")
                .and("base_date")
                .`as`("base_date")
                .andExpression("{ \$divide: [ { \$add: [$differencesExpr] }, ${pattern.size} ] }")
                .`as`("similarity_score"),
        )

        operations.add(match(Criteria.where("similarity_score").lte(1 - threshold)))
        operations.add(sort(Sort.Direction.ASC, "similarity_score"))

        val aggregation = newAggregation(operations)
        return mongoOperations
            .aggregate(
                aggregation,
                entityInformation.collectionName,
                entityInformation.javaType,
            ).mappedResults
    }

    /**
     * 특정 카테고리가 지배적인 엔티티들을 찾습니다.
     * 결과는 해당 카테고리의 분포값 기준으로 내림차순 정렬됩니다.
     *
     * @param baseDate 기준 날짜 (yyyy-MM 형식)
     * @param category 조회할 카테고리
     * @return 해당 카테고리가 지배적인 엔티티들의 리스트
     */
    override fun findByDominantCategory(
        baseDate: BaseDate,
        category: String,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.eq("rankings.dominant_category", category),
                ),
            ).sort(Sorts.descending("rankings.distribution.$category"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티의 분포 트렌드를 조회합니다.
     * 최근 데이터부터 지정된 개월 수만큼의 분포 변화를 보여줍니다.
     *
     * @param entityId 엔티티 ID
     * @param months 조회할 개월 수
     * @return 엔티티의 분포 트렌드 데이터 리스트
     */
    override fun findDistributionTrends(
        entityId: Long,
        months: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("rankings.entity_id", entityId),
            ).sort(Sorts.descending("base_date"))
            .limit(months)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티의 특정 날짜 데이터를 조회합니다.
     *
     * @param entityId 엔티티 ID
     * @param baseDate 기준 날짜 (yyyy-MM 형식)
     * @return 해당 엔티티의 데이터 또는 null
     */
    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: BaseDate,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.eq("rankings.entity_id", entityId),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    /**
     * 주어진 기간 동안 분포의 큰 변화가 있는 엔티티들을 찾습니다.
     * 분포 변화는 유클리드 거리로 계산되며, 변화량이 0.1보다 큰 엔티티들을 반환합니다.
     *
     * @param startDate 시작 날짜 (yyyy-MM 형식)
     * @param endDate 종료 날짜 (yyyy-MM 형식)
     * @return 분포 변화가 큰 엔티티들의 리스트
     */
    override fun findSignificantDistributionChanges(
        startDate: BaseDate,
        endDate: BaseDate,
    ): List<T> {
        val operations = mutableListOf<AggregationOperation>()

        operations.add(
            match(
                Criteria.where("base_date").gte(startDate.toString()).lte(endDate.toString()),
            ),
        )

        operations.add(
            group("rankings.entity_id")
                .first("rankings.name")
                .`as`("name")
                .first("rankings.entity_id")
                .`as`("entity_id")
                .push("\$ROOT")
                .`as`("timeline"),
        )

        val projectStage =
            """
            {
              ${'$'}project: {
                name: 1,
                entity_id: 1,
                firstDistribution: { ${'$'}arrayElemAt: ["${'$'}timeline.rankings.distribution", 0] },
                lastDistribution: { ${'$'}arrayElemAt: ["${'$'}timeline.rankings.distribution", -1] },
                distributionChange: {
                  ${'$'}sqrt: {
                    ${'$'}sum: {
                      ${'$'}map: {
                        input: { ${'$'}objectToArray: { ${'$'}arrayElemAt: ["${'$'}timeline.rankings.distribution", 0] } },
                        as: "entry",
                        in: {
                          ${'$'}pow: [
                            { 
                              ${'$'}subtract: [
                                { ${'$'}getField: { input: { ${'$'}arrayElemAt: ["${'$'}timeline.rankings.distribution", -1] }, field: "$${'$'}entry.k" } },
                                "$${'$'}entry.v"
                              ]
                            },
                            2
                          ]
                        }
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()

        operations.add(CustomAggregationOperation(projectStage))

        operations.add(
            match(
                Criteria.where("distributionChange").gt(0.1),
            ),
        )

        operations.add(sort(Sort.Direction.DESC, "distributionChange"))

        val aggregation = newAggregation(operations)
        return mongoOperations
            .aggregate(
                aggregation,
                entityInformation.collectionName,
                entityInformation.javaType,
            ).mappedResults
    }

    private class CustomAggregationOperation(
        private val jsonOperation: String,
    ) : AggregationOperation {
        @Deprecated("Deprecated in Java")
        override fun toDocument(context: AggregationOperationContext): Document = Document.parse(jsonOperation)
    }

    private fun Map<String, Double>.toMongoDocument(): String =
        entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ", ",
        ) { (key, value) ->
            "\"$key\": $value"
        }

    /**
     * 특정 엔티티와 유사한 분포를 가진 다른 엔티티들을 찾습니다.
     * 유사도는 코사인 유사도로 계산되며, 지정된 유사도 이상의 엔티티들을 반환합니다.
     *
     * @param entityId 기준 엔티티 ID
     * @param baseDate 기준 날짜 (yyyy-MM 형식)
     * @param similarity 최소 유사도 값 (0.0 ~ 1.0)
     * @return 유사한 분포를 가진 엔티티들의 리스트
     */
    override fun findSimilarDistributions(
        entityId: Long,
        baseDate: BaseDate,
        similarity: Double,
    ): List<T> {
        val operations = mutableListOf<AggregationOperation>()

        val baseEntity =
            findByEntityIdAndBaseDate(entityId, baseDate)
                ?: throw IllegalArgumentException("Base entity not found")

        val baseDistribution = baseEntity.rankings.first { it.entityId == entityId }.distribution
        val targetDistStr = baseDistribution.toMongoDocument()

        operations.add(
            match(
                Criteria
                    .where("base_date")
                    .`is`(baseDate.toString())
                    .and("rankings.entity_id")
                    .ne(entityId),
            ),
        )

        val projectStage =
            """
            {
              ${'$'}project: {
                rankings: 1,
                base_date: 1,
                similarityScore: {
                  ${'$'}let: {
                    vars: {
                      currentDist: "${'$'}rankings.distribution",
                      targetDist: $targetDistStr
                    },
                    in: {
                      ${'$'}divide: [
                        {
                          ${'$'}reduce: {
                            input: { ${'$'}objectToArray: "$${'$'}targetDist" },
                            initialValue: 0,
                            in: {
                              ${'$'}add: [
                                "$${'$'}value",
                                {
                                  ${'$'}multiply: [
                                    "$${'$'}this.v",
                                    { ${'$'}getField: { field: "$${'$'}this.k", input: "$${'$'}currentDist" } }
                                  ]
                                }
                              ]
                            }
                          }
                        },
                        {
                          ${'$'}multiply: [
                            {
                              ${'$'}sqrt: {
                                ${'$'}reduce: {
                                  input: { ${'$'}objectToArray: "$${'$'}targetDist" },
                                  initialValue: 0,
                                  in: { ${'$'}add: ["$${'$'}value", { ${'$'}multiply: ["$${'$'}this.v", "$${'$'}this.v"] }] }
                                }
                              }
                            },
                            {
                              ${'$'}sqrt: {
                                ${'$'}reduce: {
                                  input: { ${'$'}objectToArray: "$${'$'}currentDist" },
                                  initialValue: 0,
                                  in: { ${'$'}add: ["$${'$'}value", { ${'$'}multiply: ["$${'$'}this.v", "$${'$'}this.v"] }] }
                                }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()

        operations.add(CustomAggregationOperation(projectStage))

        operations.add(
            match(
                Criteria.where("similarityScore").gte(similarity),
            ),
        )

        operations.add(sort(Sort.Direction.DESC, "similarityScore"))

        val aggregation = newAggregation(operations)
        return mongoOperations
            .aggregate(
                aggregation,
                entityInformation.collectionName,
                entityInformation.javaType,
            ).mappedResults
    }

    /**
     * 균일한 분포를 가진 엔티티들을 찾습니다.
     * 분포의 균일성은 uniformity 메트릭으로 측정되며, 지정된 최대 분산 이하의 엔티티들을 반환합니다.
     *
     * @param baseDate 기준 날짜 (yyyy-MM 형식)
     * @param maxVariance 최대 분산값
     * @return 균일한 분포를 가진 엔티티들의 리스트
     */
    override fun findUniformDistributions(
        baseDate: BaseDate,
        maxVariance: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.lte("rankings.distribution_metrics.uniformity", maxVariance),
                ),
            ).sort(Sorts.ascending("rankings.distribution_metrics.uniformity"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 치우친(편향된) 분포를 가진 엔티티들을 찾습니다.
     * 분포의 편향도는 concentration 메트릭으로 측정되며, 지정된 최소 편향도 이상의 엔티티들을 반환합니다.
     *
     * @param baseDate 기준 날짜 (yyyy-MM 형식)
     * @param minSkewness 최소 편향도
     * @return 편향된 분포를 가진 엔티티들의 리스트
     */
    override fun findSkewedDistributions(
        baseDate: BaseDate,
        minSkewness: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.gte("rankings.distribution_metrics.concentration", minSkewness),
                ),
            ).sort(Sorts.descending("rankings.distribution_metrics.concentration"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 엔티티의 분포 변화를 조회합니다.
     * 최근 데이터부터 지정된 개월 수만큼의 분포 변화 이력을 보여줍니다.
     *
     * @param entityId 엔티티 ID
     * @param months 조회할 개월 수
     * @return 엔티티의 분포 변화 이력 리스트
     */
    override fun findDistributionChanges(
        entityId: Long,
        months: Int,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.eq("rankings.entity_id", entityId),
            ).sort(Sorts.descending("base_date"))
            .limit(months)
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    /**
     * 특정 카테고리에서 지정된 비율 이상의 점유율을 가진 엔티티들을 찾습니다.
     * 해당 카테고리의 분포값 기준으로 내림차순 정렬하여 반환합니다.
     *
     * @param baseDate 기준 날짜 (yyyy-MM 형식)
     * @param category 조회할 카테고리
     * @param minPercentage 최소 점유율 (0.0 ~ 1.0)
     * @return 카테고리 점유율이 높은 엔티티들의 리스트
     */
    override fun findCategoryDominance(
        baseDate: BaseDate,
        category: String,
        minPercentage: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate.toString()),
                    Filters.gte("rankings.distribution.$category", minPercentage),
                ),
            ).sort(Sorts.descending("rankings.distribution.$category"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    // 내부 유틸리티 함수들

    /**
     * 두 분포간의 내적(dot product)을 계산하는 MongoDB 표현식을 생성합니다.
     *
     * @param targetDistribution 비교할 대상 분포
     * @return 내적 계산을 위한 MongoDB Document
     */
    private fun calculateDotProduct(targetDistribution: Map<String, Double>): Document =
        Document(
            "\$sum",
            Document(
                "\$map",
                Document(
                    mapOf(
                        "input" to Document("\$objectToArray", targetDistribution),
                        "as" to "item",
                        "in" to
                            Document(
                                "\$multiply",
                                listOf(
                                    "\$\$item.v",
                                    Document("\$getField", listOf("\$rankings.distribution", "\$\$item.k")),
                                ),
                            ),
                    ),
                ),
            ),
        )

    /**
     * 분포의 크기(magnitude)를 계산하는 MongoDB 표현식을 생성합니다.
     *
     * @param distribution 크기를 계산할 분포
     * @return 크기 계산을 위한 MongoDB Document
     */
    private fun calculateMagnitude(distribution: Any): Document =
        Document(
            "\$sqrt",
            Document(
                "\$sum",
                Document(
                    "\$map",
                    Document(
                        mapOf(
                            "input" to Document("\$objectToArray", distribution),
                            "as" to "item",
                            "in" to Document("\$pow", listOf("\$\$item.v", 2)),
                        ),
                    ),
                ),
            ),
        )
}
