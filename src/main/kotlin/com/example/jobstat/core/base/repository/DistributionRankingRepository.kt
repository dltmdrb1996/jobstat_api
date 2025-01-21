package com.example.jobstat.core.base.repository

import com.example.jobstat.core.base.mongo.ranking.DistributionRankingDocument
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
        baseDate: String,
        pattern: Map<String, Double>,
        threshold: Double,
    ): List<T>

    fun findByDominantCategory(
        baseDate: String,
        category: String,
    ): List<T>

    fun findDistributionTrends(
        entityId: Long,
        months: Int,
    ): List<T>

    fun findSignificantDistributionChanges(
        startDate: String,
        endDate: String,
    ): List<T>

    fun findSimilarDistributions(
        entityId: Long,
        baseDate: String,
        similarity: Double,
    ): List<T>

    fun findUniformDistributions(
        baseDate: String,
        maxVariance: Double,
    ): List<T>

    fun findSkewedDistributions(
        baseDate: String,
        minSkewness: Double,
    ): List<T>

    fun findDistributionChanges(
        entityId: Long,
        months: Int,
    ): List<T>

    fun findCategoryDominance(
        baseDate: String,
        category: String,
        minPercentage: Double,
    ): List<T>

    fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
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

    override fun findByDistributionPattern(
        baseDate: String,
        pattern: Map<String, Double>,
        threshold: Double,
    ): List<T> {
        val operations = mutableListOf<AggregationOperation>()

        operations.add(match(Criteria.where("base_date").`is`(baseDate)))

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

    override fun findByDominantCategory(
        baseDate: String,
        category: String,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.eq("rankings.dominant_category", category),
                ),
            ).sort(Sorts.descending("rankings.distribution.$category"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

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

    override fun findByEntityIdAndBaseDate(
        entityId: Long,
        baseDate: String,
    ): T? {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.eq("rankings.entity_id", entityId),
                ),
            ).limit(1)
            .firstOrNull()
            ?.let { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
    }

    override fun findSignificantDistributionChanges(
        startDate: String,
        endDate: String,
    ): List<T> {
        val operations = mutableListOf<AggregationOperation>()

        operations.add(
            match(
                Criteria.where("base_date").gte(startDate).lte(endDate),
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

    override fun findSimilarDistributions(
        entityId: Long,
        baseDate: String,
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
                    .`is`(baseDate)
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

    override fun findUniformDistributions(
        baseDate: String,
        maxVariance: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.lte("rankings.distribution_metrics.uniformity", maxVariance),
                ),
            ).sort(Sorts.ascending("rankings.distribution_metrics.uniformity"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

    override fun findSkewedDistributions(
        baseDate: String,
        minSkewness: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.gte("rankings.distribution_metrics.concentration", minSkewness),
                ),
            ).sort(Sorts.descending("rankings.distribution_metrics.concentration"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

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

    override fun findCategoryDominance(
        baseDate: String,
        category: String,
        minPercentage: Double,
    ): List<T> {
        val collection = mongoOperations.getCollection(entityInformation.collectionName)

        return collection
            .find(
                Filters.and(
                    Filters.eq("base_date", baseDate),
                    Filters.gte("rankings.distribution.$category", minPercentage),
                ),
            ).sort(Sorts.descending("rankings.distribution.$category"))
            .map { doc -> mongoOperations.converter.read(entityInformation.javaType, doc) }
            .toList()
    }

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
