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
@DisplayName("RecordRepository Mongo 통합 테스트")
class RecordRepositoryMongoIntegrationTest : BatchOperationTestSupport() {
    @Autowired
    lateinit var recordRepository: RecordRepository

    private val totalRecords = 10000
    private val batchSize = 2000
    private val allRecords = mutableListOf<Record>()
    private var startTime: Long = 0
    private val performanceMetrics = hashMapOf<String, Double>()

    override fun cleanupTestData() {
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
    @DisplayName("대량의 데이터를 삽입할 수 있다")
    fun testBulkInsert() {
        startTime = System.currentTimeMillis()
        allRecords.clear()

        var totalInserted = 0
        for (batchStart in 0 until totalRecords step batchSize) {
            val records = (1..batchSize).map { createRandomRecord() }
            recordRepository.bulkInsert(records)
            totalInserted += records.size
        }

        Assertions.assertEquals(totalRecords, totalInserted, "Records count mismatch")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Bulk insert execution time: $timeSeconds seconds")
        performanceMetrics["bulk_insert"] = timeSeconds
    }

    @Test
    @Order(2)
    @DisplayName("쿼리로 모든 데이터를 조회할 수 있다")
    fun testFindAllByQuery() {
        startTime = System.currentTimeMillis()

        val query = Query()
        val records = recordRepository.findAllByQuery(query)
        allRecords.addAll(records)

        log.debug("Total records in DB: ${allRecords.size}")
        log.debug("Sample record: ${allRecords.first()}")
        log.debug("Found ${records.size} records")
        log.debug("Sample record: ${records.first()}")

        Assertions.assertEquals(totalRecords, records.size)

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find all by query execution time: $timeSeconds seconds")
        performanceMetrics["find_all_by_query"] = timeSeconds
    }

    @Test
    @Order(3)
    @DisplayName("ID 목록으로 데이터를 조회할 수 있다")
    fun testBulkFindByIds() {
        startTime = System.currentTimeMillis()

        val recordIds = allRecords.mapNotNull { it.id }
        log.debug("Attempting to find ${recordIds.size} records")

        var totalFound = 0
        for (batch in recordIds.chunked(batchSize)) {
            val foundRecords = recordRepository.bulkFindByIds(batch)
            totalFound += foundRecords.size
//            log.debug("Found ${foundRecords.size} records in current batch")
        }

        log.debug("Total records found: $totalFound")
        Assertions.assertEquals(totalRecords, totalFound, "Records count mismatch")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Bulk find by IDs execution time: $timeSeconds seconds")
        performanceMetrics["bulk_find_byids"] = timeSeconds
    }

    @Test
    @Order(4)
    @DisplayName("생성일자 범위로 데이터를 조회할 수 있다")
    fun testFindByCreatedAtBetween() {
        startTime = System.currentTimeMillis()

        val oldestRecord = allRecords.minByOrNull { it.createdAt ?: Instant.MAX }
        val newestRecord = allRecords.maxByOrNull { it.createdAt ?: Instant.MIN }

        if (oldestRecord?.createdAt != null && newestRecord?.createdAt != null) {
            val records =
                recordRepository.findByCreatedAtBetween(
                    oldestRecord.createdAt,
                    newestRecord.createdAt,
                )
            log.debug("Found ${records.size} records")
            log.debug("Sample record: ${records.first()}")
            Assertions.assertEquals(totalRecords, records.size)
        }

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Find by date range execution time: $timeSeconds seconds")
        performanceMetrics["find_by_date_range"] = timeSeconds
    }

    @Test
    @Order(5)
    @DisplayName("대량의 데이터를 수정할 수 있다")
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
        log.debug("Bulk update execution time: $timeSeconds seconds")
        performanceMetrics["bulk_update"] = timeSeconds
    }

    @Test
    @Order(6)
    @DisplayName("대량의 데이터를 upsert할 수 있다")
    fun testBulkUpsert() {
        startTime = System.currentTimeMillis()

        val beforeCount = recordRepository.findAllByQuery(Query()).size
        log.debug("Records before upsert: $beforeCount")

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
            log.debug("Upserted batch - Modified: ${result.modifiedCount}, Upserts: ${result.upserts.size}")

            Assertions.assertEquals(
                0,
                result.upserts.size,
                "Unexpected new records created during upsert",
            )
        }

        val afterCount = recordRepository.findAllByQuery(Query()).size
        log.debug("Records after upsert: $afterCount")

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
        log.debug("Bulk upsert (update-only) execution time: $timeSeconds seconds")
        performanceMetrics["bulk_upsert"] = timeSeconds
    }

    @Test
    @Order(7)
    @DisplayName("대량의 데이터를 삭제할 수 있다")
    fun testBulkDelete() {
        startTime = System.currentTimeMillis()

        log.debug(allRecords.first().toString())
        val recordIds = allRecords.mapNotNull { it.id }
        log.debug("Attempting to delete ${recordIds.size} records")

        var totalDeleted = 0
        for (batch in recordIds.chunked(batchSize)) {
            val deletedCount = recordRepository.bulkDelete(batch)
            totalDeleted += deletedCount
            log.debug("Deleted $deletedCount records in current batch")
        }

        log.debug("Total records deleted: $totalDeleted")
        Assertions.assertTrue(totalDeleted > 0, "No records were deleted")

        val remainingRecords = recordRepository.findAllByQuery(Query())
        log.debug("Remaining records after deletion: ${remainingRecords.size}")
        Assertions.assertTrue(remainingRecords.isEmpty(), "Some records still exist after deletion")

        val endTime = System.currentTimeMillis()
        val timeSeconds = (endTime - startTime) / 1000.0
        log.debug("Bulk delete execution time: $timeSeconds seconds")
        performanceMetrics["bulk_delete"] = timeSeconds
    }

    @AfterAll
    fun finalCleanup() {
        cleanupTestData()
        printExecutionSummary()
    }
}
