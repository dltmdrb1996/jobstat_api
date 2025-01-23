package com.example.jobstat.core.base.repository

import com.example.jobstat.utils.base.BatchOperationTestSupport
import com.example.jobstat.utils.dummy.Address
import com.example.jobstat.utils.dummy.Record
import com.example.jobstat.utils.dummy.RecordDto
import com.example.jobstat.utils.dummy.RecordRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import kotlin.random.Random

@TestMethodOrder(OrderAnnotation::class)
class RecordRepositoryMongoIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var recordRepository: RecordRepository

    private val totalRecords = 50000
    private val batchSize = 2000
    private val allRecords = mutableListOf<Record>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()

    override fun cleanupTestData() {
        // MongoDB 데이터 정리
        recordRepository.deleteAll()
    }

    private fun createRandomRecord(): Record {
        val cities = listOf("Seoul", "Busan", "Incheon", "Daegu", "Daejeon")
        val streets = listOf("Main St", "First St", "Second St", "Third St", "Fourth St")

        return Record(
            name = "Test Person ${Random.nextInt(1000)}",
            age = Random.nextInt(20, 60),
            data =
                RecordDto(
                    name = "Detail ${Random.nextInt(1000)}",
                    age = Random.nextInt(20, 60),
                    address =
                        Address(
                            street = streets.random(),
                            city = cities.random(),
                        ),
                ),
        )
    }

    @Test
    @Order(1)
    fun testBulkInsert() {
        startTime = System.currentTimeMillis()
        allRecords.clear()

        var totalInserted = 0
        for (batchStart in 0 until totalRecords step batchSize) {
            val records = (1..batchSize).map { createRandomRecord() }
            recordRepository.bulkInsert(records)
            totalInserted += records.size
        }

        // DB에서 전체 데이터 조회하여 allRecords 구성
        Assertions.assertEquals(totalRecords, totalInserted, "Records count mismatch")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk insert execution time: $timeSeconds seconds")
        performanceMetrics["bulk_insert"] = timeSeconds
    }

    @Test
    @Order(2)
    fun testFindAllByQuery() {
        startTime = System.currentTimeMillis()

        // 전체 데이터 조회 테스트
        val query = Query()
        val records = recordRepository.findAllByQuery(query)
        allRecords.addAll(records)

        logger.info("Total records in DB: ${allRecords.size}")
        logger.info("Sample record: ${allRecords.first()}")
        logger.info("Found ${records.size} records")
        logger.info("Sample record: ${records.first()}")

        Assertions.assertEquals(totalRecords, records.size)

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find all by query execution time: $timeSeconds seconds")
        performanceMetrics["find_all_by_query"] = timeSeconds
    }

    @Test
    @Order(3)
    fun testBulkFindByIds() {
        startTime = System.currentTimeMillis()

        val recordIds = allRecords.mapNotNull { it.id }
        logger.info("Attempting to find ${recordIds.size} records")

//        val foundRecords = recordRepository.findAllFast()
        var totalFound = 0
        for (batch in recordIds.chunked(batchSize)) {
            val foundRecords = recordRepository.bulkFindByIds(batch)
            totalFound += foundRecords.size
//            logger.info("Found ${foundRecords.size} records in current batch")
        }

        logger.info("Total records found: $totalFound")
        Assertions.assertEquals(totalRecords, totalFound, "Records count mismatch")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk find by IDs execution time: $timeSeconds seconds")
        performanceMetrics["bulk_find_byids"] = timeSeconds
    }

    @Test
    @Order(4)
    fun testFindByCreatedAtBetween() {
        startTime = System.currentTimeMillis()

        // 전체 기간에 대한 조회 테스트
        val oldestRecord = allRecords.minByOrNull { it.createdAt ?: Instant.MAX }
        val newestRecord = allRecords.maxByOrNull { it.createdAt ?: Instant.MIN }

        if (oldestRecord?.createdAt != null && newestRecord?.createdAt != null) {
            val records =
                recordRepository.findByCreatedAtBetween(
                    oldestRecord.createdAt,
                    newestRecord.createdAt,
                )
            logger.info("Found ${records.size} records")
            logger.info("Sample record: ${records.first()}")
            Assertions.assertEquals(totalRecords, records.size)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Find by date range execution time: $timeSeconds seconds")
        performanceMetrics["find_by_date_range"] = timeSeconds
    }

    @Test
    @Order(5)
    fun testBulkUpdate() {
        startTime = System.currentTimeMillis()

        val updatedRecords =
            allRecords.map { record ->
                record.update(
                    name = "${record.name}_updated",
                    age = record.age + 1,
                    data =
                        record.data?.copy(
                            name = "${record.data!!.name}_updated",
                            age = record.data!!.age + 1,
                        ),
                )
            }

        for (batch in updatedRecords.chunked(batchSize)) {
            val result = recordRepository.bulkUpdate(batch)
            Assertions.assertEquals(batch.size, result.size)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk update execution time: $timeSeconds seconds")
        performanceMetrics["bulk_update"] = timeSeconds
    }

    @Test
    @Order(6)
    fun testBulkUpsert() {
        startTime = System.currentTimeMillis()

        // 현재 DB의 전체 레코드 수 확인
        val beforeCount = recordRepository.findAllByQuery(Query()).size
        logger.info("Records before upsert: $beforeCount")

        // 기존 레코드만 업데이트 (새 레코드 생성 없음)
        val recordsToUpsert =
            allRecords.map { record ->
                record.update(
                    name = "${record.name}_upserted",
                    age = record.age + 5,
                    data =
                        record.data?.copy(
                            name = "${record.data!!.name}_upserted",
                            age = record.data!!.age + 5,
                        ),
                )
            }

        var totalModified = 0
        for (batch in recordsToUpsert.chunked(batchSize)) {
            val result = recordRepository.bulkUpsert(batch)
            totalModified += result.modifiedCount
            logger.info("Upserted batch - Modified: ${result.modifiedCount}, Upserts: ${result.upserts.size}")

            // 새로운 레코드가 생성되지 않았는지 확인
            Assertions.assertEquals(
                0,
                result.upserts.size,
                "Unexpected new records created during upsert",
            )
        }

        // 최종 DB의 전체 레코드 수 확인
        val afterCount = recordRepository.findAllByQuery(Query()).size
        logger.info("Records after upsert: $afterCount")

        // 검증
        Assertions.assertEquals(
            beforeCount,
            afterCount,
            "Record count should not change after upsert (before: $beforeCount, after: $afterCount)",
        )
        Assertions.assertEquals(
            totalRecords,
            afterCount,
            "Total records should remain at $totalRecords (found: $afterCount)",
        )

        // 업데이트된 레코드 확인
        val updatedRecords = recordRepository.findAllByQuery(Query())
        val sampleRecord = updatedRecords.first()
        Assertions.assertTrue(
            sampleRecord.name.endsWith("_upserted"),
            "Record should have been updated with '_upserted' suffix",
        )

        allRecords.clear()
        allRecords.addAll(updatedRecords)

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk upsert (update-only) execution time: $timeSeconds seconds")
        performanceMetrics["bulk_upsert"] = timeSeconds
    }

    @Test
    @Order(7)
    fun testBulkDelete() {
        startTime = System.currentTimeMillis()

        logger.info(allRecords.first().toString())
        val recordIds = allRecords.mapNotNull { it.id }
        logger.info("Attempting to delete ${recordIds.size} records")

        var totalDeleted = 0
        for (batch in recordIds.chunked(batchSize)) {
            val deletedCount = recordRepository.bulkDelete(batch)
            totalDeleted += deletedCount
            logger.info("Deleted $deletedCount records in current batch")
        }

        logger.info("Total records deleted: $totalDeleted")
        Assertions.assertTrue(totalDeleted > 0, "No records were deleted")

        // Verify deletion
        val remainingRecords = recordRepository.findAllByQuery(Query())
        logger.info("Remaining records after deletion: ${remainingRecords.size}")
        Assertions.assertTrue(remainingRecords.isEmpty(), "Some records still exist after deletion")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        logger.info("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }

    @AfterAll
    fun finalCleanup() {
        cleanupTestData()
        printExecutionSummary()
    }
}
